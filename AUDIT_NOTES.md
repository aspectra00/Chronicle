# Development notes

## Target

| Component | Version |
|---|---:|
| Minecraft | 1.21.6 |
| Fabric Loader | 0.16.13 minimum; 0.19.3 recommended |
| Fabric API | 0.128.2+1.21.6 |
| Fabric Loom | 1.17.19 |
| Java | 21 |

Chronicle is client-side only. The main source set contains mod metadata and the common initializer; client behavior lives under `src/client`.

## Runtime

- The scheduler uses wall-clock minutes for daily and weekly reminders.
- Intervals use exact millisecond deadlines.
- Trigger rules fire on a false-to-true transition.
- Weekly reminders ignore after-trigger actions and continue on every selected day.
- Notification bursts are queued and rate-limited.
- Snooze creates a one-shot interval reminder only after the configuration is saved.
- Test and customizer previews are not written to notification history.
- Watch targets are scoped to the current world or server.

## Persistence

The primary configuration file is `config/chronicle.json`. Older `daily-reminders.json` data is migrated on load.

Configuration writes use a temporary file and replacement. Invalid JSON is preserved as a backup instead of being overwritten. Temporary I/O failures keep the in-memory state and are retried by the client loop.

Limits:

| Item | Limit |
|---|---:|
| Configuration file | 4 MiB |
| Reminders | 512 |
| History entries | 500 |
| Watch targets | 256 |
| Custom background | 8 MiB, 4096 px per side, 4 megapixels |
| Custom sound | 16 MiB, 30 seconds |

## Release checks

Run from the repository root with Java 21:

```powershell
.\gradlew.bat clean build
```

Before publishing:

1. Confirm `version` in `gradle.properties`.
2. Build with compiler lint enabled.
3. Check the remapped JAR in `build/libs`.
4. Launch the client and verify the main screen, editor, history, watches, notification preview, Snooze, and Dismiss.
5. Verify a daily, weekly, interval, and condition-based reminder.
6. Confirm the release tag points to the published commit.

## Source conventions

- Keep serialized configuration field names stable.
- Prefer Fabric events over mixins when an event covers the hook.
- Keep rendering and hit-box calculations on the same integer geometry.
- Add user-facing text through the language files.
- Do not block the render thread with file or audio decoding.
