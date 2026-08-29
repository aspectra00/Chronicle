<p align="center">
  <img src="https://cdn.modrinth.com/data/cached_images/849c602a1ac00208e0371ed231540da69e4fbfb3.png" alt="Chronicle" width="600">
</p>

<p align="center">
  <a href="README.md"><img src="badges-for-readme/flags/us.png" alt="English" title="English" width="64" height="42"></a>&nbsp;&nbsp;
  <a href="README.ru.md"><img src="badges-for-readme/flags/ru.png" alt="Русский" title="Русский" width="64" height="42"></a>&nbsp;&nbsp;
  <a href="README.zh-CN.md"><img src="badges-for-readme/flags/cn.png" alt="简体中文" title="简体中文" width="64" height="42"></a>&nbsp;&nbsp;
  <a href="README.es.md"><img src="badges-for-readme/flags/es.png" alt="Español" title="Español" width="64" height="42"></a>&nbsp;&nbsp;
  <a href="README.de.md"><img src="badges-for-readme/flags/de.png" alt="Deutsch" title="Deutsch" width="64" height="42"></a>
</p>

<p align="center">
  <a href="https://github.com/aspectra00/Chronicle"><img src="https://i.imgur.com/vFmBpDq.png" alt="GitHub" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://ko-fi.com/aspectra"><img src="https://i.imgur.com/H08GkHi.png" alt="Ko-fi" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://modrinth.com/mod/chronicle-reminders"><img src="https://i.imgur.com/VROd79E.png" alt="Modrinth" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/chronicle-reminders"><img src="https://i.imgur.com/IDs74bZ.png" alt="CurseForge" width="64" height="64"></a>
</p>

<p align="center">
  <img src="badges-for-readme/minecraft.svg" alt="Minecraft 26.2" height="38">
  <img src="badges-for-readme/fabric.svg" alt="Fabric Loader 0.19.3" height="38">
  <img src="badges-for-readme/java.svg" alt="Java 25" height="38">
  <img src="badges-for-readme/chronicle.svg" alt="Chronicle 1.3.1 for Minecraft 26.2" height="38">
</p>

Chronicle is a client-side reminder mod for Minecraft. It works in singleplayer and on multiplayer servers without a server-side install.

## Support Chronicle

Chronicle is free and maintained across every supported Minecraft version. If it has saved you time or helped you avoid missing something important, you can help keep updates tested and available.

<p align="center">
  <a href="https://ko-fi.com/aspectra"><img src="https://storage.ko-fi.com/cdn/brandasset/v2/support_me_on_kofi_blue.png" alt="Support Chronicle on Ko-fi" width="220"></a>
</p>

Support goes directly toward compatibility work, release testing, and new reminder features. Members can also choose to be credited in Chronicle's in-game Community Supporters screen.

## Features

### Reminders

- Daily schedules
- Weekly schedules with selectable days
- Custom repeat intervals
- Enable, edit, disable, or delete reminders in-game
- Keep, disable, or delete a reminder after it fires
- Test the current notification settings from the menu

### Trigger rules

Trigger a reminder when:

- Health, hunger, or air reaches a set level
- The inventory is full
- The held item reaches a durability limit
- The player enters a dimension
- The player enters a configured X/Z area

Rules fire when their condition changes from false to true. They become ready again after the condition is no longer met.

### Watch This

Look at a supported target and press `R` to start or stop watching it. Chronicle can notify you when:

- A crop finishes growing
- A beehive or bee nest fills with honey
- A cauldron or composter becomes ready
- Cave vines grow berries
- A furnace, smoker, or blast furnace stops
- Copper reaches full oxidation
- A baby mob grows up

The Watches screen lists active targets for the current world or server. Chronicle only checks data already available to the client, so unloaded targets remain pending.

### Notifications

- Modern and Vanilla layouts
- Optional Snooze and Dismiss buttons in the Modern layout
- Snooze delays of 5, 10, 15, 30, or 60 minutes
- History for missed, completed, and snoozed reminders
- Minimal, Neon, Glass, and Matrix themes
- Custom title, icon, colors, sizes, and animation setting
- Optional PNG or JPG background for Modern notifications
- Live preview in the customizer
- Vanilla, muted, or custom notification sound

Custom audio supports MP3, OGG, WAV, AIFF, and AU files. JLayer is bundled for MP3 decoding; see [third-party notices](THIRD_PARTY_NOTICES.md).

### Placeholders

Reminder text supports:

- `{world}`
- `{coords}`
- `{biome}`
- `{dimension}`

Placeholders registered through Text Placeholder API are supported as well.

### Languages

- English
- Russian
- Simplified Chinese
- Spanish
- German

## Controls

| Key | Action |
|---|---|
| `J` | Open Chronicle |
| `R` | Watch or unwatch the target under the crosshair |

Both bindings can be changed under Minecraft's key bind settings.

## Requirements

| Dependency | Version |
|---|---:|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.0 or newer (0.19.3 recommended) |
| Fabric API | 0.157.0+26.2 |
| Java | 25 |

Mod Menu is optional. Text Placeholder API is included in the Chronicle JAR.

## Installation

1. Install Fabric Loader and Fabric API for Minecraft 26.2.
2. Copy the Chronicle JAR into the `mods` folder.
3. Start Minecraft and press `J`.

Settings are stored in `config/chronicle.json`.

## Building

Use Java 25 and run:

```powershell
.\gradlew.bat clean build
```

The release JAR is written to `build/libs`.

## License

Chronicle is available under the [MIT License](LICENSE).
