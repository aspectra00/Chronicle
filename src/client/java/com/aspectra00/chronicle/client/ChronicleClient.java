package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.config.Reminder;
import com.aspectra00.chronicle.client.config.ReminderConfig;
import com.aspectra00.chronicle.client.config.ReminderTrigger;
import com.aspectra00.chronicle.client.gui.CustomReminderToast;
import com.aspectra00.chronicle.client.gui.NotificationSoundScreen;
import com.aspectra00.chronicle.client.gui.ReminderConfigScreen;
import com.aspectra00.chronicle.client.gui.ReminderEditorScreen;
import com.aspectra00.chronicle.client.gui.ToastCustomizerScreen;
import com.aspectra00.chronicle.client.gui.ToastInteractionManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class ChronicleClient implements ClientModInitializer {
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("chronicle", "main"));
    private static final long TEST_TOAST_DEBOUNCE_MS = 500L;
    private static final long INTERVAL_SCAN_PERIOD_MS = 250L;
    private static final long NOTIFICATION_DRAIN_GAP_MS = 750L;
    private static final long SAVE_RETRY_MAX_MS = 30_000L;
    private static final int MAX_PENDING_NOTIFICATIONS = 32;
    private static final ArrayDeque<PendingNotification> PENDING_NOTIFICATIONS = new ArrayDeque<>();
    private static final IdentityHashMap<Reminder, PendingNotification> PENDING_BY_REMINDER =
            new IdentityHashMap<>();
    private static final IdentityHashMap<Reminder, TriggerRuntimeState> TRIGGER_STATES =
            new IdentityHashMap<>();

    public static ReminderConfig CONFIG;
    public static KeyMapping OPEN_MENU_KEY;
    public static long CONFIG_REVISION;
    private static volatile String runtimeConfigError;
    private static long lastTestToastAt = Long.MIN_VALUE;
    private static int overflowNotificationCount;
    private long lastCheckedEpochMinute = Long.MIN_VALUE;
    private long lastObservedEpochMillis = Long.MIN_VALUE;
    private long nextIntervalScanAt = Long.MIN_VALUE;
    private long nextNotificationAt = Long.MIN_VALUE;
    private long nextSaveRetryAt = Long.MIN_VALUE;
    private int saveFailureCount;
    private boolean configDirty;

    private static final class PendingNotification {
        private final Reminder source;
        private final String message;
        private int occurrences = 1;

        private PendingNotification(Reminder source, String message) {
            this.source = source;
            this.message = message;
        }
    }

    private static final class TriggerRuntimeState {
        private final ReminderTrigger definition;
        private boolean matched;

        private TriggerRuntimeState(ReminderTrigger definition, boolean matched) {
            this.definition = definition.copy();
            this.matched = matched;
        }
    }

    @Override
    public void onInitializeClient() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        CONFIG = ReminderConfig.load(configDir);
        CONFIG.ensureValid();
        PENDING_NOTIFICATIONS.clear();
        PENDING_BY_REMINDER.clear();
        overflowNotificationCount = 0;
        TRIGGER_STATES.clear();
        runtimeConfigError = CONFIG.getLoadStatus() == ReminderConfig.LoadStatus.IO_ERROR
                && CONFIG.getLoadError() != null
                ? ChronicleI18n.tr("error.load_config", CONFIG.getLoadError()) : null;
        if (CONFIG.shouldSaveAfterLoad() && !CONFIG.save()) {
            markConfigDirty(Util.getMillis());
            runtimeConfigError = CONFIG.getLastSaveError();
        }

        OPEN_MENU_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chronicle.open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_J,
                KEY_CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::onClientStopping);
        ToastInteractionManager.initialize();
    }

    private void onClientTick(Minecraft client) {
        while (OPEN_MENU_KEY != null && OPEN_MENU_KEY.consumeClick()) {
            var currentScreen = client.gui.screen();
            boolean chronicleOpen = currentScreen instanceof ReminderConfigScreen
                    || currentScreen instanceof ReminderEditorScreen
                    || currentScreen instanceof ToastCustomizerScreen
                    || currentScreen instanceof NotificationSoundScreen;
            if (!chronicleOpen) {
                client.gui.setScreen(new ReminderConfigScreen(currentScreen));
            }
        }
        if (CONFIG == null) return;

        final long monotonicNow = Util.getMillis();
        final long epochNow = System.currentTimeMillis();
        final long currentEpochMinute = epochNow / 60_000L;
        boolean clockRolledBack = lastObservedEpochMillis != Long.MIN_VALUE
                && epochNow + 1_000L < lastObservedEpochMillis;
        lastObservedEpochMillis = epochNow;

        boolean configChanged = false;
        List<Reminder> deleteAfterTrigger = new ArrayList<>();

        if (clockRolledBack || nextIntervalScanAt == Long.MIN_VALUE
                || monotonicNow >= nextIntervalScanAt) {
            nextIntervalScanAt = monotonicNow + INTERVAL_SCAN_PERIOD_MS;
            for (Reminder reminder : CONFIG.reminders) {
                if (reminder == null || !reminder.enabled
                        || reminder.scheduleType != Reminder.ScheduleType.INTERVAL) continue;
                if (clockRolledBack || reminder.nextTriggerEpochMillis <= 0L) {
                    reminder.resetIntervalTimer(epochNow);
                    configChanged = true;
                    continue;
                }
                if (epochNow >= reminder.nextTriggerEpochMillis) {
                    enqueueReminder(reminder, reminder.message);
                    reminder.resetIntervalTimer(epochNow);
                    applyAfterTrigger(reminder, deleteAfterTrigger);
                    configChanged = true;
                }
            }
            for (Reminder reminder : CONFIG.reminders) {
                if (reminder == null || !reminder.enabled
                        || reminder.scheduleType != Reminder.ScheduleType.TRIGGER) continue;
                ReminderTrigger trigger = reminder.trigger;
                ReminderTriggerEvaluator.Result result = ReminderTriggerEvaluator.evaluate(client, trigger);
                if (result == ReminderTriggerEvaluator.Result.UNAVAILABLE) continue;
                boolean matched = result == ReminderTriggerEvaluator.Result.MATCH;
                TriggerRuntimeState state = TRIGGER_STATES.get(reminder);
                if (state == null || !state.definition.sameDefinition(trigger)) {
                    TRIGGER_STATES.put(reminder, new TriggerRuntimeState(trigger, matched));
                    continue;
                }
                if (matched && !state.matched) {
                    enqueueReminder(reminder, reminder.message);
                    Reminder.AfterTriggerAction action = reminder.afterTriggerAction == null
                            ? Reminder.AfterTriggerAction.KEEP : reminder.afterTriggerAction;
                    applyAfterTrigger(reminder, deleteAfterTrigger);
                    if (action != Reminder.AfterTriggerAction.KEEP) configChanged = true;
                }
                state.matched = matched;
            }
            TRIGGER_STATES.keySet().removeIf(reminder -> reminder == null
                    || !reminder.enabled
                    || reminder.scheduleType != Reminder.ScheduleType.TRIGGER
                    || !CONFIG.reminders.contains(reminder));
        }

        if (currentEpochMinute != lastCheckedEpochMinute) {
            final ZoneId zone = ZoneId.systemDefault();
            // Replay at most a short missed window. This catches brief stalls without
            // creating a stale burst after a long pause or system sleep.
            long firstMinute;
            if (lastCheckedEpochMinute == Long.MIN_VALUE
                    || currentEpochMinute < lastCheckedEpochMinute) {
                firstMinute = currentEpochMinute;
            } else {
                long gap = currentEpochMinute - lastCheckedEpochMinute;
                firstMinute = gap <= 15L ? lastCheckedEpochMinute + 1L : currentEpochMinute;
            }

            for (long epochMinute = firstMinute; epochMinute <= currentEpochMinute; epochMinute++) {
                LocalDateTime now = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(epochMinute * 60L), zone);
                int weekDayIndex = now.getDayOfWeek().getValue() - 1;
                long wallClockMinute = now.toLocalDate().toEpochDay() * 1440L
                        + now.getHour() * 60L + now.getMinute();

                for (Reminder reminder : CONFIG.reminders) {
                    if (reminder == null || !reminder.enabled) continue;
                    Reminder.ScheduleType type = reminder.scheduleType == null
                            ? Reminder.ScheduleType.DAILY : reminder.scheduleType;
                    if (type == Reminder.ScheduleType.INTERVAL
                            || type == Reminder.ScheduleType.TRIGGER) continue;

                    boolean due = switch (type) {
                        case DAILY -> reminder.hour == now.getHour()
                                && reminder.minute == now.getMinute();
                        case WEEKLY -> reminder.hasWeeklyDay(weekDayIndex)
                                && reminder.hour == now.getHour()
                                && reminder.minute == now.getMinute();
                        case INTERVAL, TRIGGER -> false;
                    };
                    if (due && reminder.lastTriggeredWallClockMinute != wallClockMinute) {
                        enqueueReminder(reminder, reminder.message);
                        reminder.lastTriggeredWallClockMinute = wallClockMinute;
                        applyAfterTrigger(reminder, deleteAfterTrigger);
                        configChanged = true;
                    }
                }
            }
            lastCheckedEpochMinute = currentEpochMinute;
        }

        if (!deleteAfterTrigger.isEmpty()) {
            CONFIG.reminders.removeAll(deleteAfterTrigger);
            configChanged = true;
        }
        if (configChanged) {
            CONFIG_REVISION++;
            markConfigDirty(monotonicNow);
        }
        attemptDirtySave(monotonicNow);
        drainNotifications(client, monotonicNow);
    }

    private static void enqueueReminder(Reminder reminder, String message) {
        PendingNotification existing = PENDING_BY_REMINDER.get(reminder);
        if (existing != null) {
            existing.occurrences++;
            return;
        }
        PendingNotification pending = new PendingNotification(reminder,
                message == null ? "" : message);
        if (PENDING_NOTIFICATIONS.size() >= MAX_PENDING_NOTIFICATIONS) {
            overflowNotificationCount++;
            return;
        }
        PENDING_BY_REMINDER.put(reminder, pending);
        PENDING_NOTIFICATIONS.addLast(pending);
    }

    private void drainNotifications(Minecraft client, long monotonicNow) {
        if (monotonicNow < nextNotificationAt) return;
        if (PENDING_NOTIFICATIONS.isEmpty()) {
            if (overflowNotificationCount <= 0) return;
            int hiddenCount = overflowNotificationCount;
            overflowNotificationCount = 0;
            displayReminder(client, ChronicleI18n.tr("summary.more_reminders", hiddenCount));
            nextNotificationAt = monotonicNow + NOTIFICATION_DRAIN_GAP_MS;
            return;
        }
        PendingNotification pending = PENDING_NOTIFICATIONS.removeFirst();
        PENDING_BY_REMINDER.remove(pending.source);
        String message = pending.occurrences <= 1 ? pending.message
                : pending.message + " ×" + pending.occurrences;
        displayReminder(client, message);
        nextNotificationAt = monotonicNow + NOTIFICATION_DRAIN_GAP_MS;
    }

    private void markConfigDirty(long monotonicNow) {
        if (!configDirty) {
            configDirty = true;
            nextSaveRetryAt = monotonicNow;
        }
    }

    private void attemptDirtySave(long monotonicNow) {
        if (!configDirty || monotonicNow < nextSaveRetryAt) return;
        if (CONFIG.save()) {
            configDirty = false;
            saveFailureCount = 0;
            nextSaveRetryAt = Long.MIN_VALUE;
            runtimeConfigError = null;
            return;
        }
        runtimeConfigError = CONFIG.getLastSaveError();
        saveFailureCount = Math.min(6, saveFailureCount + 1);
        long retryDelay = Math.min(SAVE_RETRY_MAX_MS, 1_000L << (saveFailureCount - 1));
        nextSaveRetryAt = monotonicNow + retryDelay;
    }

    private void onClientStopping(Minecraft client) {
        if (configDirty && CONFIG != null && CONFIG.save()) {
            configDirty = false;
            runtimeConfigError = null;
        }
        CustomSoundPlayer.shutdown();
    }

    public static String getRuntimeConfigError() {
        return runtimeConfigError;
    }

    private static void applyAfterTrigger(Reminder reminder, List<Reminder> deleteAfterTrigger) {
        Reminder.ScheduleType type = reminder.scheduleType == null
                ? Reminder.ScheduleType.DAILY : reminder.scheduleType;
        if (type == Reminder.ScheduleType.WEEKLY) return;
        Reminder.AfterTriggerAction action = reminder.afterTriggerAction == null
                ? Reminder.AfterTriggerAction.KEEP : reminder.afterTriggerAction;
        switch (action) {
            case KEEP -> { }
            case DISABLE -> reminder.enabled = false;
            case DELETE -> {
                reminder.enabled = false;
                if (!deleteAfterTrigger.contains(reminder)) deleteAfterTrigger.add(reminder);
            }
        }
    }

    public static void resetIntervalTimer(Reminder reminder) {
        if (reminder != null) reminder.resetIntervalTimer();
    }

    public static void invalidateTriggerState(Reminder reminder) {
        if (reminder != null) TRIGGER_STATES.remove(reminder);
    }

    public static String weekDaysSummary(Reminder reminder) {
        if (reminder == null || reminder.scheduleType != Reminder.ScheduleType.WEEKLY) return "";
        String[] labels = {
                ChronicleI18n.tr("day.mon.short"), ChronicleI18n.tr("day.tue.short"),
                ChronicleI18n.tr("day.wed.short"), ChronicleI18n.tr("day.thu.short"),
                ChronicleI18n.tr("day.fri.short"), ChronicleI18n.tr("day.sat.short"),
                ChronicleI18n.tr("day.sun.short")
        };
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if (reminder.hasWeeklyDay(i)) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(labels[i]);
            }
        }
        return builder.length() == 0 ? ChronicleI18n.tr("summary.no_days") : builder.toString();
    }

    public static String scheduleSummary(Reminder reminder, boolean use24HourFormat) {
        if (reminder == null) return ChronicleI18n.tr("schedule.daily");
        Reminder.ScheduleType type = reminder.scheduleType == null
                ? Reminder.ScheduleType.DAILY : reminder.scheduleType;
        String schedule = switch (type) {
            case DAILY -> ChronicleI18n.tr("summary.daily", displayTime(reminder, use24HourFormat));
            case WEEKLY -> ChronicleI18n.tr("summary.weekly", weekDaysSummary(reminder),
                    displayTime(reminder, use24HourFormat));
            case INTERVAL -> ChronicleI18n.tr("summary.every", formatInterval(reminder.intervalMinutes));
            case TRIGGER -> triggerSummary(reminder.trigger);
        };
        if (type == Reminder.ScheduleType.WEEKLY) {
            return schedule;
        }
        Reminder.AfterTriggerAction after = reminder.afterTriggerAction == null
                ? Reminder.AfterTriggerAction.KEEP : reminder.afterTriggerAction;
        return switch (after) {
            case KEEP -> schedule;
            case DISABLE -> schedule + " • " + ChronicleI18n.tr("summary.once_disable");
            case DELETE -> schedule + " • " + ChronicleI18n.tr("summary.once_delete");
        };
    }

    public static String triggerSummary(ReminderTrigger trigger) {
        if (trigger == null || trigger.type == null) {
            return ChronicleI18n.tr("summary.trigger.health", 25);
        }
        return switch (trigger.type) {
            case HEALTH_BELOW -> ChronicleI18n.tr("summary.trigger.health", trigger.threshold);
            case HUNGER_BELOW -> ChronicleI18n.tr("summary.trigger.hunger", trigger.threshold);
            case AIR_BELOW -> ChronicleI18n.tr("summary.trigger.air", trigger.threshold);
            case INVENTORY_FULL -> ChronicleI18n.tr("summary.trigger.inventory_full");
            case DURABILITY_BELOW -> ChronicleI18n.tr("summary.trigger.durability", trigger.threshold);
            case ENTER_DIMENSION -> ChronicleI18n.tr("summary.trigger.dimension", trigger.normalizedTarget());
            case ENTER_AREA -> ChronicleI18n.tr("summary.trigger.area", trigger.x, trigger.z, trigger.radius);
        };
    }

    public static String displayTime(Reminder reminder, boolean use24HourFormat) {
        if (reminder == null) return "00:00";
        if (use24HourFormat) return reminder.timeString();
        int hour = reminder.hour % 12;
        if (hour == 0) hour = 12;
        String period = reminder.hour >= 12 ? "PM" : "AM";
        return String.format("%d:%02d %s", hour, reminder.minute, period);
    }

    public static String formatInterval(int minutes) {
        minutes = Math.max(1, minutes);
        if (minutes % 1440 == 0) {
            int days = minutes / 1440;
            return ChronicleI18n.tr(days == 1 ? "duration.day" : "duration.days", days);
        }
        if (minutes % 60 == 0) {
            int hours = minutes / 60;
            return ChronicleI18n.tr(hours == 1 ? "duration.hour" : "duration.hours", hours);
        }
        return ChronicleI18n.tr(minutes == 1 ? "duration.minute" : "duration.minutes", minutes);
    }

    public static void showReminder(Minecraft client, String message) {
        displayReminder(client, message);
    }

    private static void displayReminder(Minecraft client, String message) {
        if (client == null || CONFIG == null) return;
        String resolvedMessage = ChroniclePlaceholders.resolve(message);
        String resolvedTitle = ChroniclePlaceholders.resolve(CONFIG.toastTitle);
        displayResolvedReminder(client, resolvedMessage, resolvedTitle, message);
    }

    private static void displayResolvedReminder(Minecraft client, String resolvedMessage,
                                                String resolvedTitle, String sourceMessage) {
        int snoozeMinutes = CONFIG.toastSnoozeMinutes;
        CustomReminderToast.SnoozeAction action = CONFIG.toastActionsEnabled
                ? () -> snoozeReminder(sourceMessage, snoozeMinutes)
                : null;
        client.gui.toastManager().addToast(new CustomReminderToast(
                CONFIG, resolvedMessage, resolvedTitle, action));
        CustomSoundPlayer.playConfigured(client, CONFIG);
    }

    public static boolean snoozeReminder(String message) {
        int minutes = CONFIG == null
                ? ReminderConfig.DEFAULT_SNOOZE_MINUTES : CONFIG.toastSnoozeMinutes;
        return snoozeReminder(message, minutes);
    }

    public static boolean snoozeReminder(String message, int minutes) {
        if (CONFIG == null) return false;
        if (CONFIG.reminders.size() >= ReminderConfig.MAX_REMINDERS) {
            runtimeConfigError = ChronicleI18n.tr("error.reminder_limit", ReminderConfig.MAX_REMINDERS);
            return false;
        }
        Reminder snoozed = new Reminder(0, 0,
                message == null || message.isBlank() ? ChronicleI18n.tr("default.reminder") : message.trim(),
                true);
        snoozed.scheduleType = Reminder.ScheduleType.INTERVAL;
        snoozed.intervalMinutes = Math.max(1, Math.min(24 * 60, minutes));
        snoozed.afterTriggerAction = Reminder.AfterTriggerAction.DELETE;
        snoozed.resetIntervalTimer();
        CONFIG.reminders.add(snoozed);
        CONFIG.ensureValid();
        if (!CONFIG.save()) {
            CONFIG.reminders.remove(snoozed);
            runtimeConfigError = CONFIG.getLastSaveError();
            return false;
        }
        CONFIG_REVISION++;
        runtimeConfigError = null;
        return true;
    }

    /** User-invoked test: bypasses scheduler anti-spam and uses the saved sound settings. */
    public static void showTestReminder(Minecraft client) {
        if (client == null || CONFIG == null) return;
        long now = Util.getMillis();
        if (lastTestToastAt != Long.MIN_VALUE && now - lastTestToastAt < TEST_TOAST_DEBOUNCE_MS) {
            return;
        }
        lastTestToastAt = now;
        String previewMessage = ChronicleI18n.tr("toast.preview.reminder");
        String resolvedMessage = ChroniclePlaceholders.resolve(previewMessage);
        String resolvedTitle = ChroniclePlaceholders.resolve(CONFIG.toastTitle);
        int snoozeMinutes = CONFIG.toastSnoozeMinutes;
        CustomReminderToast.SnoozeAction action = CONFIG.toastActionsEnabled
                ? () -> snoozeReminder(previewMessage, snoozeMinutes)
                : null;
        client.gui.toastManager().addToast(new CustomReminderToast(
                CONFIG, resolvedMessage, resolvedTitle, action));
        CustomSoundPlayer.playConfigured(client, CONFIG);
    }
}
