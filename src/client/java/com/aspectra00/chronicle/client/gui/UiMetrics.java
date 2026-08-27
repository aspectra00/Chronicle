package com.aspectra00.chronicle.client.gui;

public final class UiMetrics {
    public static final int MAX_PANEL_WIDTH = 920;
    public static final int SCREEN_GUTTER = 12;
    public static final int PANEL_TOP = 18;
    public static final int COMPACT_PANEL_TOP = 8;
    public static final int HEADER_HEIGHT = 58;
    public static final int COMPACT_HEADER_HEIGHT = 50;
    public static final int HEADER_TITLE_OFFSET = 18;
    public static final int HEADER_SUBTITLE_OFFSET = 38;
    public static final int COMPACT_HEADER_TITLE_OFFSET = 10;
    public static final int COMPACT_HEADER_SUBTITLE_OFFSET = 30;

    public static final int GAP_XS = 4;
    public static final int GAP_SM = 8;
    public static final int GAP_MD = 12;
    public static final int GAP_LG = 20;

    public static final int CONTROL_HEIGHT = 28;
    public static final int COMPACT_CONTROL_HEIGHT = 24;
    public static final int PRIMARY_BUTTON_HEIGHT = 30;
    public static final int FOOTER_AREA_HEIGHT = 66;
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

    public static int contentInset(int panelWidth) {
        int width = Math.max(1, panelWidth);
        int preferred = Math.min(24, Math.max(8, width / 12));
        return Math.min(preferred, Math.max(0, (width - 1) / 2));
    }

    public static int centeredTextY(int y, int height, int lineHeight) {
        return y + Math.max(1, (height - lineHeight + 1) / 2);
    }

    public static int headerHeight(boolean compact) {
        return compact ? COMPACT_HEADER_HEIGHT : HEADER_HEIGHT;
    }

    public static int headerTitleY(int top, boolean compact) {
        return top + (compact ? COMPACT_HEADER_TITLE_OFFSET : HEADER_TITLE_OFFSET);
    }

    public static int headerSubtitleY(int top, boolean compact) {
        return top + (compact ? COMPACT_HEADER_SUBTITLE_OFFSET : HEADER_SUBTITLE_OFFSET);
    }

    public static int headerDividerY(int top, int headerHeight) {
        return top + Math.max(1, headerHeight - 2);
    }
}
