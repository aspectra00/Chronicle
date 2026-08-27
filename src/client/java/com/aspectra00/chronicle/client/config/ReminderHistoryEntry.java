package com.aspectra00.chronicle.client.config;

public final class ReminderHistoryEntry {
    public enum Status {
        MISSED,
        COMPLETED,
        SNOOZED
    }

    public String message;
    public Status status;
    public long occurredAtEpochMillis;
    public int snoozeMinutes;

    public ReminderHistoryEntry() {
        this("", Status.MISSED, System.currentTimeMillis(), 0);
    }

    public ReminderHistoryEntry(String message, Status status, long occurredAtEpochMillis,
                                int snoozeMinutes) {
        this.message = message;
        this.status = status;
        this.occurredAtEpochMillis = occurredAtEpochMillis;
        this.snoozeMinutes = snoozeMinutes;
    }
}
