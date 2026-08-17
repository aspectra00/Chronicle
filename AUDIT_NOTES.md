# Chronicle deep audit — fixed85

All Java and resource sources were reviewed for Minecraft 26.2 GUI lifecycle,
font metrics, resize/GUI scale, clipping, input routing, scheduler state and config
persistence. A clean Gradle build completed successfully with the declared 26.2
dependencies.

## Fixed

1. Borderless `EditBox` values, caret and selection now use a single vertically
   centered baseline. The previous five-pixel screen-level offset was incorrect
   for a 28px field and could leave color values visually clipped or crowded.
2. Compact Toast Customizer color rows no longer overlap. The palette target was
   previously placed 18px inside the final field row, and single-column pairs
   shared identical coordinates.
3. Size-control groups now fit their columns and stack when necessary; compact
   title input uses the full available width.
4. Compact scroll extent includes palette cards, the complete HSV picker and live
   preview. One-column palettes no longer overlap the picker.
5. The style selector and its caption now scroll with all other editor content;
   only the footer remains fixed. Every scrolling control shares the same clip
   boundary and hit-test viewport at Minecraft's 320x240 logical GUI floor.
6. Scrolling buttons are re-styled before `disableScissor()`. Partial cards and
   editor buttons can no longer bleed over headers or footers.
7. Fixed footer controls receive mouse input before content underneath them.
   The HSV picker cannot intercept clicks outside its clipped viewport.
8. Focus is retained across useful scroll/resize rebuilds and cleared when its
   field leaves the viewport, preventing hidden text fields from consuming input.
9. The main reminder list now exposes a real interactive row at 320x240 instead
   of drawing row text without constructing its buttons.
10. Inactive schedule tabs have a distinct state, button/error text is safely
    width-clipped, and HSV's current-color swatch reflects saturation/value rather
    than displaying only the pure hue.
11. A failed enable/save operation restores the interval timer as well as the
    enabled flag. Broken legacy configs are backed up at the correct path, and
    atomic-save fallback handles provider-specific `IOException` variants.
12. Config strings are trimmed and safely bounded without splitting surrogate
    pairs; stale HEX validation clears as soon as the field becomes valid.
13. Section captions are positioned from their actual field coordinates. The
    ICON/TITLE and color captions now have explicit vertical gutters, so text no
    longer sits behind the ICON size row or crowds the next control at any scale.

## fixed74 interface polish

1. `UiMetrics` now supplies the common 8px control gap, 12px section gap,
   24px responsive content inset, field/button heights, header boundary and text
   baseline used across all screens.
2. The reminder list, schedule editor and toast customizer now share one button
   renderer. Selected schedule modes, weekly days, toast styles and primary
   actions remain visually selected without requiring hover; keyboard focus uses
   the same highlight.
3. Compact reminder cards reserve a real five-pixel text-to-button gutter and an
   eight-pixel row gutter. The 320x240 layout still exposes one complete row.
4. Long editor and customizer views show a proportional scroll indicator. Mode
   changes return to the top instead of leaving the user halfway down a different
   form.
5. Reminder deletion requires a second confirmation click within three seconds.
6. Clicking any HEX color field selects it as the palette/HSV target. The active
   field and matching palette swatch stay highlighted; narrow picker captions are
   width-clipped instead of colliding with the HEX value.
7. Read-only scale values no longer behave like clickable buttons. Footer widths
   are derived from the same inner content bounds and cannot escape the panel.
8. The schedule editor's first caption starts below the header clip, and every
   subsequent section is positioned from the actual bottom of the preceding
   controls. Stacked mode, time, day and message layouts therefore keep identical
   gutters.
9. The toast customizer uses the scrolling layout below 680 logical pixels. Its
   desktop layout is only selected when the complete palette fits above the
   footer. The HSV picker is positioned from the real preview height.
10. Truncated toast body text now always receives an ellipsis, and the message
    editor shows a non-overlapping 80-character counter when space permits.

## fixed75 usability and code audit

1. Compact Toast Customizer now starts with the real live preview. Style, text,
   size, color fields, palettes and HSV controls follow in a predictable order.
   The fixed `SHOW TOAST` action remains available while the preview is scrolled
   away.
2. Color captions use a strict 28px control + 8px gutter + 17px caption offset.
   `ACCENT`, `TITLE COLOR`, `MESSAGE COLOR` and `ICON COLOR` can no longer sit
   behind the preceding HEX field.
3. Palette-target fields no longer receive a permanent blue focus-like outline.
   The explicit `EDIT COLOR: … >` control communicates the active palette/HSV
   target without suggesting that the user clicked or focused a field.
4. The live preview, style section, color rows, palette cards and HSV picker now
   share one compact scroll model. The picker follows its palette with a 12px
   section gap, and bottom scrolling leaves 24px before the fixed footer.
5. The HSV readout now includes the target's real alpha channel instead of
   displaying `00`; picker handles stay inside their controls. Palette card hover
   and selection borders now render using the computed state.
6. Scrolling the reminder editor first captures all current field values, so
   partially typed time, interval or message text is never replaced by an older
   snapshot during a widget rebuild.
7. A failed save restores the in-memory 12/24-hour preference as well as the
   reminder. Editing an interval reminder without changing its interval no longer
   silently restarts its countdown.
8. Toast rendering no longer draws a shadow outside the dimensions reported to
   Minecraft's toast manager, preventing edge fragments and overlap between
   stacked notifications.
9. The config path now comes from Fabric Loader's config-directory API instead
   of rebuilding it from the Minecraft game directory.

## fixed76 notification redesign and motion

1. On wide compact layouts, the HSV picker now uses the left side of the color
   workspace while the target selector and eight presets use the previously empty
   right side. Narrow layouts keep the same logical order in a single column.
2. Each palette preset is one full-width card with a swatch and name. The old
   detached `USE` column and its dark dividers were removed; the hue strip is a
   continuous per-pixel gradient with only one outer boundary.
3. The compact live preview remains at the top and updates immediately. `SHOW
   TOAST` stays fixed in the footer so a user can test the current draft at any
   scroll position.
4. A new `TOAST: MODERN / VANILLA` control changes the notification shell. The
   modern shell has a compact hierarchy, inset shadow, accent rail and one-pixel
   progress indicator; vanilla mode uses Minecraft's current `toast/system`
   sprite and a pixel-bevel icon slot, so resource packs can restyle it naturally.
5. A new persisted `ANIMATIONS: ON / OFF` setting controls Chronicle's motion.
   Shared wall-clock transitions provide a subtle 220ms opening settle and a
   deferred 170ms close, while button hover/emphasis interpolates independently
   of frame rate. Disabled motion switches screens and button states immediately.
6. Screen navigation is ignored while a close transition is already running,
   preventing double activation. Resize/GUI-scale rebuilds reset transient drags
   and preserve the meaningful text-field focus and draft values.
7. Toast text has separate title/body rhythm, adaptive one/two-line wrapping,
   Unicode-safe clipping and guarded geometry for tiny manager bounds. The modern
   shadow remains inside the size reported to Minecraft's toast manager.
8. Config loading is backward compatible: older files receive `MODERN` and
   animations enabled by default, while invalid future frame names normalize to
   the safe modern mode. Failed saves roll both new settings back in memory.
9. Static layout assertions cover the 320x240 floor, 427x240, 640x360, 920x384,
   960x540, the 720px desktop breakpoint, 1280x720 and 1920x1080. All checked
   layouts preserve the 8px control gap, 12px section gap and non-overlapping
   picker/palette columns.

## fixed77 Vanilla geometry correction

1. The Vanilla preview and real toast no longer reuse the much larger responsive
   dimensions of the modern card. That stretched Minecraft's 160x64 nine-slice
   sprite into a disproportionate blue panel.
2. Vanilla width now follows `SystemToast`: 160px minimum, up to 200px of text
   plus 30px margins, clamped to the current logical GUI width. A one-line title
   and message produce the native 32px height; wrapped message lines add 12px.
3. Vanilla text uses Minecraft's native 18px left inset, y=7 title baseline,
   twelve-pixel line rhythm, yellow title and white message. The non-vanilla icon
   tile, custom font scaling and custom progress line are intentionally omitted.
4. The live preview centers the compact toast inside its reserved area without
   stretching it. Its caption identifies native sizing, and irrelevant scale
   controls display `1.00x` / `NO ICON` and cannot be clicked until MODERN is
   selected again.

## Compatibility verified

- Minecraft 26.2 / Java 25
- Fabric Loader 0.19.3
- Fabric API 0.157.0+26.2
- Text Placeholder API 3.1.0-beta.1+26.2 (included in the mod JAR)
- Fabric Loom 1.17.19 / Gradle 9.5.1
- `GuiGraphicsExtractor` rendering only; no raw OpenGL dependency

## Verification

`gradlew clean build --no-daemon` completed successfully. Layout assertions cover
320x240, intermediate compact sizes, the 720px desktop transition and 1920x1080.
Automated source/build verification cannot replace a manual in-game visual pass
with every resource pack, font and OS DPI combination.

## fixed78 Smart reminders

1. Added a native Minecraft key mapping, default `L`, under the Chronicle
   category. It opens the reminder screen directly and is fully rebindable.
2. Added per-reminder post-trigger actions: keep repeating, disable after the
   first display, or delete after the first display. Scheduler-side mutations
   refresh an already-open list safely.
3. Added friendly live placeholders for player, save/server name, coordinates,
   biome and dimension. Text Placeholder API is bundled, so common/client
   placeholders registered by other mods are parsed as well.
4. Added OFF/VANILLA/CUSTOM sound modes, volume control, native file selection,
   async decoding, one-clip caching and OGG/WAV/AIFF/AU-family support. File size,
   duration, channel count and sample rate are bounded before playback.
5. The scheduler now exits before allocating date/time objects on the other
   client ticks, keeps the existing 15-minute catch-up ceiling, and suppresses
   identical toast bursts within 2.5 seconds.
6. Added 145 matching translation keys in English, Russian, Simplified Chinese,
   Spanish and German. Custom-drawn labels, errors, schedule summaries, palette
   names, sound settings and key mapping names all use the active game language.
7. The sound screen preserves its draft and focus through resize/GUI-scale
   rebuilds. Its compact-height layout removes the secondary caption and reduces
   control spacing before any labels, fields or footer actions can collide.

## fixed79 logo integration and final audit

1. Replaced the Fabric/Mod Menu icon with the supplied Chronicle clock artwork.
   Added a tighter 128px UI texture made from the same source so the thin mark
   remains readable in small headers without changing its geometry or colors.
2. Added the brand mark to the main list, reminder editor, toast customizer and
   sound screen. Header text now starts after the logo and is clipped against the
   real remaining width at compact GUI scales.
3. Added a reserved right-side Chronicle mark to the modern notification while
   preserving the user's custom icon tile. The mark is omitted automatically only
   when an exceptionally narrow toast cannot keep a safe text column.
4. Added a compact Chronicle tile to the Vanilla notification. Vanilla still uses
   the resource-pack-aware `toast/system` sprite and native typography; its text
   inset and width calculation now reserve the exact logo width and gap.
5. Corrected button focus semantics. Mouse hover, keyboard focus and persistent
   selected state are no longer conflated. Keyboard focus uses a quiet outline
   only after keyboard input; opening a screen with the mouse no longer makes the
   first button look selected.
6. Fixed the Chronicle key mapping creating nested main screens when `L` was
   pressed inside the editor, toast customizer or sound screen. It now opens the
   menu only when no Chronicle screen is already active.
7. Fixed stale asynchronous sound errors reappearing after the user changed sound
   mode. Audio requests use a generation token, and clearing/switching modes
   invalidates late worker results. Sound-screen buttons are also ignored during
   close transitions, matching every other Chronicle screen.
8. Restyled the sound path field with the shared Chronicle border, surface,
   baseline and focus treatment. Added the same press feedback used by the other
   screens, and made the global animation toggle disable all press pulses too.
9. Verified all 145 localization keys across English, Russian, Simplified Chinese,
   Spanish and German, including identical format-placeholder sets. Verified all
   static translation references from Java, both PNG dimensions, responsive
   header/toast geometry at 320x240 through 1920x1080, and required JAR entries.
10. `clean build --no-daemon --warning-mode all` succeeds for version 1.2.1. The
    remapped JAR includes the Mod Menu icon, UI logo texture, five languages and
    bundled Text Placeholder API.

## fixed80 logo sampling, J key and test action

1. Fixed the invisible header/toast logo at its actual source. Minecraft 26.2's
   short `GuiGraphicsExtractor.blit` overload uses the destination width/height
   as the sampled source-region width/height. At 20–28 logical pixels it therefore
   sampled only the empty top-left corner of the 128px PNG. Chronicle now calls
   the explicit overload with independent destination, 128×128 source-region and
   128×128 texture dimensions.
2. Re-exported the Fabric/Mod Menu metadata icon as a standard opaque 128×128 PNG.
   Automated pixel-bound checks place the cyan mark at x=25..102, y=24..103,
   leaving 24–25px safe margins on every side. The UI crop remains a separate
   opaque 128×128 texture with 11–13px margins for small-screen readability.
3. Changed the registered default menu key from `L` to `J`. The mapping remains
   in Minecraft Controls and is still fully rebindable.
4. Restored a short localized `TEST` action in the toast customizer and added the
   same action to the fixed main-screen header for immediate access without
   scrolling. The header reserves the button's real width before clipping title
   and subtitle text, so it cannot cover either at compact GUI scales.
5. The main-screen test uses current saved colors, frame, animation setting and
   placeholders, deliberately skips audio, bypasses scheduler duplicate filtering,
   and has a 500ms click debounce so testing cannot flood the toast stack.
6. Rechecked all five 145-key locale files and their formatted parameters, static
   Java translation references, key mapping source, test-button presence, image
   alpha/dimensions/bounds and responsive header geometry from 244×240 through
   1920×1080.
7. `clean build --no-daemon --warning-mode all` succeeds for version 1.2.2. JAR
   inspection confirms both logo resources, five languages, metadata version and
   bundled Text Placeholder API. Compiled bytecode uses GLFW key code 74 (`J`) and
   the full-region `blit` descriptor.

## fixed81 notification identity, audio layout and MP3

1. Removed the Chronicle brand mark from the right edge of MODERN notifications
   and from the left edge of VANILLA notifications, including live previews.
   Modern text reclaims the reserved width; Vanilla returns to Minecraft's native
   18px text inset and compact width calculation. The user's configurable Modern
   icon tile remains unchanged.
2. Re-exported both the UI logo texture and Fabric/Mod Menu icon as real 32-bit
   PNGs with transparent pixels outside the supplied cyan mark. Automated alpha
   checks confirm transparent backgrounds and safe mark bounds of 12..115 for the
   UI crop and 25..102 for the metadata icon.
3. The non-compact sound layout now places an eight-pixel gutter after the entire
   format-help line before the VOLUME caption. Widget construction and rendering
   call the same geometry helper, preventing resize or GUI-scale drift.
4. Main-screen TEST and customizer SHOW TOAST now play the saved sound mode,
   custom path and volume. OFF invalidates stale async playback; VANILLA uses the
   configured quiet cue; CUSTOM uses the bounded background decoder. A stale
   custom decode cannot start after a newer sound request.
5. Added `.mp3` validation, native file-dialog filtering and bounded JLayer
   decoding to little-endian 16-bit PCM. The existing 16MB input, 30-second
   decoded-duration, mono/stereo and 192kHz limits also apply to MP3. The last
   decoded clip cache now checks file size as well as path and modification time.
6. A real MP3 smoke test decoded 14,267 MPEG frames into 32,871,168 PCM samples
   at 44.1kHz stereo. The production path rejects the same long fixture once its
   decoded PCM crosses the 30-second limit instead of buffering the whole file.
7. `clean build --no-daemon --warning-mode all` succeeds for version 1.2.3. The
   remapped JAR includes JLayer as a separate nested library, the third-party
   notice, transparent logos, five locales and the expected Fabric metadata.

## fixed82 menu identity and sound-field polish

1. Removed logo rendering from the reminder list, reminder editor, toast
   customizer and sound settings. Header title X coordinates now start directly
   at the shared content inset; title clipping, subtitles and the main TEST action
   all use the reclaimed width. The unused in-screen logo texture and rendering
   API were removed from the JAR.
2. Rebuilt the Fabric/Mod Menu icon as a rounded graphite tile with a restrained
   cyan outline, centered Chronicle mark and a five-pixel transparent safety
   margin. It is a 128×128 RGBA PNG; automated checks require transparent,
   partially transparent and fully opaque pixels so rounded corners cannot turn
   back into a square background.
3. Fixed the custom-sound path beginning directly on the field border. The
   borderless `EditBox` now supports render-only horizontal content padding while
   preserving its full logical hit box and narration bounds. The sound screen
   applies four pixels on each side, so glyphs, selection, cursor and horizontal
   clipping share the same inset.
4. Added a localized empty-state hint in all five languages, widened the native
   browse action for long translations and set an explicit disabled text color.
   The full stored path remains editable and is never shortened in configuration;
   only its on-screen rendering is clipped or scrolled.
5. Rechecked header geometry at the 320×240 logical floor, intermediate compact
   sizes and desktop layouts. Static source checks find no remaining menu-logo
   draw calls or references to the removed GUI texture.
6. `clean build --no-daemon --warning-mode all` succeeds for version 1.2.4.

## fixed83 notification redesign and boundary audit

1. Replaced the flat rectangular Modern toast with a smaller, wider notification
   card: stepped pixel corners, in-bounds shadow, layered surface, subtle top
   highlight, restrained accent rail, adaptive icon tile and a draining lifetime
   indicator. No Chronicle branding is reintroduced into the notification.
2. Fixed the empty-card entrance bug at its source. Minecraft 26.2 reports
   `fullyVisibleForMs == 0` throughout its native 600ms toast slide; the old code
   used that value as content alpha, making title, message and icon fully
   transparent. The complete card now uses one motion transform and full content
   opacity, so every element becomes visible together as it enters.
3. Modern toast width/height are rebalanced for faster scanning, with safe GUI
   bounds, an adaptive icon-free fallback, Unicode-safe title clipping and up to
   two body lines when height permits. The customizer still calls the exact same
   renderer as real notifications.
4. Rebuilt sound-settings geometry around one immutable layout snapshot shared by
   `init` and rendering. Normal screens use the common 17px caption-to-control
   offset and 12px section gap; compact and high-scale layouts remove secondary
   captions before they can collide while retaining all controls.
5. Removed hard minimum widths that could push Browse, volume and footer buttons
   outside a narrow sound panel. The shared content inset can no longer consume
   more than half its panel.
6. Added narrow-GUI containment for reminder-list text, empty-state copy, weekly
   day buttons, the HSV marker/current-color swatch and palette cards. These
   elements now adapt or clip inside their real bounds instead of relying on a
   normal desktop width.
7. Re-audited the scheduler, duplicate suppression, one-shot actions, placeholder
   fallback, atomic config save/rollback, async audio generation tokens, focus,
   scrolling, scissor order, footer hit-testing and resize rebuilds. No additional
   functional regression was found.
8. `build --no-daemon` succeeds for version 1.2.5 against Minecraft 26.2, Fabric
   Loader 0.19.3, Fabric API 0.157.0+26.2, Loom 1.17.19 and Java 25.

## fixed84 pixel-stable toast motion

1. Removed the two-pixel vertical settle from the Modern toast transform. Because
   Minecraft holds `fullyVisibleForMs` at zero during its native slide, rounding
   that offset kept the card one pixel below its final baseline and then moved it
   upward after the entrance completed.
2. The complete notification now animates only on X; its frame, text, icon,
   progress line and shadow keep exactly the same Y coordinate on every frame.
3. `build --no-daemon --warning-mode all` succeeds for version 1.2.6.

## fixed85 whole-project correctness and geometry audit

1. Replaced fractional screen scale/translation with a pixel-stable fade. Visual
   bounds and pointer hit boxes now remain in the same coordinate system for the
   complete open and close transition.
2. Made borderless-field padding part of its real content geometry. Rendered
   text, scrolling, caret, selection, click/drag positions and IME candidate
   placement now share one inset, including long custom-sound paths.
3. Corrected responsive layouts at the 320x240 logical minimum: schedule modes
   remain on one usable row, the main footer leaves at least one reminder card,
   save errors have a non-overlapping strip, and sound path/Browse widths remain
   fully contained. Hidden or inactive controls no longer retain focus.
4. Unified preview and real notification geometry, added dynamic resize/GUI-scale
   sizing and stable three-slot allocation, removed the second custom slide, and
   made disabled animations immediate. Unicode/CJK and long unbroken tokens wrap
   without dropping their remainder; placeholders and progress also match the
   real notification during preview and tests.
5. Fixed exact interval timing with millisecond deadlines and safe migration of
   legacy minute data. Interval checks are independent of the wall-clock minute
   gate and handle forward/backward clock changes without catch-up spam.
6. Prevented editor/scheduler races from restoring stale enabled/deadline fields
   or silently recreating a reminder deleted by a one-shot action. Failed runtime
   saves remain dirty, retry with bounded backoff and receive a final stop-time
   attempt; the main screen exposes the persistence error.
7. Config loading now distinguishes malformed JSON from transient I/O failure.
   Corrupt input receives a unique recoverable backup before atomic replacement;
   temporary read failures are never overwritten with defaults. Input size is
   capped at 4 MiB and reminder count at 512.
8. Reworked custom audio into latest-request-wins playback with deterministic
   cancellation, Clip/cache synchronization, Minecraft MASTER x UI volume and
   complete shutdown. OFF/VANILLA transitions and screen close stop stale custom
   tests instead of allowing delayed playback.
9. Added a bounded 32-item notification queue with paced delivery, duplicate
   aggregation and a localized overflow summary. Five locale files contain the
   same 154 keys and matching format parameters.
10. Enabled Java 25 `-Xlint:all`, removed dead helpers and completed static scans
    for debug prints, blocking sleeps and unfinished markers. A clean version
    1.2.7 build is required as the release gate.
