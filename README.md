![Chronicle logo](src/main/resources/assets/chronicle/icon.png)

Chronicle 26.2 — fixed85 / 1.2.7

Fabric client mod for scheduled reminders on Minecraft 26.2.

This revision fixes the clipped/overlapping color values in Toast Customizer and
performs a deeper responsive-UI, input, scissor, focus, scheduler and persistence
audit. See `AUDIT_NOTES.md` for the verified findings. The project builds against
Fabric Loader 0.19.3, Fabric API 0.157.0+26.2, Loom 1.17.19 and Java 25.

fixed76 redesigns the notification itself and the color workspace. Presets now
occupy the free area beside the HSV picker on wide compact screens, each preset is
one clear action, and the hue strip is continuous. The live preview supports a
minimal MODERN frame and a resource-pack-aware VANILLA frame. A saved animation
setting controls smooth button feedback, screen open/close transitions and toast
motion; disabling it returns navigation and feedback to immediate state changes.

fixed77 corrects the Vanilla notification geometry. It now follows Minecraft
26.2 `SystemToast` metrics instead of stretching the vanilla nine-slice over the
larger modern card: native 160px minimum width, compact one-message height and
12px line spacing. Vanilla uses native 1x typography and no custom user icon;
the customizer communicates this and disables irrelevant scale controls.

fixed78 adds the Smart in-game reminders feature set. Chronicle opens through a
native Minecraft key mapping and can be rebound in Minecraft Controls. Reminders
can repeat, disable
themselves after the first display, or delete themselves. Friendly placeholders
such as `{world}`, `{coords}`, `{biome}` and `{dimension}` are supported together
with placeholders registered by Text Placeholder API.

Notification audio can use the quiet vanilla cue, be disabled, or load a local
MP3/OGG/WAV/AIFF/AU file. Custom files are decoded off the render thread, limited to
16 MB and 30 seconds, and cached after the first use. The complete interface is
localized for English, Russian, Simplified Chinese, Spanish and German and
follows Minecraft's active language.

fixed79 installs the supplied Chronicle clock mark as the Fabric/Mod Menu icon,
screen-header identity and a compact brand mark in both notification frames. The
small UI texture uses a tighter crop for readability without altering the supplied
artwork. Keyboard focus is now drawn as a quiet accessibility outline instead of
being mistaken for mouse hover or a selected button.

fixed80 corrects the 26.2 texture-region overload that displayed only the
top-left corner of the header logo. The metadata icon is an explicit opaque
128x128 PNG with the full supplied canvas, while the UI uses a separate readable
crop. The default menu key is now `J`. A short localized `TEST` action is fixed in
the main header and remains available in the toast customizer footer.

fixed81 removes the Chronicle brand mark from both notification styles so the
toast contains only the user's icon and reminder content. Header and Mod Menu
logo textures now use real PNG transparency instead of a baked dark square. The
sound screen has a full text-line gutter between its format hint and volume
caption, its native file dialog filters supported audio files, and all toast test
actions play the currently saved OFF/VANILLA/CUSTOM sound configuration. MP3 is
decoded on Chronicle's bounded audio worker through the bundled JLayer library.

fixed82 removes decorative logos from every Chronicle settings screen and keeps
the identity only where users expect an application icon: Fabric metadata and
Mod Menu. That icon is now a self-contained rounded graphite tile with a cyan
frame and transparent safety margin. Header titles reclaim the former logo space,
improving scanability and reducing truncation on small GUI scales. The custom
sound path now has a real four-pixel text inset on both sides, width clipping,
an empty-state hint and a consistent disabled color, so long Windows paths no
longer touch or escape the field border.

fixed83 replaces the Modern notification with a compact stepped-corner card,
layered surface, restrained accent rail, adaptive icon tile, safe one/two-line
text wrapping and a remaining-time indicator. The card and all of its content now
share one motion transform; title, message and icon are visible during Minecraft's
native 600ms slide instead of appearing only after it finishes.

The sound screen now derives widget and caption positions from one immutable
layout snapshot. Standard spacing follows the shared 17px caption offset and 12px
section gap, while compact layouts remove secondary captions before they can
overlap. A final narrow-GUI audit clamps shared panel insets, sound control widths,
reminder-row text, weekday buttons, color-picker handles/swatches and palette
cards to their real bounds.

fixed84 removes the final vertical component from Modern toast motion. Its Y
coordinate is now pixel-stable throughout Minecraft's entrance and exit slides,
so the card no longer rises by one pixel when the intro finishes.

fixed85 completes a whole-project reliability and pixel-geometry audit. Screen
transitions are now fade-only, so one-pixel lines, text and hit boxes never drift
onto fractional coordinates. Responsive layouts keep the complete schedule,
sound and list controls usable at Minecraft's 320x240 logical floor; field
padding now matches caret, selection, mouse and IME coordinates.

Notification dimensions update safely after resize or GUI-scale changes, preview
and real-toast geometry share the same renderer, long Unicode/CJK text wraps
without loss, and animation-off is honored by the native toast position. Interval
deadlines now use exact milliseconds, concurrent editor/scheduler changes cannot
resurrect deleted or disabled reminders, failed saves retry safely, burst traffic
is bounded and summarized, and custom audio has deterministic cancellation and
shutdown. Config loading also distinguishes temporary I/O failure from corrupt
JSON and keeps recoverable backups before replacing damaged data.
