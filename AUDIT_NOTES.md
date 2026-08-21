# Chronicle Deep Audit

> **Minecraft 26.2 · Chronicle 1.2.8 · fixed86**
> Whole-project correctness, UI geometry, scheduler, persistence and audio audit.

Chronicle's Java and resource sources were reviewed against Minecraft 26.2 behavior for GUI lifecycle, font metrics, resize and GUI-scale changes, clipping, input routing, scheduler state, notification delivery, custom audio and configuration persistence.

A clean Gradle build completed successfully against the declared Minecraft 26.2 toolchain.

---

## Audit status

| Area | Status |
|---|:---:|
| GUI lifecycle | ✅ Verified |
| Responsive layout | ✅ Verified |
| Input / focus routing | ✅ Verified |
| Scissor / clipping | ✅ Verified |
| Toast geometry | ✅ Verified |
| Toast actions | ✅ Verified |
| Scheduler state | ✅ Verified |
| Trigger rule state | ✅ Verified |
| Config persistence | ✅ Verified |
| Custom audio lifecycle | ✅ Verified |
| Localization consistency | ✅ Verified |
| Clean release build | ✅ Passed |

### Release gate

```text
Chronicle 1.2.8
fixed86
Minecraft 26.2
Java 25
clean build: PASS
```

---

## When → Then trigger builder verification

The reminder editor now includes a fourth schedule mode for condition-based rules.

Verified trigger conditions:

- Health percentage at or below a configured threshold
- Hunger points at or below a configured threshold
- Remaining air percentage at or below a configured threshold
- Inventory with no empty slot
- Main-hand item durability at or below a configured threshold
- Entry into a namespaced dimension identifier
- Entry into a configurable X/Z radius

Runtime evaluation uses transition state instead of repeated level polling notifications. A rule initializes silently, fires only on `false → true`, stays quiet while the condition remains true, and re-arms after a false observation. Disabling, deleting, editing or changing a trigger definition resets its runtime state safely.

Configuration validation bounds percentages, hunger, coordinates and radius; canonicalizes dimension identifiers; and preserves compatibility with every pre-trigger `chronicle.json` file. All five locale files contain the same **189 translation keys**.

The editor retains trigger input and focus through scrolling, resizing and GUI-scale rebuilds. The four schedule modes use a readable responsive grid at the `320×240` logical floor, while long trigger forms remain inside the existing clipped scroll viewport.

---

## Interactive notification actions

Chronicle Modern notifications can render compact **Snooze** and **Dismiss** actions. Vanilla notifications remain action-free regardless of the saved Modern preference. The toast customizer persists the on/off preference and a selectable 5, 10, 15, 30 or 60 minute delay, and previews each frame accurately.

- Hit boxes derive from the same local geometry used to render each action.
- Toast slide position and slot position are recorded from Minecraft's own `Toast` transforms.
- Hovering an interactive toast pauses its lifetime and progress indicator.
- Screen mouse input is intercepted only when an action is hit, so underlying controls receive every unrelated click.
- Snooze creates an exact one-shot interval reminder using the selected delay.
- The new reminder is persisted before the original toast closes.
- Reminder limits and save failures keep the toast visible and surface an inline failure state.
- Successful Snooze and Dismiss actions hide the current toast immediately.
- Dismiss leaves the reminder's future schedule unchanged.
- Screen input hooks are restored after window and GUI-scale reinitialization.
- Weak interaction references cannot retain toasts removed by Minecraft's manager.

Modern action layouts place content-sized controls beside the message without overlapping title, text, icon or progress geometry. The compact frame uses balanced insets and a single icon accent, while Vanilla retains its native text inset and line rhythm. Weekly schedules always repeat; post-trigger behavior is available for Daily and condition-trigger schedules. All five locale files contain the same **194 translation keys**.

---

# fixed86

## MIT release and 26.2 compatibility verification

`fixed86` publishes Chronicle 1.2.8 under the MIT License and revalidates the complete release surface against the declared Minecraft 26.2 toolchain.

### License surface

- Replaced the project-level CC0 dedication with the standard MIT License text.
- Updated `fabric.mod.json` to the SPDX `MIT` identifier exposed by Fabric Loader and Mod Menu.
- Confirmed that the release JAR embeds the matching project license.
- Preserved the independent license notices for bundled third-party components.

### Release metadata

- Bumped the Gradle project version to **1.2.8**.
- Synchronized the README release heading and Chronicle SVG badge.
- Prepared the repository for the matching `1.2.8` Git tag.
- Excluded local Gradle, runtime and analysis artifacts from release commits.

### Compatibility gate

- Minecraft **26.2**
- Fabric Loader **0.19.3**
- Fabric API **0.157.0+26.2**
- Fabric Loom **1.17.19**
- Java **25**
- Gradle **9.5.1**

The full Java and resource source set remains lint-clean, and a clean release build completes successfully with the declared compatibility matrix.

---

# fixed85

## Whole-project correctness and geometry audit

`fixed85` completes the full Chronicle reliability and pixel-geometry audit.

### UI transitions and coordinate stability

- Replaced fractional screen scale and translation effects with a **pixel-stable fade**.
- Visual bounds and pointer hit boxes now remain in the same coordinate system throughout open and close transitions.
- One-pixel borders, text and controls no longer drift onto fractional coordinates during animation.

### Text fields and input geometry

- Borderless-field padding is now part of the field's actual content geometry.
- Rendered text, horizontal scrolling, caret placement, selection, click and drag positions all use the same inset.
- IME candidate placement follows the same geometry.
- Long custom-sound paths no longer visually disagree with their editable or selectable region.

### Responsive layouts

Corrected Chronicle's layouts at Minecraft's **320×240 logical minimum**.

- Schedule modes remain on one usable row.
- The main footer leaves enough space for at least one interactive reminder card.
- Save errors have a dedicated non-overlapping strip.
- Sound path and `Browse` controls remain fully contained.
- Hidden or inactive controls cannot retain focus.

### Notification geometry

- Unified live-preview and real-toast geometry.
- Notification dimensions now update safely after resize or GUI-scale changes.
- Added stable three-slot allocation.
- Removed the second Chronicle-specific slide transform.
- Disabling animations now makes notification motion immediate.
- Unicode, CJK and long unbroken tokens wrap without silently dropping the remaining text.
- Placeholder rendering and progress state now match between previews, tests and real notifications.

### Scheduler correctness

- Interval reminders now use **exact millisecond deadlines**.
- Legacy minute-based timing data migrates safely.
- Interval checks no longer depend on the wall-clock minute gate.
- Forward and backward system-clock changes are handled without catch-up spam.
- Editor and scheduler races can no longer restore stale enabled state or stale interval deadlines.
- A reminder deleted by a one-shot action cannot be silently recreated by an open editor.

### Persistence

- Failed runtime saves remain marked dirty.
- Save retries use bounded backoff.
- Chronicle performs a final persistence attempt during shutdown.
- The main screen exposes persistent save failure instead of silently hiding it.
- Config loading distinguishes malformed JSON from temporary I/O failure.
- Corrupt configuration receives a unique recoverable backup before replacement.
- Temporary read failures are never replaced with defaults.
- Config input is capped at **4 MiB**.
- Reminder count is capped at **512**.

### Custom audio

Custom sound playback now follows a strict **latest-request-wins** lifecycle.

- Deterministic cancellation of stale work
- Synchronized `Clip` and decoded-audio cache access
- Minecraft `MASTER` volume × Chronicle UI volume
- Complete shutdown cleanup
- `OFF` / `VANILLA` transitions cancel stale custom playback
- Closing the sound screen prevents delayed custom test playback

### Notification traffic

- Added a bounded **32-item notification queue**.
- Delivery is paced instead of dumping a burst into Minecraft's toast stack.
- Duplicate notifications are aggregated.
- Overflow is represented by a localized summary notification.

### Localization

All five locale files contain the same **154 translation keys** with matching formatting parameters.

- English
- Russian
- Simplified Chinese
- Spanish
- German

### Code quality

- Enabled Java 25 `-Xlint:all`.
- Removed dead helpers.
- Completed static scans for:
  - debug prints
  - blocking sleeps
  - unfinished markers

A clean **Chronicle 1.2.7** build is required as the release gate.

---

# Verification

## Build

```bash
gradlew clean build --no-daemon --warning-mode all
```

**Result:** successful.

Layout assertions cover:

- `320×240`
- intermediate compact sizes
- `427×240`
- `640×360`
- `920×384`
- `960×540`
- `1280×720`
- `1920×1080`
- the `720px` desktop-layout transition

Automated source and build verification cannot fully replace a manual in-game visual pass across every resource pack, font renderer, operating system and DPI configuration.

---

# Compatibility

| Component | Verified version |
|---|---|
| Minecraft | **26.2** |
| Java | **25** |
| Fabric Loader | **0.19.3** |
| Fabric API | **0.157.0+26.2** |
| Text Placeholder API | **3.1.0-beta.1+26.2** |
| Fabric Loom | **1.17.19** |
| Gradle | **9.5.1** |

### Rendering

Chronicle uses Minecraft's `GuiGraphicsExtractor` rendering path.

**No raw OpenGL dependency is used.**

---

# Core fixes

These findings predate the numbered revision history below and remain part of the verified state.

## Layout and field geometry

1. Borderless `EditBox` values, caret and selection now use one vertically centered baseline.

   The previous five-pixel screen-level offset was incorrect for a `28px` field and could leave color values visually clipped or crowded.

2. Compact Toast Customizer color rows no longer overlap.

   The palette target was previously placed `18px` inside the final field row, while single-column pairs could share identical coordinates.

3. Size-control groups fit their available columns and stack when required.

   Compact title input now uses the full available width.

4. Compact scroll extent includes:
   - palette cards
   - the complete HSV picker
   - live preview

   One-column palettes no longer overlap the picker.

5. The style selector and caption scroll together with editor content.

   Only the footer remains fixed.

   All scrolling controls share the same clip boundary and hit-test viewport at Minecraft's `320×240` logical floor.

## Clipping and input

6. Scrolling buttons are re-styled before `disableScissor()`.

   Partial cards and editor controls can no longer bleed across headers or footers.

7. Fixed footer controls receive mouse input before content underneath them.

   The HSV picker cannot intercept clicks outside its clipped viewport.

8. Focus survives useful scroll and resize rebuilds, but is cleared when its field leaves the viewport.

   Hidden text fields therefore cannot consume keyboard input.

9. The main reminder list now constructs a genuinely interactive row at `320×240` instead of rendering row text without its buttons.

## State and validation

10. Inactive schedule tabs have a distinct visual state.

    Button and error text is safely width-clipped.

    The HSV current-color swatch reflects saturation and value instead of displaying pure hue only.

11. A failed enable/save operation restores:
    - the enabled flag
    - the interval timer

    Broken legacy configs are backed up at the correct path, and atomic-save fallback handles provider-specific `IOException` variants.

12. Config strings are trimmed and bounded without splitting surrogate pairs.

    Stale HEX validation clears immediately once a field becomes valid.

13. Section captions are derived from actual field coordinates.

    `ICON`, `TITLE` and color captions have explicit vertical gutters so labels cannot sit behind controls or crowd the next row at any scale.

---

# Revision history

<details>
<summary><strong>fixed84 · Pixel-stable toast motion</strong></summary>

## fixed84

### Pixel-stable toast motion

1. Removed the two-pixel vertical settle from the Modern toast transform.

   Minecraft keeps `fullyVisibleForMs == 0` throughout its native entrance slide. Rounding the old offset therefore kept the card one pixel below its final baseline before moving it upward when entrance completed.

2. Modern notifications now animate only on the **X axis**.

   Frame, text, icon, progress line and shadow keep exactly the same Y coordinate on every frame.

3. Release verification:

```text
build --no-daemon --warning-mode all
Chronicle 1.2.6
PASS
```

</details>

<details>
<summary><strong>fixed83 · Notification redesign and boundary audit</strong></summary>

## fixed83

### Modern notification redesign

1. Replaced the flat rectangular Modern toast with a smaller, wider notification card featuring:
   - stepped pixel corners
   - in-bounds shadow
   - layered surface
   - subtle top highlight
   - restrained accent rail
   - adaptive icon tile
   - draining lifetime indicator

   Chronicle branding remains absent from notification content.

2. Fixed the empty-card entrance bug at its source.

   Minecraft 26.2 reports `fullyVisibleForMs == 0` throughout the native `600ms` toast slide. The previous renderer reused this value as content alpha, making title, message and icon transparent during entrance.

   The complete card now shares one motion transform and full content opacity.

3. Modern toast width and height were rebalanced for faster scanning.

   Added:
   - safe GUI bounds
   - adaptive icon-free fallback
   - Unicode-safe title clipping
   - up to two body lines where height permits

   The customizer still uses the same renderer as real notifications.

### Sound screen

4. Rebuilt sound-settings geometry around one immutable layout snapshot shared by initialization and rendering.

   Standard layouts use:
   - `17px` caption-to-control offset
   - `12px` section gap

   Compact and high-scale layouts remove secondary captions before collisions become possible.

5. Removed hard minimum widths that could push:
   - Browse
   - volume
   - footer controls

   outside narrow panels.

   Shared content inset can no longer consume more than half of its panel.

### Boundary audit

6. Added narrow-GUI containment for:
   - reminder-list text
   - empty-state copy
   - weekly day buttons
   - HSV marker
   - current-color swatch
   - palette cards

7. Re-audited:
   - scheduler behavior
   - duplicate suppression
   - one-shot actions
   - placeholder fallback
   - atomic config save and rollback
   - async audio generation tokens
   - focus
   - scrolling
   - scissor order
   - footer hit-testing
   - resize rebuilds

   No additional functional regression was found.

8. Release verification:

```text
build --no-daemon
Chronicle 1.2.5
PASS
```

</details>

<details>
<summary><strong>fixed82 · Menu identity and sound-field polish</strong></summary>

## fixed82

### Menu identity

1. Removed Chronicle logo rendering from:
   - reminder list
   - reminder editor
   - Toast Customizer
   - sound settings

   Header title X positions now begin directly at the shared content inset.

   Title clipping, subtitles and the main `TEST` action use the reclaimed width.

   The unused in-screen logo texture and rendering API were removed from the JAR.

2. Rebuilt the Fabric / Mod Menu icon as a rounded graphite tile with:
   - restrained cyan outline
   - centered Chronicle mark
   - five-pixel transparent safety margin

   The icon is a `128×128 RGBA` PNG.

   Automated checks require transparent, partially transparent and fully opaque pixels so rounded corners cannot regress into a square background.

### Sound path field

3. Fixed custom-sound text beginning directly on the field border.

   The borderless `EditBox` now supports render-only horizontal padding while preserving:
   - logical hit box
   - narration bounds

   The sound screen applies four pixels on each side.

4. Added:
   - localized empty-state hint in all five languages
   - wider Browse action for long translations
   - explicit disabled text color

   Stored paths remain complete in configuration. Only on-screen rendering may clip or scroll them.

### Verification

5. Rechecked header geometry from the `320×240` floor through desktop layouts.

   Static source checks find no remaining menu-logo draw calls or references to the removed GUI texture.

6. Release verification:

```text
clean build --no-daemon --warning-mode all
Chronicle 1.2.4
PASS
```

</details>

<details>
<summary><strong>fixed81 · Notification identity, audio layout and MP3</strong></summary>

## fixed81

### Notification identity

1. Removed Chronicle branding from both notification styles and their live previews.

   **Modern**
   - reclaims reserved text width
   - retains the configurable user icon tile

   **Vanilla**
   - returns to Minecraft's native `18px` text inset
   - returns to compact native width calculation

2. Re-exported the UI logo and Fabric / Mod Menu icon as real 32-bit PNGs with transparency outside the supplied cyan mark.

   Automated alpha bounds:

```text
UI crop:       12..115
Metadata icon: 25..102
```

### Sound layout

3. Non-compact sound layouts now place an `8px` gutter after the entire format-help line before the `VOLUME` caption.

   Widget construction and rendering use the same geometry helper.

### Test audio

4. Main-screen `TEST` and Customizer `SHOW TOAST` use the saved audio configuration.

   - `OFF` invalidates stale async playback.
   - `VANILLA` uses the configured quiet cue.
   - `CUSTOM` uses the bounded background decoder.
   - Older decode work cannot begin playback after a newer request.

### MP3

5. Added:
   - `.mp3` validation
   - native file-dialog filtering
   - bounded JLayer decoding to little-endian 16-bit PCM

   Existing limits also apply to MP3:
   - `16 MiB` input
   - `30 seconds` decoded duration
   - mono / stereo only
   - up to `192 kHz`

   The decoded clip cache now checks file size in addition to path and modification time.

6. MP3 smoke test:

```text
14,267 MPEG frames
32,871,168 PCM samples
44.1 kHz
stereo
```

   The production path rejects the long fixture once decoded PCM crosses the 30-second limit instead of buffering the complete file.

7. Release verification:

```text
clean build --no-daemon --warning-mode all
Chronicle 1.2.3
PASS
```

The remapped JAR includes:
- JLayer as a nested library
- third-party notice
- transparent logos
- five locales
- expected Fabric metadata

</details>

<details>
<summary><strong>fixed80 · Logo sampling, J key and test action</strong></summary>

## fixed80

### Texture-region fix

1. Fixed the invisible header/toast logo at the rendering source.

   Minecraft 26.2's short `GuiGraphicsExtractor.blit` overload uses destination width and height as the sampled source-region dimensions.

   At `20–28` logical pixels, Chronicle therefore sampled only the empty top-left corner of the `128px` texture.

   Chronicle now calls the explicit overload with independent:
   - destination dimensions
   - `128×128` source region
   - `128×128` texture dimensions

2. Re-exported the Fabric / Mod Menu metadata icon as a standard opaque `128×128` PNG.

   Automated mark bounds:

```text
x = 25..102
y = 24..103
```

   This leaves approximately `24–25px` safe margins on every side.

   The separate UI crop retains `11–13px` margins for small-screen readability.

### Key mapping

3. Changed Chronicle's default menu key:

```text
L → J
```

The mapping remains fully rebindable through Minecraft Controls.

### Test action

4. Restored a short localized `TEST` action in the Toast Customizer and added the same action to the fixed main-screen header.

   Header geometry reserves the button's actual width before clipping title and subtitle text.

5. Main-screen `TEST` uses current saved:
   - colors
   - frame
   - animation setting
   - placeholders

   It deliberately:
   - skips audio
   - bypasses scheduler duplicate filtering
   - uses a `500ms` click debounce

6. Rechecked:
   - all five `145-key` locale files
   - format parameters
   - static Java translation references
   - key mapping source
   - test-button presence
   - image alpha / dimensions / bounds
   - responsive header geometry from `244×240` through `1920×1080`

7. Release verification:

```text
clean build --no-daemon --warning-mode all
Chronicle 1.2.2
PASS
```

Compiled bytecode uses GLFW key code `74` (`J`).

</details>

<details>
<summary><strong>fixed79 · Logo integration and final audit</strong></summary>

## fixed79

### Branding

1. Replaced the Fabric / Mod Menu icon with the supplied Chronicle clock artwork.

   Added a tighter `128px` UI texture derived from the same source so the mark remains readable at small header sizes without changing its geometry or colors.

2. Added Chronicle branding to:
   - main reminder list
   - reminder editor
   - Toast Customizer
   - sound screen

   Header text begins after the logo and clips against the actual remaining width at compact GUI scales.

3. Added a reserved right-side Chronicle mark to Modern notifications while preserving the user's custom icon tile.

   The mark is omitted only if an exceptionally narrow toast cannot preserve a safe text column.

4. Added a compact Chronicle tile to Vanilla notifications.

   Vanilla retains:
   - resource-pack-aware `toast/system`
   - native typography

   Its text inset and width calculation reserve the exact logo width and gap.

### Focus semantics

5. Separated:
   - mouse hover
   - keyboard focus
   - persistent selected state

   Keyboard focus uses a quiet outline only after keyboard input.

### Navigation

6. Fixed the Chronicle key mapping creating nested main screens when pressed inside another Chronicle screen.

   The menu opens only when no Chronicle screen is already active.

### Audio state

7. Prevented stale asynchronous sound errors from reappearing after sound-mode changes.

   Requests use a generation token. Clearing or switching modes invalidates late worker results.

   Sound-screen controls are also ignored during close transitions.

8. Restyled the sound path field using Chronicle's shared:
   - border
   - surface
   - baseline
   - focus treatment

   Added shared press feedback.

   Disabling global animations also disables all press pulses.

### Verification

9. Verified:
   - all five `145-key` localization files
   - matching format placeholders
   - static Java translation references
   - both PNG dimensions
   - responsive header and toast geometry from `320×240` through `1920×1080`
   - required JAR entries

10. Release verification:

```text
clean build --no-daemon --warning-mode all
Chronicle 1.2.1
PASS
```

</details>

<details>
<summary><strong>fixed78 · Smart reminders</strong></summary>

## fixed78

### Native key mapping

1. Added a Minecraft-native Chronicle key mapping under the Chronicle category.

   Initial default:

```text
L
```

It opens the reminder screen directly and is fully rebindable.

### Post-trigger behavior

2. Added per-reminder actions:

- keep repeating
- disable after first display
- delete after first display

Scheduler-side mutations safely refresh an already-open reminder list.

### Placeholders

3. Added friendly live placeholders for:
   - player
   - save / server name
   - coordinates
   - biome
   - dimension

Text Placeholder API is bundled, allowing compatible placeholders registered by other mods to be parsed as well.

### Notification audio

4. Added:

- `OFF`
- `VANILLA`
- `CUSTOM`

Custom audio supports:
- native file selection
- asynchronous decoding
- one-clip caching
- OGG
- WAV
- AIFF / AU-family formats

Before playback Chronicle bounds:
- file size
- decoded duration
- channel count
- sample rate

### Scheduler

5. Scheduler work exits before allocating date/time objects on inactive client ticks.

   Existing `15-minute` catch-up ceiling remains.

   Identical toast bursts are suppressed within `2.5 seconds`.

### Localization

6. Added `145` matching translation keys in:

- English
- Russian
- Simplified Chinese
- Spanish
- German

Custom labels, errors, schedule summaries, palette names, sound settings and key mappings follow Minecraft's active language.

### Sound-screen rebuilds

7. Sound settings preserve draft values and focus through resize and GUI-scale changes.

   Compact-height layout removes secondary captions and reduces spacing before controls can collide.

</details>

<details>
<summary><strong>fixed77 · Vanilla geometry correction</strong></summary>

## fixed77

### Vanilla sizing

1. Vanilla preview and real notifications no longer reuse Modern responsive dimensions.

   Stretching Minecraft's `160×64` nine-slice sprite into the larger Modern card produced a disproportionate panel.

2. Vanilla width now follows `SystemToast` behavior:

- `160px` minimum
- up to `200px` of text
- `30px` margins
- clamped to current logical GUI width

A one-line title and message produce the native `32px` height.

Wrapped message lines add `12px` each.

### Native typography

3. Vanilla rendering uses:

```text
left text inset: 18px
title baseline:  y=7
line rhythm:     12px
title color:     yellow
message color:   white
```

Intentionally omitted:
- non-Vanilla icon tile
- custom font scaling
- custom progress line

### Preview behavior

4. Live preview centers the compact Vanilla toast inside its reserved region instead of stretching it.

   Irrelevant controls display:

```text
1.00x
NO ICON
```

and remain disabled until Modern style is selected.

</details>

<details>
<summary><strong>fixed76 · Notification redesign and motion</strong></summary>

## fixed76

### Color workspace

1. Wide compact layouts place the HSV picker on the left and use the free right-side space for:
   - active target selector
   - eight presets

   Narrow layouts preserve the same logical order in one column.

2. Palette presets are now full-width cards containing:
   - swatch
   - name

   Removed:
   - detached `USE` column
   - dark divider lines

   Hue rendering is a continuous per-pixel gradient with a single outer boundary.

### Live preview

3. Compact live preview remains at the top and updates immediately.

   `SHOW TOAST` remains fixed in the footer and can be triggered regardless of scroll position.

### Notification styles

4. Added:

```text
TOAST: MODERN / VANILLA
```

**Modern**
- compact hierarchy
- inset shadow
- accent rail
- one-pixel progress indicator

**Vanilla**
- Minecraft `toast/system` sprite
- resource-pack awareness
- pixel-bevel icon slot

### Animation preference

5. Added persisted:

```text
ANIMATIONS: ON / OFF
```

Enabled animations provide:
- `220ms` screen opening settle
- deferred `170ms` close
- frame-rate-independent button hover / emphasis interpolation

Disabled animations switch screens and button states immediately.

### Navigation safety

6. Navigation is ignored while a close transition is already active.

   Resize and GUI-scale rebuilds:
   - reset transient drags
   - preserve meaningful field focus
   - preserve draft values

### Toast text

7. Added:
   - separate title/body rhythm
   - adaptive one/two-line wrapping
   - Unicode-safe clipping
   - tiny-manager-bound protection

Modern shadow remains inside the dimensions reported to Minecraft's toast manager.

### Config migration

8. Older configuration files receive:

```text
frame = MODERN
animations = enabled
```

Invalid future frame names normalize to Modern.

Failed saves roll both new settings back in memory.

### Static layout assertions

9. Assertions cover:

```text
320×240
427×240
640×360
920×384
960×540
720px desktop breakpoint
1280×720
1920×1080
```

All checked layouts preserve:
- `8px` control gap
- `12px` section gap
- non-overlapping picker / palette columns

</details>

<details>
<summary><strong>fixed75 · Usability and code audit</strong></summary>

## fixed75

1. Compact Toast Customizer begins with the live preview.

   Order:

```text
Preview
Style
Text
Size
Colors
Palettes
HSV
```

`SHOW TOAST` remains fixed while the preview scrolls away.

2. Color captions follow:

```text
28px control
+ 8px gutter
+ 17px caption offset
```

This prevents `ACCENT`, `TITLE COLOR`, `MESSAGE COLOR` and `ICON COLOR` from overlapping the preceding HEX field.

3. Palette-target fields no longer show a permanent focus-like blue outline.

   Active color target is represented explicitly by:

```text
EDIT COLOR: … >
```

4. Preview, style section, color rows, palettes and HSV use one compact scroll model.

   Bottom scrolling retains `24px` before the fixed footer.

5. HSV readout now displays the target's actual alpha instead of `00`.

   Picker handles remain in bounds.

   Palette hover and selection borders use computed state correctly.

6. Scrolling the reminder editor captures current draft values before rebuilding widgets.

   Partially entered:
   - time
   - interval
   - message

   values cannot be replaced by stale snapshots.

7. Failed saves restore:
   - in-memory 12/24-hour preference
   - reminder state

   Editing an interval reminder without changing its interval no longer restarts the countdown.

8. Toast shadow is contained within the dimensions reported to Minecraft's toast manager.

9. Config path now comes from Fabric Loader's config-directory API.

</details>

<details>
<summary><strong>fixed74 · Interface polish</strong></summary>

## fixed74

1. `UiMetrics` now provides Chronicle's common geometry:

```text
control gap:       8px
section gap:      12px
content inset:    24px responsive
field heights
button heights
header boundary
text baseline
```

2. Reminder list, schedule editor and Toast Customizer share one button renderer.

   Persistent selection is visible for:
   - schedule modes
   - weekly days
   - toast styles
   - primary actions

   Keyboard focus receives the same highlight treatment.

3. Compact reminder cards reserve:
   - `5px` text-to-button gutter
   - `8px` row gutter

   The `320×240` layout still exposes one complete row.

4. Long editor and customizer screens show a proportional scroll indicator.

   Changing mode returns scroll position to the top.

5. Reminder deletion requires a second confirmation click within **three seconds**.

6. Clicking any HEX field selects it as the palette / HSV target.

   Active field and matching palette swatch remain highlighted.

   Narrow picker captions are width-clipped instead of colliding with the HEX value.

7. Read-only scale values no longer behave like buttons.

   Footer widths derive from shared inner content bounds and cannot escape the panel.

8. Schedule-editor sections derive their Y position from the actual bottom of preceding controls.

   Stacked:
   - mode
   - time
   - day
   - message

   layouts therefore keep consistent gutters.

9. Toast Customizer switches to scrolling layout below `680` logical pixels.

   Desktop mode is used only if the complete palette fits above the footer.

   HSV position derives from the real preview height.

10. Truncated toast body text always receives an ellipsis.

    The message editor shows a non-overlapping `80-character` counter when enough space exists.

</details>

---

# Final verified state

Chronicle `fixed86` extends the `fixed74 → fixed85` reliability sequence with a synchronized MIT-licensed 1.2.8 release.

The verified release state includes:

- pixel-stable screen transitions
- responsive operation at `320×240`
- consistent text-input geometry
- viewport-safe scrolling and hit testing
- native-correct Vanilla toast sizing
- shared preview / real-toast rendering
- exact millisecond scheduler deadlines
- editor / scheduler race protection
- safe persistence retries
- recoverable corrupt-config backups
- bounded reminder traffic
- deterministic custom-audio cancellation
- five synchronized localizations
- Java 25 lint-clean release validation

```text
Chronicle 1.2.8 / fixed86
Minecraft 26.2
Release audit: COMPLETE
Clean build: PASS
```
