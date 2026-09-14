# MinimalTextLauncher

Ein extrem minimalistischer Android-Launcher.

## Konzept

- schwarzer Hintergrund
- weiße Textschrift
- Batteriestand als erster Listeneintrag, ohne `%`
- darunter alle startbaren Apps alphabetisch
- keine Icons
- keine Werbung
- keine Widgets
- keine zusätzlichen Bedienelemente
- App-Einträge sind direkt anklickbar
- vertikal scrollbar

## Build über GitHub Actions

1. Repository auf GitHub erstellen.
2. Alle Dateien dieses Projekts hochladen.
3. Änderungen committen.
4. `Actions` öffnen.
5. `Build APK` auswählen.
6. `Run workflow` starten.
7. Nach erfolgreichem Build unter `Artifacts` die APK `MinimalTextLauncher-debug` herunterladen.

## Launcher festlegen

Nach der Installation kann Android fragen, ob Minimal Text Launcher als Standard-Start-App verwendet werden soll. Falls nicht, kann dies in den Android-Einstellungen unter den Standard-Apps bzw. der Home-App ausgewählt werden.

## Hinweis zur Schrift

Die aktuelle Version verwendet die systemeigene Sans-Schrift als robuste, offline-fähige Basis. Dadurch ist der Build vollständig unabhängig von einem externen Font-Download. Die Schrift kann im nächsten Schritt problemlos durch eine fest eingebettete freie Schrift wie Manrope ersetzt werden.
