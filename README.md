# Werkmundo

Werkmundo unterstützt Handwerksbetriebe bei der Erstellung und
Verwaltung von Angeboten und Dokumenten.

Das Projekt wird auf Grundlage der vereinbarten Systemarchitektur
neu aufgebaut. Der bisherige Stand dient als fachliche Referenz
und liegt im Branch `archive/vorsemester`.

## Aktueller Stand

Dieses Repository enthält derzeit das Strukturgrundgerüst.
Die meisten Dateien sind Platzhalter. Build-Konfigurationen,
Implementierungen und eine neue Startanleitung werden ergänzt.

Die Anwendung ist mit diesem Grundgerüst noch nicht startbar.

## Projektstruktur

| Ordner | Inhalt |
|---|---|
| frontend/ | Oberfläche nach MVVM mit Model, View, ViewModel und API-Client |
| backend/services/ | Benutzer-, KI-, Angebots-, Dokumenten- und Katalogservice |
| gateway/ | Gemeinsamer Einstiegspunkt für Backend-Anfragen |
| processengine/ | BPMN-Modelle, Prozesssteuerung und Service-Anbindung |
| contracts/ | Vereinbarte Schnittstellen und Datenformate |
| tests/ | Übergreifende Tests; Modultests liegen bei den jeweiligen Modulen |
| documents/ | Architektur, Zuständigkeiten und Anleitungen |
| .github/ | GitHub-Workflows und Vorlagen |

## Architektur

Das Frontend kommuniziert über einen API-Client mit dem Gateway.
Dieses leitet Anfragen an die zuständigen Services weiter.

Jeder Service verwaltet seine eigenen fachlichen Daten.
Die Process Engine koordiniert die vorgesehenen BPMN-Abläufe.
Authentifizierung und Berechtigungen werden mit Keycloak abgestimmt.

## Zusammenarbeit

Pro Aufgabe wird ein eigener Branch erstellt.
Änderungen werden per Pull Request und Review übernommen.

Die Zuständigkeiten stehen in
[documents/team-ownership.md](documents/team-ownership.md).
Eine allgemein verständliche Erklärung der Handwerker-Komponenten bietet
[documents/handwerker-squad-systemarchitektur.md](documents/handwerker-squad-systemarchitektur.md).

Die Branch-Namen und der Integrationsweg nach main werden
in den gemeinsamen Arbeitsregeln festgehalten.

## Tests und Bereitstellung

CI-Prüfungen werden passend zu den ausführbaren Modulen eingerichtet.
Eine erfolgreiche Strukturprüfung bestätigt keine Funktionsfähigkeit.

Die automatische Bereitstellung wird mit dem Serverbetrieb abgestimmt.
Vor der Übernahme dieses Umbaus nach main muss die bestehende
Portainer-Anbindung geprüft werden.