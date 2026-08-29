<p align="center">
  <a href="README.md"><kbd><img src="badges-for-readme/flags/us.png" alt="English" title="English" width="64" height="42"></kbd></a>&nbsp;&nbsp;
  <a href="README.ru.md"><kbd><img src="badges-for-readme/flags/ru.png" alt="Русский" title="Русский" width="64" height="42"></kbd></a>&nbsp;&nbsp;
  <a href="README.zh-CN.md"><kbd><img src="badges-for-readme/flags/cn.png" alt="简体中文" title="简体中文" width="64" height="42"></kbd></a>&nbsp;&nbsp;
  <a href="README.es.md"><kbd><img src="badges-for-readme/flags/es.png" alt="Español" title="Español" width="64" height="42"></kbd></a>&nbsp;&nbsp;
  <a href="README.de.md"><kbd>✓&nbsp;<img src="badges-for-readme/flags/de.png" alt="Deutsch ausgewählt" title="Deutsch" width="64" height="42"></kbd></a>
</p>

<p align="center">
  <img src="https://cdn.modrinth.com/data/cached_images/849c602a1ac00208e0371ed231540da69e4fbfb3.png" alt="Chronicle" width="600">
</p>

<p align="center">
  <a href="https://github.com/aspectra00/Chronicle"><img src="https://i.imgur.com/vFmBpDq.png" alt="GitHub" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://ko-fi.com/aspectra"><img src="https://i.imgur.com/H08GkHi.png" alt="Ko-fi" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://modrinth.com/mod/chronicle-reminders"><img src="https://i.imgur.com/VROd79E.png" alt="Modrinth" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/chronicle-reminders"><img src="https://i.imgur.com/IDs74bZ.png" alt="CurseForge" width="64" height="64"></a>
</p>

<p align="center">
  <img src="badges-for-readme/minecraft.svg" alt="Minecraft" height="38">
  <img src="badges-for-readme/fabric.svg" alt="Fabric Loader" height="38">
  <img src="badges-for-readme/java.svg" alt="Java" height="38">
  <img src="badges-for-readme/chronicle.svg" alt="Chronicle" height="38">
</p>

Chronicle ist eine clientseitige Erinnerungs-Mod für Minecraft. Sie funktioniert im Einzelspieler und auf Mehrspieler-Servern, ohne dass eine serverseitige Installation nötig ist.

## Chronicle unterstützen

Chronicle ist kostenlos und wird für jede unterstützte Minecraft-Version gepflegt. Wenn dir die Mod Zeit gespart oder dabei geholfen hat, etwas Wichtiges nicht zu verpassen, kannst du die weitere Entwicklung und das Testen neuer Versionen unterstützen.

<p align="center">
  <a href="https://ko-fi.com/aspectra"><img src="https://storage.ko-fi.com/cdn/brandasset/v2/support_me_on_kofi_blue.png" alt="Chronicle auf Ko-fi unterstützen" width="220"></a>
</p>

Die Unterstützung fließt in Kompatibilitätsarbeit, Release-Tests und neue Erinnerungsfunktionen. Mitglieder können außerdem auf Wunsch im Community-Supporters-Bildschirm im Spiel genannt werden.

## Funktionen

### Erinnerungen

- Tägliche Zeitpläne
- Wöchentliche Zeitpläne mit auswählbaren Tagen
- Benutzerdefinierte Wiederholungsintervalle
- Erinnerungen direkt im Spiel aktivieren, bearbeiten, deaktivieren oder löschen
- Eine Erinnerung nach dem Auslösen behalten, deaktivieren oder löschen
- Die aktuellen Benachrichtigungseinstellungen direkt im Menü testen

### Auslöseregeln

Eine Erinnerung kann ausgelöst werden, wenn:

- Gesundheit, Hunger oder Luft einen festgelegten Wert erreicht
- Das Inventar voll ist
- Der gehaltene Gegenstand eine festgelegte Haltbarkeitsgrenze erreicht
- Der Spieler eine Dimension betritt
- Der Spieler einen festgelegten X/Z-Bereich betritt

Regeln werden ausgelöst, wenn ihre Bedingung von falsch zu wahr wechselt. Sobald die Bedingung nicht mehr erfüllt ist, sind sie wieder bereit.

### Watch This

Sieh ein unterstütztes Ziel an und drücke `R`, um die Beobachtung zu starten oder zu beenden. Chronicle kann dich benachrichtigen, wenn:

- Eine Pflanze vollständig ausgewachsen ist
- Ein Bienenstock oder Bienennest mit Honig gefüllt ist
- Ein Kessel oder Komposter bereit ist
- Höhlenranken Leuchtbeeren tragen
- Ein Ofen, Räucherofen oder Schmelzofen stoppt
- Kupfer vollständig oxidiert ist
- Ein Jungtier ausgewachsen ist

Der Watches-Bildschirm zeigt aktive Ziele der aktuellen Welt oder des aktuellen Servers. Chronicle prüft nur Daten, die dem Client bereits vorliegen. Ziele in nicht geladenen Bereichen bleiben daher ausstehend.

### Benachrichtigungen

- Modern- und Vanilla-Layout
- Optionale Snooze- und Dismiss-Schaltflächen im Modern-Layout
- Schlummerzeiten von 5, 10, 15, 30 oder 60 Minuten
- Verlauf für verpasste, erledigte und verschobene Erinnerungen
- Minimal-, Neon-, Glass- und Matrix-Designs
- Anpassbare Titel, Symbole, Farben, Größen und Animationen
- Optionaler PNG- oder JPG-Hintergrund für Modern-Benachrichtigungen
- Live-Vorschau im Anpassungsmenü
- Vanilla-, stummer oder benutzerdefinierter Benachrichtigungston

Benutzerdefinierte Audiodateien werden als MP3, OGG, WAV, AIFF und AU unterstützt. JLayer ist für die MP3-Dekodierung enthalten; weitere Informationen stehen in den [Hinweisen zu Drittanbieterkomponenten](THIRD_PARTY_NOTICES.md).

### Platzhalter

Erinnerungstexte unterstützen:

- `{world}`
- `{coords}`
- `{biome}`
- `{dimension}`

Auch über die Text Placeholder API registrierte Platzhalter werden unterstützt.

### Sprachen

- Englisch
- Russisch
- Vereinfachtes Chinesisch
- Spanisch
- Deutsch

## Steuerung

| Taste | Aktion |
|---|---|
| `J` | Chronicle öffnen |
| `R` | Das Ziel unter dem Fadenkreuz beobachten oder die Beobachtung beenden |

Beide Tasten können in den Minecraft-Tasteneinstellungen geändert werden.

## Voraussetzungen

| Abhängigkeit | Version |
|---|---:|
| Minecraft | 1.21.2 |
| Fabric Loader | 0.16.7 oder neuer (0.19.3 empfohlen) |
| Fabric API | 0.106.1+1.21.2 |
| Java | 21 |

Mod Menu ist optional. Die Text Placeholder API ist bereits in der Chronicle-JAR enthalten.

## Installation

1. Installiere Fabric Loader und Fabric API für die angegebene Minecraft-Version.
2. Kopiere die Chronicle-JAR in den Ordner `mods`.
3. Starte Minecraft und drücke `J`.

Die Einstellungen werden in `config/chronicle.json` gespeichert.

## Bauen

Verwende die oben angegebene Java-Version und führe Folgendes aus:

```powershell
.\gradlew.bat clean build
```

Die fertige JAR wird in `build/libs` abgelegt.

## Lizenz

Chronicle steht unter der [MIT-Lizenz](LICENSE).
