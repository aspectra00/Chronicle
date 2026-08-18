![Chronicle logo](src/main/resources/assets/chronicle/icon.png)

#  Chronicle
<p align="center">
  <a href="https://github.com/aspectrea00/Chronicle"><img src="https://voxelforge-oss.github.io/voxicons/voxicons/icons-64/brands/github.png" alt="GitHub" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://ko-fi.com/aspectra"><img src="https://voxelforge-oss.github.io/voxicons/voxicons/icons-64/brands/kofi.png" alt="Ko-fi" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://modrinth.com/mod/chronicle-reminders"><img src="https://voxelforge-oss.github.io/voxicons/voxicons/icons-64/brands/modrinth.png" alt="Modrinth" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/chronicle-reminders"><img src="https://voxelforge-oss.github.io/voxicons/voxicons/icons-64/brands/curse-forge.png" alt="CurseForge" width="64" height="64"></a>
</p>

<p align="center">
<img src="badges-for-readme/minecraft.svg" height="38">
<img src="badges-for-readme/fabric.svg" height="38">
<img src="badges-for-readme/java.svg" height="38">
<img src="badges-for-readme/chronicle.svg" height="38">
</p>
> **Smart scheduled reminders for Minecraft.**  
> Create reminders, customize notifications, repeat them automatically, and keep track of what matters without leaving the game.
> 



**Chronicle 1.2.7 · fixed85** is a client-side Fabric mod for Minecraft **26.2** that adds flexible scheduled reminders with customizable notifications, repeat rules, placeholders, custom sounds, localization, and responsive in-game configuration screens.

---

##  Features

###  Smart reminders

Create reminders directly in Minecraft and decide exactly how they behave.

- Schedule reminders for later
- Repeat reminders automatically
- Disable a reminder after its first display
- Delete a reminder automatically after it fires
- Edit and manage reminders in-game
- Handles reminder bursts without flooding the screen

Chronicle also supports context-aware placeholders:

- `{world}`
- `{coords}`
- `{biome}`
- `{dimension}`

Additional placeholders registered through **Text Placeholder API** are supported as well.

---

##  Custom notifications

Choose how Chronicle reminders appear in-game.

### Modern

A compact custom notification with:

- Stepped-corner card design
- Adaptive icon tile
- Accent rail
- One or two-line text wrapping
- Remaining-time indicator
- Smooth native toast motion
- Shared preview and real-toast renderer

### Vanilla

Prefer something closer to Minecraft itself?

The **Vanilla** style follows Minecraft 26.2 `SystemToast` geometry:

- Native minimum width
- Native typography
- Compact message height
- Vanilla-style spacing
- Resource-pack-aware rendering

The preview inside Chronicle uses the same geometry as actual notifications, so what you configure is what you get.

---

##  Toast Customizer

Chronicle includes a full notification customization workspace.

Customize:

- Notification colors
- HSV values
- Preset palettes
- Notification style
- Scale where supported
- Animation behavior
- Live preview

The color workspace adapts to narrow and wide GUI layouts, including Minecraft's **320×240 logical minimum**.

The hue picker uses a continuous spectrum and all picker handles, swatches and palette cards remain clamped to their real UI bounds.

---

##  Notification sounds

Each reminder can use your saved Chronicle sound configuration.

Choose between:

- **Vanilla** — quiet Minecraft notification cue
- **Off** — no notification sound
- **Custom** — use a local audio file

Supported custom formats:

`MP3` · `OGG` · `WAV` · `AIFF` · `AU`

Custom audio is:

- Decoded outside the render thread
- Cached after first use
- Limited to **16 MB**
- Limited to **30 seconds**
- Safely cancelled during shutdown or replacement

MP3 playback uses the bundled **JLayer** decoder.

---

##  Animations

Chronicle has a global animation option.

When enabled, it controls:

- Button feedback
- Screen fade transitions
- Toast motion

When disabled, UI navigation and feedback become immediate, and native notification positioning respects the setting as well.

Screen transitions are **fade-only**, keeping text, borders, hit boxes and pixel geometry aligned to integer coordinates.

---

##  Controls

Chronicle uses Minecraft's native key mapping system.

**Default key:** `J`

You can change it normally from:

**Options → Controls → Key Binds**

A compact **TEST** action is also available from the Chronicle interface for quickly checking your current notification configuration.

---

##  Languages

Chronicle automatically follows Minecraft's selected language.

Currently included:

- 🇬🇧 English
- 🇷🇺 Русский
- 🇨🇳 简体中文
- 🇪🇸 Español
- 🇩🇪 Deutsch

---

##  Reliability

Chronicle's scheduler and persistence layer are designed to survive more than the ideal "click button, nothing goes wrong" universe humans apparently dream of.

The current release includes:

- Exact millisecond interval deadlines
- Thread-safe editor and scheduler updates
- Protection against deleted reminders being resurrected
- Protection against disabled reminders firing again
- Safe retry behavior after failed saves
- Bounded notification burst handling
- Recoverable configuration backups
- Separate handling for temporary I/O errors and malformed JSON
- Deterministic custom-audio cancellation
- Safe shutdown behavior
- Correct notification resize and GUI-scale updates
- Unicode and CJK-safe wrapping without silently losing text
- Correct caret, selection, mouse and IME coordinates in padded fields

---

##  Requirements

| Dependency | Version |
|---|---:|
| Minecraft | **26.2** |
| Fabric Loader | **0.19.3** |
| Fabric API | **0.157.0+26.2** |
| Java | **25** |

### Development

Chronicle currently builds with:

| Tool | Version |
|---|---:|
| Fabric Loom | **1.17.19** |
| Java | **25** |

---

##  Installation

1. Install **Fabric Loader** for Minecraft 26.2.
2. Install the matching **Fabric API**.
3. Place the Chronicle `.jar` file into your Minecraft `mods` folder.
4. Launch Minecraft.
5. Press **J** to open Chronicle.

Chronicle is a **client-side mod**.

---

##  Current release

### Chronicle 1.2.7 — fixed85

`fixed85` completes a project-wide reliability, responsive-layout and pixel-geometry audit.

Highlights include:

- Fade-only screen transitions with pixel-stable rendering
- Full usability at Minecraft's 320×240 logical GUI floor
- Correct text-field padding for caret, selection, mouse and IME input
- Safe notification resizing after window or GUI-scale changes
- Shared renderer geometry between preview and real notifications
- Reliable Unicode/CJK wrapping
- Animation-off support for native toast positioning
- Exact interval scheduling
- Race-safe editor and scheduler interaction
- Safe persistence retries and recoverable backups
- Bounded reminder bursts
- Deterministic custom-audio lifecycle

For the full verified audit, see [`AUDIT_NOTES.md`](AUDIT_NOTES.md).

---

<details>
<summary><strong> fixed76–fixed85 revision history</strong></summary>

### fixed85

Completes a whole-project reliability and pixel-geometry audit.

Screen transitions are now fade-only, eliminating fractional movement of one-pixel lines, text and hit boxes. Responsive layouts preserve complete schedule, sound and list controls at Minecraft's 320×240 logical minimum.

Notification geometry updates safely after window resize and GUI-scale changes. Preview and real notifications now share the same renderer. Long Unicode and CJK text wraps without content loss.

Scheduler and persistence improvements include exact millisecond interval deadlines, race-safe reminder editing, safe save retries, bounded burst handling, deterministic custom-audio cancellation and recoverable configuration backups.

### fixed84

Removes the final vertical component from Modern notification motion.

The notification Y coordinate remains pixel-stable throughout Minecraft's entrance and exit animations, eliminating the previous one-pixel rise when the intro completed.

### fixed83

Redesigns the Modern notification as a compact stepped-corner card with:

- Layered surfaces
- Restrained accent rail
- Adaptive icon tile
- Safe one/two-line wrapping
- Remaining-time indicator

The entire notification now shares one motion transform, keeping the title, message and icon visible throughout Minecraft's native 600 ms slide.

The sound screen was also rebuilt around a single immutable layout snapshot, with improved spacing and narrow-GUI bounds handling.

### fixed82

Removes decorative Chronicle logos from settings screens and keeps branding where application identity is expected: Fabric metadata and Mod Menu.

Header titles reclaim the former logo area for improved readability at small GUI scales.

The custom-sound path field gains proper internal padding, clipping, empty-state text and consistent disabled styling.

### fixed81

Removes Chronicle branding from notification content so reminders contain only the user's icon and reminder text.

Header and Mod Menu textures now use real PNG transparency.

The sound screen gains improved vertical spacing and native file-dialog filters. All test actions use the currently saved **OFF**, **VANILLA** or **CUSTOM** sound configuration.

MP3 decoding runs on Chronicle's bounded audio worker through the bundled JLayer library.

### fixed80

Fixes the Minecraft 26.2 texture-region overload that caused only the upper-left part of the header logo to render.

The metadata icon becomes an explicit opaque 128×128 PNG while the UI uses a separate readable crop.

The default Chronicle key changes to **J**.

A localized **TEST** action is permanently available in the main header and Toast Customizer footer.

### fixed79

Adds the supplied Chronicle clock mark as the Fabric/Mod Menu identity and introduces clearer keyboard-focus rendering.

Keyboard focus now appears as a subtle accessibility outline instead of being confused with mouse hover or selection state.

### fixed78

Introduces the Smart in-game Reminders feature set.

Adds:

- Native Minecraft key mapping
- Repeatable reminders
- Disable-after-display behavior
- Delete-after-display behavior
- Friendly built-in placeholders
- Text Placeholder API integration
- Vanilla/off/custom notification audio
- Local MP3/OGG/WAV/AIFF/AU loading
- Off-thread decoding
- Audio caching
- English, Russian, Simplified Chinese, Spanish and German localization

### fixed77

Corrects Vanilla notification geometry to match Minecraft 26.2 `SystemToast` metrics instead of stretching the vanilla nine-slice across the larger Modern card.

Vanilla notifications use:

- Native 160 px minimum width
- Compact single-message height
- 12 px line spacing
- Native 1× typography
- No custom user icon

Irrelevant scale controls are disabled when Vanilla mode is selected.

### fixed76

Redesigns both notification presentation and the color workspace.

Color presets occupy unused space beside the HSV picker on wider compact screens, each preset becomes a single clear action, and the hue strip becomes continuous.

The live preview gains both **Modern** and resource-pack-aware **Vanilla** frames.

A saved animation preference controls button feedback, screen transitions and notification motion.

</details>

---

##  Audit

The verified UI, scheduler, input, persistence, notification and audio findings for this revision are documented in:

[`AUDIT_NOTES.md`](AUDIT_NOTES.md)

---

<p align="center">
  <strong>Chronicle</strong><br>
  Scheduled reminders that stay inside the game.
</p>
