[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$originalDirectory = Get-Location
$variables = @('JAVA_HOME', 'Path', 'OFFER_PERSISTENCE', 'OFFER_DB_ENABLED',
    'OFFER_DB_JDBC_URL', 'OFFER_DB_USER', 'OFFER_DB_PASSWORD', 'DOCUMENT_DB_PASSWORD',
    'DOCUMENT_DB_TESTS', 'QUARKUS_DATASOURCE_JDBC_URL', 'QUARKUS_DATASOURCE_USERNAME',
    'QUARKUS_DATASOURCE_PASSWORD')
$previousValues = @{}
foreach ($variable in $variables) {
    $previousValues[$variable] = [Environment]::GetEnvironmentVariable($variable, 'Process')
}

function Invoke-Checked {
    param([string]$Executable, [string[]]$Arguments)
    $previousErrorPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 can turn native progress on stderr into errors.
        $ErrorActionPreference = 'Continue'
        & $Executable @Arguments
        $commandExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorPreference
    }
    if ($commandExitCode -ne 0) { throw "Command failed (exit $commandExitCode): $Executable" }
}

function Read-DatabasePassword {
    param([string]$Existing, [string]$Label)
    if (-not [string]::IsNullOrWhiteSpace($Existing)) { return $Existing }
    $secret = Read-Host $Label -AsSecureString
    $value = (New-Object System.Net.NetworkCredential('', $secret)).Password
    if ([string]::IsNullOrWhiteSpace($value)) { throw 'Database password must not be empty.' }
    return $value
}

function Test-Service {
    param([string]$Service, [string]$RequiredSuite)
    Set-Location (Join-Path $repositoryRoot "backend/services/$Service")
    $logPath = Join-Path (Get-Location) 'auftraggeber-verification.log'
    Write-Host "Building and testing $Service. Log: $logPath"
    $previousErrorPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & .\mvnw.cmd -B '-Dquarkus.http.test-port=18082' clean verify *> $logPath
        $buildExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorPreference
    }
    Get-Content -LiteralPath $logPath -Tail 35
    if ($buildExitCode -ne 0) { throw "$Service build failed. See $logPath" }
    $reports = @(Get-ChildItem -LiteralPath 'target/surefire-reports' -Filter 'TEST-*.xml')
    if ($reports.Count -eq 0) { throw "$Service produced no test reports." }
    $databaseTestsFound = $false
    $testCount = 0
    foreach ($report in $reports) {
        [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        $suite = $xml.testsuite
        $testCount += [int]$suite.tests
        if ([int]$suite.failures -gt 0 -or [int]$suite.errors -gt 0 -or [int]$suite.skipped -gt 0) {
            throw "Failed or skipped tests in $($report.Name)."
        }
        if ($suite.name.EndsWith($RequiredSuite) -and [int]$suite.tests -gt 0) {
            $databaseTestsFound = $true
        }
    }
    if (-not $databaseTestsFound) { throw "Database tests missing: $RequiredSuite" }
    Write-Host "$Service verified: $testCount tests, including PostgreSQL."
}

try {
    $jdk = Get-ChildItem -LiteralPath "$env:ProgramFiles/Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk-25*' } | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) {
        $env:JAVA_HOME = $jdk.FullName
        $env:Path = "$env:JAVA_HOME/bin;$env:Path"
    }
    # --version writes to stdout, unlike -version, including on Windows PowerShell 5.1.
    $javaVersion = (& java --version | Out-String)
    if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '(?m)^(?:openjdk|java)\s+25(?:[.\s]|$)') {
        throw 'Java 25 is required.'
    }
    Invoke-Checked -Executable 'docker' -Arguments @('info', '--format', '{{.ServerVersion}}')

    $env:OFFER_DB_PASSWORD = Read-DatabasePassword -Existing $env:OFFER_DB_PASSWORD `
        -Label 'Existing Offer database password'
    $env:DOCUMENT_DB_PASSWORD = Read-DatabasePassword -Existing $env:DOCUMENT_DB_PASSWORD `
        -Label 'Document database password (choose one on first use)'

    Set-Location (Join-Path $repositoryRoot 'backend/services/offer-service')
    Invoke-Checked -Executable 'docker' -Arguments @('compose', '-f', 'docker-compose.db.yml', 'up', '-d', '--wait', '--wait-timeout', '90')
    $env:OFFER_PERSISTENCE = 'postgres'
    $env:OFFER_DB_ENABLED = 'true'
    $env:OFFER_DB_JDBC_URL = 'jdbc:postgresql://127.0.0.1:55432/offer_db'
    $env:OFFER_DB_USER = 'offer_user'
    $env:QUARKUS_DATASOURCE_JDBC_URL = $env:OFFER_DB_JDBC_URL
    $env:QUARKUS_DATASOURCE_USERNAME = $env:OFFER_DB_USER
    $env:QUARKUS_DATASOURCE_PASSWORD = $env:OFFER_DB_PASSWORD
    Test-Service -Service 'offer-service' -RequiredSuite 'OfferJpaRepositoryIntegrationTest'

    Set-Location (Join-Path $repositoryRoot 'backend/services/document-service')
    Invoke-Checked -Executable 'docker' -Arguments @('compose', '-f', 'docker-compose.db.yml', 'up', '-d', '--wait', '--wait-timeout', '90')
    $env:DOCUMENT_DB_TESTS = 'true'
    $env:QUARKUS_DATASOURCE_JDBC_URL = 'jdbc:postgresql://127.0.0.1:55433/document_db'
    $env:QUARKUS_DATASOURCE_USERNAME = 'document_user'
    $env:QUARKUS_DATASOURCE_PASSWORD = $env:DOCUMENT_DB_PASSWORD
    Test-Service -Service 'document-service' -RequiredSuite 'PostgresDocumentRepositoryTest'

    Write-Host 'Both services passed their build and PostgreSQL tests. UI, OIDC and BPMN still need integration checks.'
} finally {
    foreach ($variable in $variables) {
        [Environment]::SetEnvironmentVariable($variable, $previousValues[$variable], 'Process')
    }
    Set-Location $originalDirectory
}
