package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;

public final class UiAnimation {
    private UiAnimation() {}

    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /** Smooth ease-out used for screen entry and subtle widget motion. */
    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    /** Quintic smooth-step: quiet at both ends, good for a minimal UI. */
    public static float smooth(float t) {
        t = clamp01(t);
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }

    public static float pressProgress(long pressedAt, long now, long durationMs) {
        if (pressedAt < 0L || (ChronicleClient.CONFIG != null
                && !ChronicleClient.CONFIG.animationsEnabled)) return 0.0f;
        return 1.0f - smooth((now - pressedAt) / (float) Math.max(1L, durationMs));
    }

    public static int pressInset(float progress) {
        return Math.round(2.0f * clamp01(progress));
    }
}
