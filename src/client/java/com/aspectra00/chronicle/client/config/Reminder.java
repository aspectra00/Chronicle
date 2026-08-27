package com.aspectra00.chronicle.client.config;

import java.util.Arrays;

public class Reminder {
    public enum ScheduleType {
        DAILY,
        WEEKLY,
        INTERVAL,
        TRIGGER
    }

    public enum AfterTriggerAction {
        KEEP,
        DISABLE,
        DELETE
    }

    public int hour;
    public int minute;
    public String message;
    public boolean enabled;

    public ScheduleType scheduleType = ScheduleType.DAILY;

    public AfterTriggerAction afterTriggerAction = AfterTriggerAction.KEEP;

    public int intervalMinutes = 60;

    public boolean[] weeklyDays = new boolean[]{true, true, true, true, true, true, true};

    public ReminderTrigger trigger = new ReminderTrigger();

    public long lastTriggeredEpochMinute = 0L;

    public long nextTriggerEpochMillis = 0L;

    public long lastTriggeredWallClockMinute = Long.MIN_VALUE;

    public Reminder() {
        this(0, 0, "Reminder", true);
    }

    public Reminder(int hour, int minute, String message, boolean enabled) {
        this.hour = hour;
        this.minute = minute;
        this.message = message;
        this.enabled = enabled;
        this.scheduleType = ScheduleType.DAILY;
        this.intervalMinutes = 60;
        this.weeklyDays = new boolean[]{true, true, true, true, true, true, true};
    }

    public String timeString() {
        return String.format("%02d:%02d", hour, minute);
    }

    public Reminder copy() {
        Reminder copy = new Reminder(this.hour, this.minute, this.message, this.enabled);
        copy.scheduleType = this.scheduleType;
        copy.afterTriggerAction = this.afterTriggerAction;
        copy.intervalMinutes = this.intervalMinutes;
        copy.weeklyDays = this.weeklyDays == null ? null : Arrays.copyOf(this.weeklyDays, 7);
        copy.trigger = this.trigger == null ? new ReminderTrigger() : this.trigger.copy();
        copy.lastTriggeredEpochMinute = this.lastTriggeredEpochMinute;
        copy.nextTriggerEpochMillis = this.nextTriggerEpochMillis;
        copy.lastTriggeredWallClockMinute = this.lastTriggeredWallClockMinute;
        return copy;
    }

    public void resetIntervalTimer() {
        resetIntervalTimer(System.currentTimeMillis());
    }

    public void resetIntervalTimer(long nowMillis) {
        long intervalMillis = Math.max(1L, intervalMinutes) * 60_000L;
        this.nextTriggerEpochMillis = saturatedAdd(Math.max(0L, nowMillis), intervalMillis);
        this.lastTriggeredEpochMinute = 0L;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    public boolean hasWeeklyDay(int dayIndex) {
        return weeklyDays != null && dayIndex >= 0 && dayIndex < 7 && weeklyDays[dayIndex];
    }

    public boolean hasAnyWeeklyDay() {
        if (weeklyDays == null) {
            return false;
        }
        for (boolean selected : weeklyDays) {
            if (selected) {
                return true;
            }
        }
        return false;
    }

    public void ensureWeeklyDays() {
        if (weeklyDays == null) {
            weeklyDays = new boolean[]{true, true, true, true, true, true, true};
            return;
        }
        if (weeklyDays.length != 7) {
            weeklyDays = Arrays.copyOf(weeklyDays, 7);
        }
    }
}
