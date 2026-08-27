package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;

final class ChronicleScreenTransition {
    private static final long OPEN_DURATION_MS = 220L;
    private static final long CLOSE_DURATION_MS = 170L;

    private long openedAt = Util.getMillis();
    private long closingAt = -1L;
    private Screen target;

    void added() {
        openedAt = Util.getMillis();
        closingAt = -1L;
        target = null;
    }

    boolean isClosing() {
        return closingAt >= 0L;
    }

    void start(Minecraft minecraft, Screen next) {
        if (isClosing()) return;
        if (!animationsEnabled()) {
            minecraft.gui.setScreen(next);
            return;
        }
        target = next;
        closingAt = Util.getMillis();
    }

    boolean tick(Minecraft minecraft) {
        if (!isClosing()) return false;
        if (!animationsEnabled() || Util.getMillis() - closingAt >= CLOSE_DURATION_MS) {
            Screen next = target;
            closingAt = -1L;
            target = null;
            minecraft.gui.setScreen(next);
            return true;
        }
        return false;
    }

    void begin(GuiGraphicsExtractor graphics, int width, int height) {
        graphics.fill(0, 0, width, height, UiFrame.BACKDROP);
    }

    void end(GuiGraphicsExtractor graphics, int width, int height) {
        float progress = progress();
        if (progress < 1.0f) {
            int alpha = Math.max(0, Math.min(105, Math.round((1.0f - progress) * 105.0f)));
            graphics.fill(0, 0, width, height, (alpha << 24) | 0x00090C11);
        }
    }

    private float progress() {
        if (!animationsEnabled()) return 1.0f;
        long now = Util.getMillis();
        if (isClosing()) {
            return 1.0f - UiAnimation.smooth((now - closingAt) / (float) CLOSE_DURATION_MS);
        }
        return UiAnimation.easeOutCubic((now - openedAt) / (float) OPEN_DURATION_MS);
    }

    private static boolean animationsEnabled() {
        return ChronicleClient.CONFIG == null || ChronicleClient.CONFIG.animationsEnabled;
    }
}
