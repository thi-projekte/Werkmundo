# CraftVoice lokal einrichten

Diese Anleitung beschreibt die Einrichtung von CraftVoice unter Windows. Für Apple-Systeme kann die Einleitung bei Bedarf mit einer KI angepasst werden.

## Ersteinrichtung unter Windows

### 1. Prozessorarchitektur prüfen

Öffne eine Shell und führe folgenden Befehl aus:

```powershell
$env:PROCESSOR_ARCHITECTURE
```

Wenn als Ergebnis `AMD64` angezeigt wird, ist alles in Ordnung. Bei einem anderen Ergebnis bitte Patrick oder eine KI fragen und darum bitten, die Einrichtung vollständig zu übernehmen.

### 2. WSL aktualisieren

Führe in der Shell folgenden Befehl aus:

```powershell
wsl --update
```

Damit wird sichergestellt, dass WSL (Windows Subsystem for Linux) auf dem aktuellen Stand ist.

### 3. Docker Desktop starten oder installieren

Öffne Docker Desktop. Falls Docker Desktop noch nicht installiert ist, installiere es zuerst und starte es anschließend.

### 4. Benötigtes Docker-Image laden

Führe in der Shell folgenden Befehl aus:

```powershell
docker pull ghcr.io/thi-projekte/handwerker/user-service:sprint4
```

### 5. Projektordner anlegen

Lege einen Ordner an, in dem das CraftVoice-Projekt gespeichert werden soll, und wechsle in diesen Ordner.

### 6. Repository herunterladen

Führe im angelegten Ordner folgenden Befehl aus. Dafür kann Git Bash, Visual Studio Code (mit der GitGraph-Erweiterung) oder die Shell verwendet werden:

```powershell
git pull https://github.com/thi-projekte/CraftVoice.git
```

> Hinweis: Falls der Ordner noch kein Git-Repository ist, kann alternativ zunächst `git clone https://github.com/thi-projekte/CraftVoice.git` verwendet werden.

### 7. In den CraftVoice-Ordner wechseln

Nach dem Herunterladen sollte ein Ordner namens `CraftVoice` vorhanden sein. Öffne diesen Ordner mit Visual Studio Code oder wechsle in der Shell hinein:

```powershell
cd CraftVoice
```

### 8. Frontend-Image bauen

Führe im `CraftVoice`-Ordner folgenden Befehl aus:

```powershell
docker compose -f local-setup\docker-compose.local.yml build --no-cache frontend
```

Dabei werden verschiedene benötigte Komponenten geladen und das Frontend-Image erstellt.

### 9. Lokale Umgebung starten

Führe anschließend folgenden Befehl aus:

```powershell
docker compose -f local-setup\docker-compose.local.yml up -d
```

Es sollten sieben Container gestartet werden. In Docker Desktop sollten sie den Status `healthy`, `Starting` oder einen vergleichbaren erfolgreichen Status anzeigen.

### 10. CraftVoice öffnen

Öffne im Browser folgende Adresse:

<http://localhost:5173>

CraftVoice sollte nun geladen und bereit sein. 🙂

## Zugangsdaten für die lokale Umgebung

- **Benutzer:** `test@craftvoice.local`
- **Passwort:** `test`

## Nachfolgende Starts

Nach einer erfolgreichen Ersteinrichtung muss Docker Desktop bei jedem Start geöffnet sein, da die Docker-Befehle sonst nicht funktionieren.

Wechsle zunächst in den `CraftVoice`-Ordner.

### Wenn sich Dateien geändert haben

Führe diese Befehle aus:

```powershell
docker compose -f local-setup\docker-compose.local.yml build --no-cache frontend
docker compose -f local-setup\docker-compose.local.yml up -d
```

### Wenn sich nichts geändert hat

In diesem Fall reicht normalerweise:

```powershell
docker compose -f local-setup\docker-compose.local.yml up -d
```
