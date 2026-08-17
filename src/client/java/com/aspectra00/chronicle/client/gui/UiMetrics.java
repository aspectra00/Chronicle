package com.aspectra00.chronicle.client.gui;

/**
 * Shared geometry for every Chronicle screen.
 * Keeping these values in one place prevents small spacing differences from
 * accumulating between the list, editor and toast customizer.
 */
public final class UiMetrics {
    public static final int MAX_PANEL_WIDTH = 920;
    public static final int SCREEN_GUTTER = 12;
    public static final int PANEL_TOP = 18;
    public static final int COMPACT_PANEL_TOP = 8;
    public static final int HEADER_HEIGHT = 58;

    public static final int GAP_XS = 4;
    public static final int GAP_SM = 8;
    public static final int GAP_MD = 12;
    public static final int GAP_LG = 20;

    public static final int CONTROL_HEIGHT = 28;
    public static final int PRIMARY_BUTTON_HEIGHT = 30;
    /** Font line plus the standard eight-pixel caption-to-control gutter. */
    public static final int LABEL_OFFSET = 17;

    private UiMetrics() {}

    public static int panelWidth(int screenWidth) {
        return Math.max(1, Math.min(MAX_PANEL_WIDTH, screenWidth - SCREEN_GUTTER * 2));
    }

    public static int panelLeft(int screenWidth, int panelWidth) {
        return Math.max(0, (screenWidth - panelWidth) / 2);
    }

    public static int panelTop(int screenHeight) {
        return screenHeight < 360 ? COMPACT_PANEL_TOP : PANEL_TOP;
    }

    /** Responsive inset that settles at 24px on normal Minecraft GUI sizes. */
    public static int contentInset(int panelWidth) {
        int safeWidth = Math.max(1, panelWidth);
        int preferred = Math.min(24, Math.max(8, safeWidth / 12));
        // A large GUI scale can leave fewer than sixteen logical pixels. Never
        // let the inset consume or cross the complete panel in that case.
        return Math.min(preferred, Math.max(0, (safeWidth - 1) / 2));
    }

    public static int centeredTextY(int y, int height, int lineHeight) {
        return y + Math.max(1, (height - lineHeight + 1) / 2);
    }
}
