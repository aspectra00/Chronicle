package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.WeakHashMap;

/** Shared window chrome so every Chronicle screen uses the same frame, border and accent bar. */
public final class UiFrame {
    public static final int BACKDROP = 0xFF090C11;
    public static final int PANEL = 0xFF10151C;
    public static final int FRAME = 0xFF02050A;
    public static final int ACCENT = 0xFF8FB3E8;
    public static final int INNER_LINE = 0xFF252E3A;
    public static final int CONTROL_HOVER = 0xFF3A4655;
    public static final int CONTROL_HOVER_FILL = 0xFF151B23;
    public static final int ACCENT_SOFT = 0xFF202B3D;
    public static final int TEXT = 0xFFE7ECF2;
    public static final int MUTED = 0xFF8995A4;
    public static final int SUBTLE = 0xFF5F6B79;
    public static final int FRAME_SIZE = 2;
    public static final int ACCENT_SIZE = 2;
    private static final Map<Button, ButtonMotion> BUTTON_MOTION = new WeakHashMap<>();

    private UiFrame() {}

    public static void drawWindow(GuiGraphicsExtractor g, int left, int top, int width, int bottom) {
        int right = left + Math.max(1, width);
        int safeBottom = Math.max(top + FRAME_SIZE + ACCENT_SIZE + 1, bottom);
        // Full, shared black frame.
        g.fill(left, top, right, safeBottom, FRAME);
        // Interior surface.
        g.fill(left + FRAME_SIZE, top + FRAME_SIZE, Math.max(left + FRAME_SIZE, right - FRAME_SIZE),
                Math.max(top + FRAME_SIZE, safeBottom - FRAME_SIZE), PANEL);
        // Identical blue header strip on every screen.
        g.fill(left + FRAME_SIZE, top + FRAME_SIZE, Math.max(left + FRAME_SIZE, right - FRAME_SIZE),
                Math.min(safeBottom, top + FRAME_SIZE + ACCENT_SIZE), ACCENT);
        // Small dark separator directly beneath the accent.
        int separatorY = top + FRAME_SIZE + ACCENT_SIZE;
        if (separatorY < safeBottom - FRAME_SIZE) {
            g.fill(left + FRAME_SIZE, separatorY, Math.max(left + FRAME_SIZE, right - FRAME_SIZE),
                    separatorY + 1, INNER_LINE);
        }
    }

    public static void drawControlBorder(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        g.fill(x, y, x + w, y + h, FRAME);
        if (w > 2 && h > 2) {
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
        }
    }

    /** Shared button surface, selection state and font baseline for all screens. */
    public static void drawButton(GuiGraphicsExtractor g, Font font, Button button,
                                  int accent, boolean emphasized, int mouseX, int mouseY) {
        if (button == null || !button.visible) return;
        int x = button.getX();
        int y = button.getY();
        int w = button.getWidth();
        int h = button.getHeight();
        boolean hovered = button.active
                && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        boolean focused = button.active && button.isFocused() && !hovered
                && Minecraft.getInstance().getLastInputType().isKeyboard();

        ButtonMotion motion = BUTTON_MOTION.computeIfAbsent(button,
                ignored -> new ButtonMotion(hovered ? 1.0f : 0.0f,
                        emphasized ? 1.0f : 0.0f, Util.getMillis()));
        motion.update(hovered, emphasized, animationsEnabled());
        float attention = Math.max(motion.hover, motion.emphasis);
        int outer = lerpColor(INNER_LINE, CONTROL_HOVER, motion.hover);
        int hoverSurface = lerpColor(PANEL, CONTROL_HOVER_FILL, motion.hover);
        int inner = lerpColor(hoverSurface, ACCENT_SOFT, motion.emphasis);
        int line = lerpColor(INNER_LINE, accent, attention);
        int textColor = !button.active ? MUTED : lerpColor(MUTED, TEXT, attention);

        g.fill(x, y, x + w, y + h, outer);
        if (w > 2 && h > 2) {
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, inner);
        }
        if (w > 4 && h > 3) {
            g.fill(x + 2, y + 2, x + w - 2, y + 3, line);
        }
        // Keyboard focus remains visible for accessibility, but it is deliberately
        // a quiet outline instead of the same filled state used for mouse hover.
        if (focused && w > 6 && h > 6) {
            g.fill(x + 2, y + 2, x + w - 2, y + 3, ACCENT);
            g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, ACCENT);
            g.fill(x + 2, y + 3, x + 3, y + h - 3, ACCENT);
            g.fill(x + w - 3, y + 3, x + w - 2, y + h - 3, ACCENT);
        }

        if (w < 8 || h < font.lineHeight + 2) return;
        Component label = Component.literal(trimToWidth(font, button.getMessage().getString(), w - 8));
        int textX = x + Math.max(4, (w - font.width(label)) / 2);
        int textY = UiMetrics.centeredTextY(y, h, font.lineHeight);
        g.text(font, label, textX, textY, textColor, false);
    }

    /** Minimal scroll indicator shared by all long Chronicle views. */
    public static void drawScrollBar(GuiGraphicsExtractor g, int x, int top, int bottom,
                                     float progress, float visibleFraction) {
        int safeBottom = Math.max(top + 1, bottom);
        int trackHeight = safeBottom - top;
        if (trackHeight < 8 || visibleFraction >= 0.999f) return;
        float p = Math.max(0.0f, Math.min(1.0f, progress));
        float fraction = Math.max(0.05f, Math.min(1.0f, visibleFraction));
        int thumbHeight = Math.max(12, Math.round(trackHeight * fraction));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int thumbY = top + Math.round((trackHeight - thumbHeight) * p);
        g.fill(x, top, x + 2, safeBottom, 0xFF1A212B);
        g.fill(x - 1, thumbY, x + 3, thumbY + thumbHeight, 0xFF59697C);
    }

    public static String trimToWidth(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        if (font.width(ellipsis) > maxWidth) {
            int end = text.length();
            while (end > 0 && font.width(text.substring(0, end)) > maxWidth) {
                end = text.offsetByCodePoints(end, -1);
            }
            return text.substring(0, end);
        }
        int target = Math.max(0, maxWidth - font.width(ellipsis));
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > target) {
            end = text.offsetByCodePoints(end, -1);
        }
        return text.substring(0, end) + ellipsis;
    }

    private static boolean animationsEnabled() {
        return ChronicleClient.CONFIG == null || ChronicleClient.CONFIG.animationsEnabled;
    }

    private static int lerpColor(int from, int to, float amount) {
        float t = UiAnimation.clamp01(amount);
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = Math.round(((from >>> 16) & 255) + (((to >>> 16) & 255) - ((from >>> 16) & 255)) * t);
        int g = Math.round(((from >>> 8) & 255) + (((to >>> 8) & 255) - ((from >>> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static final class ButtonMotion {
        private float hover;
        private float emphasis;
        private long updatedAt;

        private ButtonMotion(float hover, float emphasis, long updatedAt) {
            this.hover = hover;
            this.emphasis = emphasis;
            this.updatedAt = updatedAt;
        }

        private void update(boolean hovered, boolean emphasized, boolean animated) {
            long now = Util.getMillis();
            if (!animated) {
                hover = hovered ? 1.0f : 0.0f;
                emphasis = emphasized ? 1.0f : 0.0f;
                updatedAt = now;
                return;
            }
            long elapsed = Math.max(0L, Math.min(100L, now - updatedAt));
            float blend = 1.0f - (float)Math.exp(-elapsed / 62.0f);
            hover += ((hovered ? 1.0f : 0.0f) - hover) * blend;
            emphasis += ((emphasized ? 1.0f : 0.0f) - emphasis) * blend;
            updatedAt = now;
        }
    }
}
