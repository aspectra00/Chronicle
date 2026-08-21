package com.aspectra00.chronicle.client.config;

import com.aspectra00.chronicle.client.ChronicleI18n;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReminderConfig {
    public enum LoadStatus {
        LOADED,
        NEW_CONFIG,
        MIGRATED_LEGACY,
        RECOVERED_CORRUPT,
        IO_ERROR
    }

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Reminder.ScheduleType.class,
                    (JsonDeserializer<Reminder.ScheduleType>) (json, type, context) -> {
                        try {
                            return Reminder.ScheduleType.valueOf(json.getAsString().toUpperCase(Locale.ROOT));
                        } catch (RuntimeException ignored) {
                            return Reminder.ScheduleType.DAILY;
                        }
                    })
            .registerTypeAdapter(Reminder.AfterTriggerAction.class,
                    (JsonDeserializer<Reminder.AfterTriggerAction>) (json, type, context) -> {
                        try {
                            return Reminder.AfterTriggerAction.valueOf(json.getAsString().toUpperCase(Locale.ROOT));
                        } catch (RuntimeException ignored) {
                            return Reminder.AfterTriggerAction.KEEP;
                        }
                    })
            .registerTypeAdapter(ReminderTrigger.Type.class,
                    (JsonDeserializer<ReminderTrigger.Type>) (json, type, context) -> {
                        try {
                            return ReminderTrigger.Type.valueOf(json.getAsString().toUpperCase(Locale.ROOT));
                        } catch (RuntimeException ignored) {
                            return ReminderTrigger.Type.HEALTH_BELOW;
                        }
                    })
            .setPrettyPrinting()
            .create();
    private static final String FILE_NAME = "chronicle.json";
    private static final String LEGACY_FILE_NAME = "daily-reminders.json";
    private static final long MAX_CONFIG_BYTES = 4L * 1024L * 1024L;
    public static final int MAX_REMINDERS = 512;
    public static final int DEFAULT_SNOOZE_MINUTES = 5;
    public List<Reminder> reminders = new ArrayList<>();
    /** User-facing time format preference. Reminder values stay 24-hour internally. */
    public boolean use24HourFormat = true;

    // Toast customization.
    public String toastStyle = "MINIMAL";
    public int toastBackgroundColor = 0xFF10151C;
    public int toastBorderColor = 0xFF252E3A;
    public int toastAccentColor = 0xFF8FB3E8;
    public int toastTitleColor = 0xFFE7ECF2;
    public int toastMessageColor = 0xFFB3BFCC;
    public int toastIconColor = 0xFFE7ECF2;
    public String toastIcon = "!";
    public String toastTitle = "CHRONICLE";
    public float toastTitleScale = 1.00f;
    public float toastMessageScale = 1.00f;
    public float toastIconScale = 2.00f;
    /** Overall notification shell: MODERN or VANILLA. */
    public String toastFrameStyle = "MODERN";
    /** Chronicle screen, button and toast micro-animations. */
    public boolean animationsEnabled = true;
    public boolean toastActionsEnabled = true;
    public int toastSnoozeMinutes = DEFAULT_SNOOZE_MINUTES;

    // Notification sound: OFF, VANILLA or CUSTOM.
    public String notificationSoundMode = "VANILLA";
    public String customSoundPath = "";
    public float notificationSoundVolume = 0.75f;

    private transient Path path;
    private transient String lastSaveError;
    private transient String loadError;
    private transient LoadStatus loadStatus = LoadStatus.NEW_CONFIG;
    private transient boolean initialSaveRequired;
    private transient boolean writesBlocked;

    public static ReminderConfig load(Path configDir) {
        ReminderConfig config = new ReminderConfig();
        config.path = configDir.resolve(FILE_NAME);
        Path sourcePath = config.path;
        boolean legacySource = false;

        try {
            Files.createDirectories(configDir);
            boolean sourceExists = Files.exists(sourcePath);
            if (!sourceExists && !Files.notExists(sourcePath)) {
                throw new IOException("Unable to determine whether " + sourcePath + " exists");
            }
            if (!sourceExists) {
                Path legacyPath = configDir.resolve(LEGACY_FILE_NAME);
                if (Files.exists(legacyPath)) {
                    sourcePath = legacyPath;
                    legacySource = true;
                    sourceExists = true;
                } else if (!Files.notExists(legacyPath)) {
                    throw new IOException("Unable to determine whether " + legacyPath + " exists");
                } else {
                    config.loadStatus = LoadStatus.NEW_CONFIG;
                    config.initialSaveRequired = true;
                }
            }
            if (sourceExists) {
                try {
                    long fileSize = Files.size(sourcePath);
                    if (fileSize > MAX_CONFIG_BYTES) {
                        throw new JsonParseException("Configuration exceeds 4 MiB");
                    }
                    String json = Files.readString(sourcePath, StandardCharsets.UTF_8);
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    ReminderConfig loaded = GSON.fromJson(root, ReminderConfig.class);
                    if (loaded != null && loaded.reminders != null
                            && loaded.reminders.size() > MAX_REMINDERS) {
                        throw new JsonParseException("Configuration contains more than "
                                + MAX_REMINDERS + " reminders");
                    }
                    if (loaded != null) config.copyPresentValues(root, loaded);
                    config.loadStatus = legacySource ? LoadStatus.MIGRATED_LEGACY : LoadStatus.LOADED;
                    config.initialSaveRequired = legacySource;
                } catch (RuntimeException parseFailure) {
                    config.loadError = errorDetail(parseFailure);
                    if (backupBrokenConfig(sourcePath)) {
                        config.loadStatus = LoadStatus.RECOVERED_CORRUPT;
                        config.initialSaveRequired = true;
                    } else {
                        config.loadStatus = LoadStatus.IO_ERROR;
                        config.writesBlocked = true;
                    }
                    config.reminders = new ArrayList<>();
                }
            }
        } catch (IOException ioFailure) {
            // An I/O failure is not evidence that the JSON is corrupt. In particular,
            // never replace a valid config after a transient lock or permission error.
            config.loadError = errorDetail(ioFailure);
            config.loadStatus = LoadStatus.IO_ERROR;
            config.writesBlocked = true;
            config.initialSaveRequired = false;
            config.reminders = new ArrayList<>();
        }

        // No starter reminders. A first launch stays empty.
        config.ensureValid();
        return config;
    }

    private void copyPresentValues(JsonObject root, ReminderConfig loaded) {
        if (root.has("reminders") && loaded.reminders != null) reminders = loaded.reminders;
        if (root.has("use24HourFormat") && !root.get("use24HourFormat").isJsonNull()) {
            use24HourFormat = loaded.use24HourFormat;
        }
        if (root.has("toastStyle") && loaded.toastStyle != null) toastStyle = loaded.toastStyle;
        if (root.has("toastBackgroundColor")) toastBackgroundColor = loaded.toastBackgroundColor;
        if (root.has("toastBorderColor")) toastBorderColor = loaded.toastBorderColor;
        if (root.has("toastAccentColor")) toastAccentColor = loaded.toastAccentColor;
        if (root.has("toastTitleColor")) toastTitleColor = loaded.toastTitleColor;
        if (root.has("toastMessageColor")) toastMessageColor = loaded.toastMessageColor;
        if (root.has("toastIconColor")) toastIconColor = loaded.toastIconColor;
        if (root.has("toastIcon") && loaded.toastIcon != null) toastIcon = loaded.toastIcon;
        if (root.has("toastTitle") && loaded.toastTitle != null) toastTitle = loaded.toastTitle;
        if (root.has("toastTitleScale")) toastTitleScale = loaded.toastTitleScale;
        if (root.has("toastMessageScale")) toastMessageScale = loaded.toastMessageScale;
        if (root.has("toastIconScale")) toastIconScale = loaded.toastIconScale;
        if (root.has("toastFrameStyle") && loaded.toastFrameStyle != null) {
            toastFrameStyle = loaded.toastFrameStyle;
        }
        if (root.has("animationsEnabled") && !root.get("animationsEnabled").isJsonNull()) {
            animationsEnabled = loaded.animationsEnabled;
        }
        if (root.has("toastActionsEnabled") && !root.get("toastActionsEnabled").isJsonNull()) {
            toastActionsEnabled = loaded.toastActionsEnabled;
        }
        if (root.has("toastSnoozeMinutes") && !root.get("toastSnoozeMinutes").isJsonNull()) {
            toastSnoozeMinutes = loaded.toastSnoozeMinutes;
        }
        if (root.has("notificationSoundMode") && loaded.notificationSoundMode != null) {
            notificationSoundMode = loaded.notificationSoundMode;
        }
        if (root.has("customSoundPath") && loaded.customSoundPath != null) {
            customSoundPath = loaded.customSoundPath;
        }
        if (root.has("notificationSoundVolume")) {
            notificationSoundVolume = loaded.notificationSoundVolume;
        }
    }

    private static boolean backupBrokenConfig(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                String baseName = path.getFileName().toString() + ".broken-" + System.currentTimeMillis();
                Path backup = path.resolveSibling(baseName);
                int suffix = 1;
                while (Files.exists(backup)) {
                    backup = path.resolveSibling(baseName + "-" + suffix++);
                }
                // Copy first. The original stays in place until a complete replacement
                // has been atomically written, so failed recovery cannot destroy it.
                Files.copy(path, backup);
                return true;
            }
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
        return false;
    }

    private static String errorDetail(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    public boolean save() {
        lastSaveError = null;
        if (writesBlocked) {
            lastSaveError = ChronicleI18n.tr("error.load_config",
                    loadError == null ? LoadStatus.IO_ERROR.name() : loadError);
            return false;
        }
        if (path == null) {
            lastSaveError = ChronicleI18n.tr("error.config_path");
            return false;
        }
        try {
            Files.createDirectories(path.getParent());
            Path tempPath = path.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            try {
                Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                // Providers differ in the exception used when atomic replacement is
                // unavailable (Windows commonly reports a generic IOException).
                try {
                    Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException replaceFailure) {
                    replaceFailure.addSuppressed(atomicFailure);
                    throw replaceFailure;
                }
            }
            initialSaveRequired = false;
            return true;
        } catch (IOException | RuntimeException ex) {
            String detail = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName() : ex.getMessage();
            lastSaveError = ChronicleI18n.tr("error.save_config", detail);
            try {
                Files.deleteIfExists(path.resolveSibling(FILE_NAME + ".tmp"));
            } catch (IOException ignoredCleanup) {
                // Best-effort cleanup only.
            }
            return false;
        }
    }

    public String getLastSaveError() {
        return lastSaveError;
    }

    public String getLoadError() {
        return loadError;
    }

    public LoadStatus getLoadStatus() {
        return loadStatus;
    }

    public boolean shouldSaveAfterLoad() {
        return initialSaveRequired && !writesBlocked;
    }

    public boolean isWriteBlocked() {
        return writesBlocked;
    }

    public void ensureValid() {
        long nowMillis = System.currentTimeMillis();
        if (reminders == null) {
            reminders = new ArrayList<>();
        }

        reminders.removeIf(r -> r == null);
        for (Reminder r : reminders) {
            r.hour = Math.max(0, Math.min(23, r.hour));
            r.minute = Math.max(0, Math.min(59, r.minute));
            if (r.message == null || r.message.isBlank()) {
                r.message = ChronicleI18n.tr("default.reminder");
            } else {
                r.message = truncateSafely(r.message.trim(), 80);
            }
            if (r.scheduleType == null) {
                r.scheduleType = Reminder.ScheduleType.DAILY;
            }
            if (r.afterTriggerAction == null) {
                r.afterTriggerAction = Reminder.AfterTriggerAction.KEEP;
            }
            if (r.trigger == null) {
                r.trigger = new ReminderTrigger();
            }
            if (r.trigger.type == null) {
                r.trigger.type = ReminderTrigger.Type.HEALTH_BELOW;
            }
            r.trigger.threshold = switch (r.trigger.type) {
                case HEALTH_BELOW, AIR_BELOW, DURABILITY_BELOW ->
                        Math.max(1, Math.min(100, r.trigger.threshold));
                case HUNGER_BELOW -> Math.max(0, Math.min(20, r.trigger.threshold));
                default -> r.trigger.threshold;
            };
            Identifier triggerTarget = r.trigger.target == null
                    ? null : Identifier.tryParse(r.trigger.normalizedTarget());
            if (triggerTarget == null || triggerTarget.toString().length() > 128) {
                r.trigger.target = "minecraft:overworld";
            } else {
                r.trigger.target = triggerTarget.toString();
            }
            r.trigger.x = Math.max(-30_000_000, Math.min(30_000_000, r.trigger.x));
            r.trigger.z = Math.max(-30_000_000, Math.min(30_000_000, r.trigger.z));
            r.trigger.radius = Math.max(1, Math.min(30_000_000, r.trigger.radius));
            r.intervalMinutes = Math.max(1, Math.min(7 * 24 * 60, r.intervalMinutes));
            if (r.scheduleType == Reminder.ScheduleType.INTERVAL) {
                long intervalMillis = r.intervalMinutes * 60_000L;
                if (r.nextTriggerEpochMillis <= 0L && r.lastTriggeredEpochMinute > 0L) {
                    // The old field discarded seconds. Migrate from the latest possible
                    // instant in that minute, so an upgraded reminder cannot fire early.
                    long legacyLastMillis = saturatedMultiply(r.lastTriggeredEpochMinute, 60_000L);
                    legacyLastMillis = saturatedAdd(legacyLastMillis, 59_999L);
                    r.nextTriggerEpochMillis = saturatedAdd(legacyLastMillis, intervalMillis);
                    r.lastTriggeredEpochMinute = 0L;
                    initialSaveRequired = true;
                }
                long furthestReasonableDeadline = saturatedAdd(nowMillis,
                        7L * 24L * 60L * 60_000L + 60_000L);
                if (r.nextTriggerEpochMillis < 0L
                        || r.nextTriggerEpochMillis > furthestReasonableDeadline) {
                    r.nextTriggerEpochMillis = 0L;
                    r.lastTriggeredEpochMinute = 0L;
                    initialSaveRequired = true;
                }
            }
            if (r.weeklyDays == null || r.weeklyDays.length != 7) {
                r.ensureWeeklyDays();
            }
            if (r.scheduleType == Reminder.ScheduleType.WEEKLY && !r.hasAnyWeeklyDay()) {
                r.weeklyDays = new boolean[]{true, true, true, true, true, true, true};
            }
        }

        if (toastStyle == null || toastStyle.isBlank()) toastStyle = "MINIMAL";
        toastStyle = switch (toastStyle.toUpperCase(java.util.Locale.ROOT)) {
            case "MINIMAL", "GLASS", "MATRIX", "NEON" -> toastStyle.toUpperCase(java.util.Locale.ROOT);
            default -> "MINIMAL";
        };
        if (toastIcon == null || toastIcon.isBlank()) toastIcon = "!";
        if (toastTitle == null || toastTitle.isBlank()) toastTitle = "CHRONICLE";
        toastIcon = toastIcon.trim();
        int iconEnd = toastIcon.offsetByCodePoints(0, Math.min(2, toastIcon.codePointCount(0, toastIcon.length())));
        toastIcon = toastIcon.substring(0, iconEnd);
        toastTitle = truncateSafely(toastTitle.trim(), 40);
        toastTitleScale = crispScale(toastTitleScale, 1.00f);
        toastMessageScale = crispScale(toastMessageScale, 1.00f);
        toastIconScale = crispScale(toastIconScale, 2.00f);
        toastFrameStyle = toastFrameStyle == null ? "MODERN"
                : switch (toastFrameStyle.toUpperCase(java.util.Locale.ROOT)) {
                    case "MODERN", "VANILLA" -> toastFrameStyle.toUpperCase(java.util.Locale.ROOT);
                    default -> "MODERN";
                };
        toastSnoozeMinutes = Math.max(1, Math.min(24 * 60, toastSnoozeMinutes));
        notificationSoundMode = notificationSoundMode == null ? "VANILLA"
                : switch (notificationSoundMode.toUpperCase(java.util.Locale.ROOT)) {
                    case "OFF", "VANILLA", "CUSTOM" -> notificationSoundMode.toUpperCase(java.util.Locale.ROOT);
                    default -> "VANILLA";
                };
        if (customSoundPath == null) customSoundPath = "";
        customSoundPath = truncateSafely(customSoundPath.trim(), 1024);
        if (!Float.isFinite(notificationSoundVolume)) notificationSoundVolume = 0.75f;
        notificationSoundVolume = Math.max(0.0f, Math.min(1.0f, notificationSoundVolume));
    }

    private static float crispScale(float value, float fallback) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            value = fallback;
        }
        return Math.max(1.0f, Math.min(2.0f, Math.round(value)));
    }

    private static String truncateSafely(String value, int maxUtf16Length) {
        if (value.length() <= maxUtf16Length) return value;
        int end = maxUtf16Length;
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static long saturatedMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) return 0L;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

}
