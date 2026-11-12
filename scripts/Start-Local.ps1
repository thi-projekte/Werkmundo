[CmdletBinding()]
param([switch]$ValidateOnly, [switch]$WithoutAi)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$setup = Join-Path $root 'local-setup'
$environmentFile = Join-Path $setup '.env.local'
$oldDirectory = Get-Location
$previousEnvironment = @{}

function Invoke-Docker {
    param([string[]]$Arguments)
    $oldPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $result = & docker @Arguments
        $commandExitCode = $LASTEXITCODE
    } finally { $ErrorActionPreference = $oldPreference }
    if ($commandExitCode -ne 0) { throw "Docker stopped with exit code $commandExitCode. Existing data was not deleted." }
    return $result
}

try {
    Set-Location $root
    $versionText = Invoke-Docker -Arguments @('compose', 'version', '--short')
    $version = [version](($versionText -replace '^v','') -replace '-.*$','')
    if ($version -lt [version]'2.24.4') { throw 'Docker Compose 2.24.4 or newer is required for the extension file.' }

    if (-not (Test-Path -LiteralPath $environmentFile)) {
        $template = Get-Content -LiteralPath (Join-Path $setup '.env.local.example')
        $lines = foreach ($line in $template) {
            if ($line -match '^([A-Z_]+)=GENERATE$') { "$($Matches[1])=$([guid]::NewGuid().ToString('N'))" }
            else { $line }
        }
        [IO.File]::WriteAllLines($environmentFile, [string[]]$lines, (New-Object Text.UTF8Encoding($false)))
        Write-Host 'Created local-setup/.env.local with local passwords. Keep this file for subsequent starts.'
    }
    $values = @{}
    foreach ($line in (Get-Content -LiteralPath $environmentFile)) {
        if ($line -match '^([A-Z_]+)=(.*)$') { $values[$Matches[1]] = $Matches[2] }
    }
    # Read the existing client configuration; do not modify the realm or create users.
    $originalRealm = Get-Content -LiteralPath (Join-Path $setup 'handwerker-realm.json') -Raw | ConvertFrom-Json
    $originalClient = @($originalRealm.clients | Where-Object clientId -eq 'user-service')
    if ($originalClient.Count -ne 1 -or [string]::IsNullOrWhiteSpace($originalClient[0].secret)) {
        throw 'Original user-service client configuration needs review.'
    }
    $values['USER_SERVICE_OIDC_SECRET'] = [string]$originalClient[0].secret

    foreach ($key in @('USER_DB_PASSWORD','OFFER_DB_PASSWORD','DOCUMENT_DB_PASSWORD',
        'KEYCLOAK_ADMIN_PASSWORD','USER_SERVICE_OIDC_SECRET')) {
        if ([string]::IsNullOrWhiteSpace($values[$key]) -or $values[$key] -eq 'GENERATE') {
            throw "Missing local setting: $key"
        }
    }
    # Compose normally prioritizes inherited test variables over --env-file.
    # Use the local integration file consistently and restore the terminal afterwards.
    foreach ($key in $values.Keys) {
        $previousEnvironment[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, $values[$key], 'Process')
    }

    $compose = @('compose','--env-file',$environmentFile,'-f',
        (Join-Path $setup 'docker-compose.local.yml'),'-f',
        (Join-Path $setup 'docker-compose.integrated.yml'))
    if (-not $WithoutAi) { $compose += @('--profile','ai') }
    Invoke-Docker -Arguments @($compose + @('config','--quiet')) | Out-Host
    if ($ValidateOnly) { Write-Host 'Combined Compose configuration is valid.'; return }

    Invoke-Docker -Arguments @('info','--format','{{.ServerVersion}}') | Out-Host
    Invoke-Docker -Arguments @($compose + @('up','-d','--build')) | Out-Host
    Invoke-Docker -Arguments @($compose + @('ps')) | Out-Host
    # Authentication integration remains with the responsible team.
    Write-Host 'Frontend: http://localhost:5173 (services may still be initializing).'
    Write-Host 'Keycloak: http://localhost:8090. Local credentials are in local-setup/.env.local.'
    Write-Host 'The process delegates, catalog implementation and PDF/sending adapters are not supplied by this startup script.'
    if (-not $WithoutAi) { Write-Host 'AI uses the existing stub unless MEGALLM_API_KEY is configured.' }
} finally {
    foreach ($key in $previousEnvironment.Keys) {
        [Environment]::SetEnvironmentVariable($key, $previousEnvironment[$key], 'Process')
    }
    Set-Location $oldDirectory
}
