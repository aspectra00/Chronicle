package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.CustomToastBackground;
import com.aspectra00.chronicle.client.config.ReminderConfig;
import com.aspectra00.chronicle.client.config.ReminderHistoryEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;


public final class CustomReminderToast implements Toast {
    private static final long DISPLAY_TIME_MS = 10_000L;
    private static final int ACTION_BUTTON_HEIGHT = 15;
    private static final int MODERN_SHADOW_SIZE = 2;
    private static final int MODERN_HORIZONTAL_INSET = 9;
    private static final int MODERN_CONTENT_TOP = 6;
    private static final int MODERN_CONTENT_BOTTOM = 5;
    private static final int MODERN_TEXT_GAP = 4;
    private static final int MODERN_MESSAGE_OPTICAL_OFFSET = 1;
    private static final int MODERN_ICON_SIZE = 28;
    private static final int MODERN_ICON_GAP = 9;
    private static final int MODERN_ACTION_GAP = 5;
    private static final Identifier VANILLA_TOAST_BACKGROUND =
            Identifier.withDefaultNamespace("toast/system");
    private static final int VANILLA_MIN_WIDTH = 160;
    private static final int VANILLA_MAX_LINE_WIDTH = 200;
    private static final int VANILLA_TEXT_X = 18;
    private static final int VANILLA_TEXT_RIGHT_MARGIN = 12;
    private static final int VANILLA_LINE_SPACING = 12;

    private final String message;
    private final String title;
    private final String icon;
    private final ReminderToastTheme theme;
    private final float titleScale;
    private final float messageScale;
    private final float iconScale;
    private final String frameStyle;
    private final String backgroundImagePath;
    private final boolean animationsEnabled;
    private final SnoozeAction snoozeAction;
    private final HistoryAction historyAction;
    private final int snoozeMinutes;
    private final Object token = new Object();

    private Visibility visibility = Visibility.HIDE;
    private long shownAt;
    private long elapsedDisplayMs;
    private long displayDurationMs = DISPLAY_TIME_MS;
    private long lastUpdateTime;
    private long actionFailedUntil;
    private float renderedX = Float.NaN;
    private float renderedY = Float.NaN;
    private int renderedWidth;
    private int renderedHeight;
    private int renderedGuiWidth;
    private int renderedGuiHeight;
    private long lastRenderedAt;
    private boolean dismissed;
    private boolean pointerOverToast;
    private boolean outcomeRecorded;

    @FunctionalInterface
    public interface SnoozeAction {
        boolean snooze();
    }

    @FunctionalInterface
    public interface HistoryAction {
        void record(ReminderHistoryEntry.Status status, int snoozeMinutes);
    }

    private record ActionBounds(int snoozeX, int snoozeWidth, int dismissX, int dismissWidth,
                                int y, int height) {
    }

    public CustomReminderToast(ReminderConfig config, String message) {
        this(config, message, config == null ? "CHRONICLE" : config.toastTitle);
    }

    public CustomReminderToast(ReminderConfig config, String message, String resolvedTitle) {
        this(config, message, resolvedTitle, null);
    }

    public CustomReminderToast(ReminderConfig config, String message, String resolvedTitle,
                               SnoozeAction snoozeAction) {
        this(config, message, resolvedTitle, snoozeAction, null);
    }

    public CustomReminderToast(ReminderConfig config, String message, String resolvedTitle,
                               SnoozeAction snoozeAction, HistoryAction historyAction) {
        this(message,
                resolvedTitle,
                config == null ? "!" : config.toastIcon,
                config == null ? ReminderToastTheme.defaultMinimal() : ReminderToastTheme.fromConfig(config),
                config == null ? 1.00f : config.toastTitleScale,
                config == null ? 1.00f : config.toastMessageScale,
                config == null ? 2.00f : config.toastIconScale,
                config == null ? "MODERN" : config.toastFrameStyle,
                config == null || config.animationsEnabled,
                snoozeAction,
                config == null ? ReminderConfig.DEFAULT_SNOOZE_MINUTES : config.toastSnoozeMinutes,
                historyAction,
                config == null ? "" : config.toastBackgroundImagePath);
    }

    public CustomReminderToast(String message, String title, String icon, ReminderToastTheme theme) {
        this(message, title, icon, theme, 1.00f, 1.00f, 2.00f);
    }

    public CustomReminderToast(String message, String title, String icon, ReminderToastTheme theme,
                               float titleScale, float messageScale, float iconScale) {
        this(message, title, icon, theme, titleScale, messageScale, iconScale, "MODERN", true);
    }

    public CustomReminderToast(String message, String title, String icon, ReminderToastTheme theme,
                               float titleScale, float messageScale, float iconScale,
                               String frameStyle, boolean animationsEnabled) {
        this(message, title, icon, theme, titleScale, messageScale, iconScale,
                frameStyle, animationsEnabled, null);
    }

    public CustomReminderToast(String message, String title, String icon, ReminderToastTheme theme,
                               float titleScale, float messageScale, float iconScale,
                               String frameStyle, boolean animationsEnabled,
                               SnoozeAction snoozeAction) {
        this(message, title, icon, theme, titleScale, messageScale, iconScale,
                frameStyle, animationsEnabled, snoozeAction,
                ReminderConfig.DEFAULT_SNOOZE_MINUTES);
    }

    public CustomReminderToast(String message, String title, String icon, ReminderToastTheme theme,
                               float titleScale, float messageScale, float iconScale,
                               String frameStyle, boolean animationsEnabled,
                               SnoozeAction snoozeAction, int snoozeMinutes) {
        this(message, title, icon, theme, titleScale, messageScale, iconScale,
                frameStyle, animationsEnabled, snoozeAction, snoozeMinutes, null);
    }

    public CustomReminderToast(String message, String title, String icon, ReminderToastTheme theme,
                               float titleScale, float messageScale, float iconScale,
                               String frameStyle, boolean animationsEnabled,
                               SnoozeAction snoozeAction, int snoozeMinutes,
                               HistoryAction historyAction) {
        this(message, title, icon, theme, titleScale, messageScale, iconScale,
                frameStyle, animationsEnabled, snoozeAction, snoozeMinutes, historyAction, "");
    }

    public CustomReminderToast(String message, String title, String icon, ReminderToastTheme theme,
                               float titleScale, float messageScale, float iconScale,
                               String frameStyle, boolean animationsEnabled,
                               SnoozeAction snoozeAction, int snoozeMinutes,
                               HistoryAction historyAction, String backgroundImagePath) {
        this.message = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        this.title = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        this.icon = icon == null || icon.isBlank() ? "!" : icon.trim();
        this.theme = theme == null ? ReminderToastTheme.defaultMinimal() : theme;
        this.titleScale = crispScale(titleScale, 1.00f);
        this.messageScale = crispScale(messageScale, 1.00f);
        this.iconScale = crispScale(iconScale, 2.00f);
        this.frameStyle = "VANILLA".equalsIgnoreCase(frameStyle) ? "VANILLA" : "MODERN";
        this.backgroundImagePath = backgroundImagePath == null ? "" : backgroundImagePath.trim();
        this.animationsEnabled = animationsEnabled;
        this.snoozeAction = snoozeAction;
        this.snoozeMinutes = normalizeSnoozeMinutes(snoozeMinutes);
        this.historyAction = historyAction;

        this.shownAt = 0L;
        if (showsActions()) ToastInteractionManager.register(this);
    }

    private static String fallbackMessage() {
        String localized = ChronicleI18n.tr("default.reminder");
        return localized == null || localized.isBlank() ? "Reminder" : localized;
    }

    private static float crispScale(float value, float fallback) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            value = fallback;
        }
        return Math.max(1.0f, Math.min(2.0f, Math.round(value)));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int normalizeSnoozeMinutes(int minutes) {
        return clampInt(minutes, 1, 24 * 60);
    }

    private boolean showsActions() {
        return snoozeAction != null && !"VANILLA".equals(frameStyle);
    }

    public static int responsiveWidth(int guiWidth) {
        int logical = Math.max(1, guiWidth);
        int available = Math.max(1, logical - 12);
        if (available < 120) {
            return available;
        }
        int calculated = Math.round(logical * 0.38f);
        return clampInt(calculated, Math.min(210, available), Math.min(480, available));
    }

    public static int responsiveHeight(int guiHeight) {
        int logical = Math.max(1, guiHeight);
        int available = Math.max(1, logical - 12);
        if (available < 56) {
            return available;
        }
        int calculated = Math.round(logical * 0.095f);
        return clampInt(calculated, Math.min(58, available), Math.min(60, available));
    }

    private static int vanillaWidthWithin(Font font, String title, String message, int availableWidth) {
        int available = Math.max(1, availableWidth);
        if (available < VANILLA_MIN_WIDTH) {
            return available;
        }
        String toastTitle = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        String toastMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        int longest = Math.max(font.width(toastTitle), font.width(toastMessage));
        int textWidth = Math.min(VANILLA_MAX_LINE_WIDTH, longest);
        return Math.min(available, Math.max(VANILLA_MIN_WIDTH,
                textWidth + VANILLA_TEXT_X + VANILLA_TEXT_RIGHT_MARGIN));
    }

    private static int vanillaHeightWithin(Font font, String message, int width, int availableHeight,
                                           boolean showActions) {
        int available = Math.max(1, availableHeight);
        String toastMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        int textWidth = Math.max(1, Math.min(VANILLA_MAX_LINE_WIDTH,
                width - VANILLA_TEXT_X - VANILLA_TEXT_RIGHT_MARGIN));
        int messageLines = Math.max(1, wrapText(font, toastMessage, textWidth, 4).size());
        int contentHeight = 20 + messageLines * VANILLA_LINE_SPACING;
        int actionBottom = 19 + (font.lineHeight - ACTION_BUTTON_HEIGHT) / 2
                + ACTION_BUTTON_HEIGHT + 8;
        return Math.min(available, Math.max(contentHeight, showActions ? actionBottom : 0));
    }


    @Override
    public int width() {
        Minecraft minecraft = Minecraft.getInstance();
        return layoutWidth(minecraft.font, title, message, frameStyle,
                minecraft.getWindow().getGuiScaledWidth(), showsActions());
    }

    @Override
    public int height() {
        Minecraft minecraft = Minecraft.getInstance();
        int currentWidth = layoutWidth(minecraft.font, title, message, frameStyle,
                minecraft.getWindow().getGuiScaledWidth(), showsActions());
        return layoutHeight(minecraft.font, message, frameStyle, currentWidth,
                minecraft.getWindow().getGuiScaledHeight(), showsActions());
    }

    @Override
    public int occcupiedSlotCount() {
        return 3;
    }

    @Override
    public float yPos(int firstSlotIndex) {
        renderedY = firstSlotIndex * (float) Toast.SLOT_HEIGHT;
        return renderedY;
    }

    @Override
    public float xPos(int guiWidth, float visiblePortion) {
        int currentWidth = width();
        renderedX = dismissed ? guiWidth : animationsEnabled
                ? guiWidth - currentWidth * visiblePortion
                : visibility == Visibility.SHOW ? guiWidth - currentWidth : guiWidth;
        return renderedX;
    }

    static int layoutWidth(Font font, String title, String message, String frameStyle, int guiWidth) {
        return layoutWidth(font, title, message, frameStyle, guiWidth, false);
    }

    static int layoutWidth(Font font, String title, String message, String frameStyle, int guiWidth,
                           boolean showActions) {
        int available = Math.max(1, guiWidth - 12);
        boolean vanilla = "VANILLA".equalsIgnoreCase(frameStyle);
        int naturalWidth = vanilla
                ? vanillaWidthWithin(font, title, message, available)
                : responsiveWidth(guiWidth);
        return showActions && !vanilla
                ? Math.min(available, Math.max(300, naturalWidth)) : naturalWidth;
    }

    static int layoutHeight(Font font, String message, String frameStyle, int width, int guiHeight) {
        return layoutHeight(font, message, frameStyle, width, guiHeight, true);
    }

    static int layoutHeight(Font font, String message, String frameStyle, int width,
                            int guiHeight, boolean showActions) {
        int available = Math.max(1, guiHeight - 12);
        return "VANILLA".equalsIgnoreCase(frameStyle)
                ? vanillaHeightWithin(font, message, width, available, false)
                : responsiveHeight(guiHeight);
    }

    @Override
    public Object getToken() {
        return token;
    }

    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager toastManager, long time) {
        long previousUpdateTime = lastUpdateTime;
        lastUpdateTime = time;
        if (dismissed) {
            visibility = Visibility.HIDE;
            return;
        }
        if (shownAt == 0L) {
            shownAt = time;
        }
        if (pointerOverToast && previousUpdateTime > 0L && time > previousUpdateTime) {
            shownAt += time - previousUpdateTime;
        }
        double multiplier = Math.max(0.1D, toastManager.getNotificationDisplayTimeMultiplier());
        long duration = Math.max(500L, (long) (DISPLAY_TIME_MS * multiplier));
        displayDurationMs = duration;
        elapsedDisplayMs = Math.max(0L, time - shownAt);
        visibility = elapsedDisplayMs < duration ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        int currentWidth = width();
        int currentHeight = height();
        renderedWidth = currentWidth;
        renderedHeight = currentHeight;
        Minecraft minecraft = Minecraft.getInstance();
        renderedGuiWidth = minecraft.getWindow().getGuiScaledWidth();
        renderedGuiHeight = minecraft.getWindow().getGuiScaledHeight();
        lastRenderedAt = Util.getMillis();
        double localMouseX = Double.NaN;
        double localMouseY = Double.NaN;
        if (minecraft.gui.screen() != null && !minecraft.mouseHandler.isMouseGrabbed()) {
            localMouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()) - renderedX;
            localMouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()) - renderedY;
        }
        boolean showActions = showsActions();
        pointerOverToast = showActions && localMouseX >= 0.0 && localMouseX < currentWidth
                && localMouseY >= 0.0 && localMouseY < currentHeight;
        renderCard(
                graphics, font, currentWidth, currentHeight,
                message, title, icon, theme,
                titleScale, messageScale, iconScale,
                frameStyle,
                true, elapsedDisplayMs, displayDurationMs,
                showActions, localMouseX, localMouseY,
                Util.getMillis() < actionFailedUntil,
                snoozeMinutes, backgroundImagePath
        );
    }

    @Override
    public void onFinishedRendering() {
        recordOutcome(ReminderHistoryEntry.Status.MISSED);
        ToastInteractionManager.unregister(this);
    }

    public static void renderPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int width,
            int height,
            String message,
            String title,
            String icon,
            ReminderToastTheme theme,
            float titleScale,
            float messageScale,
            float iconScale
    ) {
        renderPreview(graphics, font, width, height, message, title, icon, theme,
                titleScale, messageScale, iconScale, "MODERN");
    }

    public static void renderPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int width,
            int height,
            String message,
            String title,
            String icon,
            ReminderToastTheme theme,
            float titleScale,
            float messageScale,
            float iconScale,
            String frameStyle
    ) {
        renderPreview(graphics, font, width, height, message, title, icon, theme,
                titleScale, messageScale, iconScale, frameStyle, true);
    }

    public static void renderPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int width,
            int height,
            String message,
            String title,
            String icon,
            ReminderToastTheme theme,
            float titleScale,
            float messageScale,
            float iconScale,
            String frameStyle,
            boolean showActions
    ) {
        renderPreview(graphics, font, width, height, message, title, icon, theme,
                titleScale, messageScale, iconScale, frameStyle, showActions,
                ReminderConfig.DEFAULT_SNOOZE_MINUTES);
    }

    public static void renderPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int width,
            int height,
            String message,
            String title,
            String icon,
            ReminderToastTheme theme,
            float titleScale,
            float messageScale,
            float iconScale,
            String frameStyle,
            boolean showActions,
            int snoozeMinutes
    ) {
        renderPreview(graphics, font, width, height, message, title, icon, theme,
                titleScale, messageScale, iconScale, frameStyle, showActions,
                snoozeMinutes, "");
    }

    public static void renderPreview(
            GuiGraphicsExtractor graphics,
            Font font,
            int width,
            int height,
            String message,
            String title,
            String icon,
            ReminderToastTheme theme,
            float titleScale,
            float messageScale,
            float iconScale,
            String frameStyle,
            boolean showActions,
            int snoozeMinutes,
            String backgroundImagePath
    ) {
        boolean vanilla = "VANILLA".equalsIgnoreCase(frameStyle);
        showActions = showActions && !vanilla;
        int cardWidth = width;
        int cardHeight = height;
        if (vanilla) {
            cardWidth = vanillaWidthWithin(font, title, message, width);
            cardHeight = vanillaHeightWithin(font, message, cardWidth, height, showActions);
            graphics.pose().pushMatrix();
            graphics.pose().translate(Math.max(0, (width - cardWidth) / 2),
                    Math.max(0, (height - cardHeight) / 2));
        }
        renderCard(
                graphics, font, cardWidth, cardHeight,
                message, title, icon, theme,
                crispScale(titleScale, 1.00f),
                crispScale(messageScale, 1.00f),
                crispScale(iconScale, 2.00f),
                frameStyle,
                !vanilla, 0L, DISPLAY_TIME_MS,
                showActions, Double.NaN, Double.NaN, false,
                normalizeSnoozeMinutes(snoozeMinutes), backgroundImagePath
        );
        if (vanilla) {
            graphics.pose().popMatrix();
        }
    }

    private static void renderCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int width,
            int height,
            String message,
            String title,
            String icon,
            ReminderToastTheme theme,
            float titleScale,
            float messageScale,
            float iconScale,
            String frameStyle,
            boolean showProgress,
            long progressTimeMs,
            long displayDurationMs,
            boolean showActions,
            double mouseX,
            double mouseY,
            boolean actionFailed,
            int snoozeMinutes,
            String backgroundImagePath
    ) {
        int h = Math.max(1, height);
        int w = Math.max(1, width);
        ReminderToastTheme toastTheme = theme == null ? ReminderToastTheme.defaultMinimal() : theme;
        boolean vanilla = "VANILLA".equalsIgnoreCase(frameStyle);

        float progress = showProgress
                ? Math.max(0.0f, Math.min(1.0f,
                progressTimeMs / (float) Math.max(1L, displayDurationMs)))
                : 1.0f;

        if (vanilla) {
            renderVanillaCard(graphics, font, w, h, message, title, showActions, snoozeMinutes);
            renderActionButtons(graphics, font, w, h, toastTheme, true, showActions,
                    titleScale, messageScale, mouseX, mouseY, actionFailed, snoozeMinutes);
            return;
        }
        String toastIcon = icon == null || icon.isBlank()
                ? "!"
                : icon.codePoints().limit(2)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        String toastTitle = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        String toastMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();

        renderModernFrame(graphics, w, h, toastTheme, showProgress, progress,
                backgroundImagePath);

        if (w < 120 || h < 40) {
            String compactMessage = trimToWidth(font, toastMessage, Math.max(1, w - 12));
            graphics.text(font, Component.literal(compactMessage), 6,
                    Math.max(2, (h - font.lineHeight) / 2), toastTheme.message(), false);
            return;
        }

        int cardW = Math.max(1, w - MODERN_SHADOW_SIZE);
        int cardH = Math.max(1, h - MODERN_SHADOW_SIZE);
        ActionBounds actionBounds = actionBounds(font, w, h, showActions, false,
                titleScale, messageScale, snoozeMinutes);
        int contentBottom = Math.max(MODERN_CONTENT_TOP + 1, cardH - MODERN_CONTENT_BOTTOM);
        int contentHeight = Math.max(1, contentBottom - MODERN_CONTENT_TOP);
        int iconColumnEnd = MODERN_HORIZONTAL_INSET + MODERN_ICON_SIZE + MODERN_ICON_GAP;
        boolean showIcon = cardW >= 150 && contentHeight >= 38
                && (actionBounds == null || actionBounds.snoozeX() - iconColumnEnd >= 100);
        int textX = MODERN_HORIZONTAL_INSET;
        if (showIcon) {
            int iconBox = Math.max(22, Math.min(MODERN_ICON_SIZE, contentHeight - 10));
            int iconX = MODERN_HORIZONTAL_INSET;
            int iconY = MODERN_CONTENT_TOP + Math.max(0, (contentHeight - iconBox) / 2);
            drawModernIconTile(graphics, iconX, iconY, iconBox, toastTheme);

            int iconTextWidth = font.width(Component.literal(toastIcon));
            int fittedIconScale = Math.max(1, Math.min(2,
                    Math.min((iconBox - 10) / Math.max(1, iconTextWidth),
                            (iconBox - 10) / Math.max(1, font.lineHeight))));
            float iconDrawScale = Math.min(iconScale, fittedIconScale);
            int iconOpticalX = "!".equals(toastIcon) ? 1 : 0;
            int iconOpticalY = "!".equals(toastIcon) ? 1 : 0;
            int iconTextX = iconX + Math.max(1,
                    (iconBox - Math.round(iconTextWidth * iconDrawScale)) / 2) + iconOpticalX;
            int iconTextY = iconY + Math.max(1,
                    (iconBox - Math.round(font.lineHeight * iconDrawScale)) / 2) + iconOpticalY;
            drawScaled(graphics, font, toastIcon, iconTextX, iconTextY, iconDrawScale,
                    toastTheme.icon(), false);
            textX = iconX + iconBox + MODERN_ICON_GAP;
        }

        int rightPad = MODERN_HORIZONTAL_INSET;
        int contentRight = cardW;
        int available = Math.max(1, contentRight - rightPad - textX);

        int titlePixelHeight = Math.max(font.lineHeight, (int) Math.ceil(font.lineHeight * titleScale));
        int bodyPixelHeight = Math.max(font.lineHeight, (int) Math.ceil(font.lineHeight * messageScale));
        int titleMaxWidth = Math.max(1, (int) (available / titleScale));
        String clippedTitle = trimToWidth(font, toastTitle, titleMaxWidth);

        int maxLines = actionBounds == null
                && contentHeight >= titlePixelHeight + bodyPixelHeight * 2 + 12 ? 2 : 1;
        int bodyAvailable = actionBounds == null ? available
                : Math.max(1, Math.min(available, actionBounds.snoozeX() - textX - 7));
        int bodyMaxWidth = Math.max(1, (int) (bodyAvailable / messageScale));
        List<String> lines = wrapText(font, toastMessage, bodyMaxWidth, maxLines);

        int bodyLineHeight = bodyPixelHeight + 1;
        int totalBodyHeight = lines.size() * bodyLineHeight;
        int titleY = actionBounds == null
                ? MODERN_CONTENT_TOP + Math.max(0,
                (contentHeight - titlePixelHeight - MODERN_TEXT_GAP
                        - MODERN_MESSAGE_OPTICAL_OFFSET - totalBodyHeight) / 2)
                : modernTitleY(font, cardH, titleScale, messageScale);
        int bodyStartY = titleY + titlePixelHeight + MODERN_TEXT_GAP
                + MODERN_MESSAGE_OPTICAL_OFFSET;

        drawScaled(graphics, font, clippedTitle, textX, titleY, titleScale,
                toastTheme.title(), false);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = bodyStartY + i * bodyLineHeight;
            if (lineY + bodyPixelHeight > contentBottom) {
                break;
            }
            drawScaled(graphics, font, lines.get(i), textX, lineY,
                    messageScale, toastTheme.message(), false);
        }
        renderActionButtons(graphics, font, w, h, toastTheme, false, showActions,
                titleScale, messageScale, mouseX, mouseY, actionFailed, snoozeMinutes);
    }

    private static ActionBounds actionBounds(Font font, int width, int height, boolean showActions,
                                             boolean vanilla, float titleScale,
                                             float messageScale, int snoozeMinutes) {
        if (!showActions || width < 150 || height < 32) return null;
        int cardWidth = Math.max(1, width - (vanilla ? 0 : MODERN_SHADOW_SIZE));
        int cardHeight = Math.max(1, height - (vanilla ? 0 : MODERN_SHADOW_SIZE));
        int side = vanilla ? 10 : MODERN_HORIZONTAL_INSET;
        int gap = vanilla ? 6 : MODERN_ACTION_GAP;
        int available = Math.max(1, cardWidth - side * 2 - gap);
        String snooze = ChronicleI18n.tr("toast.action.snooze",
                ChronicleClient.formatInterval(normalizeSnoozeMinutes(snoozeMinutes)));
        String failed = ChronicleI18n.tr("toast.action.snooze_failed");
        String dismiss = ChronicleI18n.tr("toast.action.dismiss");
        int desiredSnooze = clampInt(Math.max(font.width(snooze), font.width(failed)) + 12, 66, 116);
        int desiredDismiss = clampInt(font.width(dismiss) + 12, 50, 88);
        int snoozeWidth = desiredSnooze;
        int dismissWidth = desiredDismiss;
        if (snoozeWidth + dismissWidth > available) {
            dismissWidth = Math.max(46, Math.min(desiredDismiss, available * 2 / 5));
            snoozeWidth = Math.max(1, available - dismissWidth);
        }
        int y = inlineActionY(font, cardHeight, vanilla, titleScale, messageScale);
        int dismissX = cardWidth - side - dismissWidth;
        int snoozeX = dismissX - gap - snoozeWidth;
        return new ActionBounds(snoozeX, snoozeWidth, dismissX, dismissWidth,
                y, ACTION_BUTTON_HEIGHT);
    }

    private static int modernTitleY(Font font, int cardHeight,
                                    float titleScale, float messageScale) {
        int contentBottom = Math.max(MODERN_CONTENT_TOP + 1,
                cardHeight - MODERN_CONTENT_BOTTOM);
        int contentHeight = Math.max(1, contentBottom - MODERN_CONTENT_TOP);
        int titleHeight = Math.max(font.lineHeight,
                (int) Math.ceil(font.lineHeight * titleScale));
        int messageHeight = Math.max(font.lineHeight,
                (int) Math.ceil(font.lineHeight * messageScale));
        int totalTextHeight = titleHeight + MODERN_TEXT_GAP
                + MODERN_MESSAGE_OPTICAL_OFFSET + messageHeight;
        int preferred = MODERN_CONTENT_TOP + Math.max(0,
                (contentHeight - totalTextHeight) / 2);
        return clampInt(preferred, MODERN_CONTENT_TOP,
                Math.max(MODERN_CONTENT_TOP, contentBottom - totalTextHeight));
    }

    private static int inlineActionY(Font font, int cardHeight, boolean vanilla,
                                     float titleScale, float messageScale) {
        int preferred;
        if (vanilla) {
            preferred = 19 + (font.lineHeight - ACTION_BUTTON_HEIGHT) / 2;
        } else {
            int titleHeight = Math.max(font.lineHeight,
                    (int) Math.ceil(font.lineHeight * titleScale));
            int messageHeight = Math.max(font.lineHeight,
                    (int) Math.ceil(font.lineHeight * messageScale));
            int titleY = modernTitleY(font, cardHeight, titleScale, messageScale);
            int messageY = titleY + titleHeight + MODERN_TEXT_GAP;
            preferred = messageY + (messageHeight - ACTION_BUTTON_HEIGHT) / 2;
        }
        int top = vanilla ? 2 : MODERN_CONTENT_TOP;
        int bottom = vanilla ? cardHeight - 3 : cardHeight - MODERN_CONTENT_BOTTOM;
        return clampInt(preferred, top, Math.max(top, bottom - ACTION_BUTTON_HEIGHT));
    }

    private static void renderActionButtons(GuiGraphicsExtractor graphics, Font font,
                                            int width, int height, ReminderToastTheme theme,
                                            boolean vanilla, boolean showActions,
                                            float titleScale, float messageScale,
                                            double mouseX, double mouseY,
                                            boolean actionFailed, int snoozeMinutes) {
        ActionBounds bounds = actionBounds(font, width, height, showActions, vanilla,
                titleScale, messageScale, snoozeMinutes);
        if (bounds == null) return;
        boolean snoozeHovered = inside(bounds.snoozeX(), bounds.y(), bounds.snoozeWidth(),
                bounds.height(), mouseX, mouseY);
        boolean dismissHovered = inside(bounds.dismissX(), bounds.y(), bounds.dismissWidth(),
                bounds.height(), mouseX, mouseY);
        String snooze = actionFailed
                ? ChronicleI18n.tr("toast.action.snooze_failed")
                : ChronicleI18n.tr("toast.action.snooze",
                ChronicleClient.formatInterval(normalizeSnoozeMinutes(snoozeMinutes)));
        String dismiss = ChronicleI18n.tr("toast.action.dismiss");
        if (!vanilla) {
            int dividerHeight = Math.min(font.lineHeight, Math.max(1, bounds.height() - 4));
            int dividerX = bounds.snoozeX() - 4;
            int dividerY = bounds.y() + (bounds.height() - dividerHeight) / 2;
            graphics.fill(dividerX, dividerY, dividerX + 1, dividerY + dividerHeight,
                    blendColor(theme.background(), theme.border(), 0.34f));
        }
        drawActionButton(graphics, font, bounds.snoozeX(), bounds.y(), bounds.snoozeWidth(),
                bounds.height(), snooze, theme, vanilla, true, snoozeHovered, actionFailed);
        drawActionButton(graphics, font, bounds.dismissX(), bounds.y(), bounds.dismissWidth(),
                bounds.height(), dismiss, theme, vanilla, false, dismissHovered, false);
    }

    private static void drawActionButton(GuiGraphicsExtractor graphics, Font font,
                                         int x, int y, int width, int height, String label,
                                         ReminderToastTheme theme, boolean vanilla,
                                         boolean primary, boolean hovered, boolean failed) {
        if (vanilla) {
            int border = failed ? 0xFFD69A9A
                    : hovered || primary ? 0xFFC8C8C8 : 0xFF696969;
            int surface = primary ? (hovered ? 0xFF474747 : 0xFF343434)
                    : (hovered ? 0xFF303030 : 0xFF202020);
            fillSteppedRect(graphics, x, y, width, height, border);
            if (width > 2 && height > 2) {
                fillSteppedRect(graphics, x + 1, y + 1, width - 2, height - 2, surface);
            }
        } else {
            int error = 0xFFD69A9A;
            int surface = failed
                    ? blendColor(theme.background(), error, 0.20f)
                    : primary
                    ? blendColor(theme.background(), theme.accent(), hovered ? 0.24f : 0.14f)
                    : blendColor(theme.background(), theme.border(), hovered ? 0.22f : 0.10f);
            int underline = failed ? error
                    : hovered ? theme.accent()
                    : primary ? blendColor(theme.border(), theme.accent(), 0.68f)
                    : blendColor(theme.background(), theme.border(), 0.48f);
            fillSteppedRect(graphics, x, y, width, height, surface);
            if (width > 8 && height > 4) {
                graphics.fill(x + 4, y + height - 2, x + width - 4, y + height - 1,
                        underline);
            }
        }
        String shown = trimToWidth(font, label, Math.max(1, width - 8));
        int textX = x + Math.max(4, (width - font.width(shown)) / 2);
        int textY = y + Math.max(1, (height - font.lineHeight) / 2);
        graphics.text(font, Component.literal(shown), textX, textY,
                failed ? 0xFFFFC2C2
                        : vanilla || hovered ? 0xFFFFFFFF
                        : primary ? theme.title() : theme.message(), false);
    }

    private static boolean inside(int x, int y, int width, int height,
                                  double mouseX, double mouseY) {
        return Double.isFinite(mouseX) && Double.isFinite(mouseY)
                && mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    boolean handleMouseClick(Minecraft minecraft, double mouseX, double mouseY) {
        if (minecraft == null || !showsActions() || dismissed
                || visibility != Visibility.SHOW
                || !Float.isFinite(renderedX) || !Float.isFinite(renderedY)
                || minecraft.gui.hud.isHidden()
                || renderedGuiWidth != minecraft.getWindow().getGuiScaledWidth()
                || renderedGuiHeight != minecraft.getWindow().getGuiScaledHeight()
                || Util.getMillis() - lastRenderedAt > 1_000L) {
            return false;
        }
        ActionBounds bounds = actionBounds(minecraft.font, renderedWidth, renderedHeight,
                true, "VANILLA".equals(frameStyle), titleScale, messageScale, snoozeMinutes);
        if (bounds == null) return false;
        double localX = mouseX - renderedX;
        double localY = mouseY - renderedY;
        if (inside(bounds.dismissX(), bounds.y(), bounds.dismissWidth(), bounds.height(),
                localX, localY)) {
            playButtonSound(minecraft);
            recordOutcome(ReminderHistoryEntry.Status.COMPLETED);
            dismissImmediately();
            return true;
        }
        if (!inside(bounds.snoozeX(), bounds.y(), bounds.snoozeWidth(), bounds.height(),
                localX, localY)) {
            return false;
        }
        playButtonSound(minecraft);
        boolean succeeded;
        try {
            succeeded = snoozeAction.snooze();
        } catch (RuntimeException ignored) {
            succeeded = false;
        }
        if (succeeded) {
            recordOutcome(ReminderHistoryEntry.Status.SNOOZED);
            dismissImmediately();
        } else {
            actionFailedUntil = Util.getMillis() + 3_000L;
            long maximumElapsed = Math.max(0L, displayDurationMs - 3_000L);
            if (elapsedDisplayMs > maximumElapsed) {
                elapsedDisplayMs = maximumElapsed;
                shownAt = Math.max(0L, lastUpdateTime - elapsedDisplayMs);
            }
        }
        return true;
    }

    private void recordOutcome(ReminderHistoryEntry.Status status) {
        if (outcomeRecorded || historyAction == null || status == null) return;
        outcomeRecorded = true;
        try {
            historyAction.record(status,
                    status == ReminderHistoryEntry.Status.SNOOZED ? snoozeMinutes : 0);
        } catch (RuntimeException ignored) {
        }
    }

    private void dismissImmediately() {
        dismissed = true;
        visibility = Visibility.HIDE;
        ToastInteractionManager.unregister(this);
    }

    private static void playButtonSound(Minecraft minecraft) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private static void renderModernFrame(GuiGraphicsExtractor graphics, int width, int height,
                                           ReminderToastTheme theme, boolean showProgress, float progress,
                                           String backgroundImagePath) {
        if (width < 6 || height < 6) {
            graphics.fill(0, 0, width, height, theme.background());
            return;
        }
        int cardW = Math.max(1, width - MODERN_SHADOW_SIZE);
        int cardH = Math.max(1, height - MODERN_SHADOW_SIZE);
        fillSteppedRect(graphics, MODERN_SHADOW_SIZE, MODERN_SHADOW_SIZE,
                cardW, cardH, 0x42000000);
        fillSteppedRect(graphics, 0, 0, cardW, cardH, theme.border());
        int surface = blendColor(theme.background(), theme.border(), 0.05f);
        fillSteppedRect(graphics, 1, 1, Math.max(1, cardW - 2), Math.max(1, cardH - 2), surface);
        int imageW = Math.max(0, cardW - 4);
        int imageH = Math.max(0, cardH - 4);
        if (CustomToastBackground.draw(graphics, backgroundImagePath, 2, 2, imageW, imageH)) {
            int tint = (0xA6 << 24) | (surface & 0x00FFFFFF);
            graphics.fill(2, 2, 2 + imageW, 2 + imageH, tint);
        }

        if (showProgress && cardW > 10) {
            float remaining = 1.0f - UiAnimation.clamp01(progress);
            int trackStart = 5;
            int trackEnd = cardW - 5;
            int progressEnd = trackStart + Math.round((trackEnd - trackStart) * remaining);
            graphics.fill(trackStart, cardH - 2, trackEnd, cardH - 1,
                    blendColor(theme.background(), theme.border(), 0.38f));
            if (progressEnd > trackStart) {
                graphics.fill(trackStart, cardH - 2, Math.min(trackEnd, progressEnd),
                        cardH - 1, theme.accent());
            }
        }
    }

    private static void fillSteppedRect(GuiGraphicsExtractor graphics, int x, int y,
                                        int width, int height, int color) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (w < 5 || h < 5) {
            graphics.fill(x, y, x + w, y + h, color);
            return;
        }
        graphics.fill(x + 2, y, x + w - 2, y + h, color);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        graphics.fill(x, y + 2, x + w, y + h - 2, color);
    }

    private static void renderVanillaCard(GuiGraphicsExtractor graphics, Font font, int width, int height,
                                          String message, String title, boolean showActions,
                                          int snoozeMinutes) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_TOAST_BACKGROUND, 0, 0, width, height);
        if (width < VANILLA_TEXT_X + 8 || height < font.lineHeight + 4) {
            return;
        }

        String toastTitle = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        String toastMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        int titleAvailable = Math.max(1, Math.min(VANILLA_MAX_LINE_WIDTH,
                width - VANILLA_TEXT_X - VANILLA_TEXT_RIGHT_MARGIN));
        ActionBounds bounds = actionBounds(font, width, height, showActions,
                true, 1.00f, 1.00f, snoozeMinutes);
        int messageAvailable = bounds == null ? titleAvailable
                : Math.max(1, Math.min(titleAvailable,
                        bounds.snoozeX() - VANILLA_TEXT_X - 6));
        String clippedTitle = trimToWidth(font, toastTitle, titleAvailable);
        int textBottom = height;
        int maxMessageLines = Math.max(1, (textBottom - 20) / VANILLA_LINE_SPACING);
        List<String> lines = wrapText(font, toastMessage, messageAvailable, maxMessageLines);

        graphics.text(font, Component.literal(clippedTitle), VANILLA_TEXT_X, 7, 0xFFFFFF00, false);
        for (int i = 0; i < lines.size(); i++) {
            int y = 7 + VANILLA_LINE_SPACING * (i + 1);
            if (y + font.lineHeight > textBottom - 3) break;
            graphics.text(font, Component.literal(lines.get(i)), VANILLA_TEXT_X, y, 0xFFFFFFFF, false);
        }
    }

    private static void drawModernIconTile(GuiGraphicsExtractor graphics, int x, int y, int size,
                                            ReminderToastTheme theme) {
        int surface = blendColor(theme.background(), theme.border(), 0.16f);
        fillSteppedRect(graphics, x, y, size, size, surface);
    }

    private static int blendColor(int from, int to, float amount) {
        float t = UiAnimation.clamp01(amount);
        int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
        int r = Math.round(((from >>> 16) & 255) + (((to >>> 16) & 255) - ((from >>> 16) & 255)) * t);
        int g = Math.round(((from >>> 8) & 255) + (((to >>> 8) & 255) - ((from >>> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static List<String> wrapText(Font font, String value, int maxWidth, int maxLines) {
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        int lineWidth = Math.max(1, maxWidth);
        int lineLimit = Math.max(1, maxLines);
        List<String> result = new ArrayList<>();
        if (normalized.isEmpty()) {
            result.add(fallbackMessage());
            return result;
        }

        String remaining = normalized;
        while (!remaining.isEmpty() && result.size() < lineLimit) {
            String fitted = font.plainSubstrByWidth(remaining, lineWidth);
            int fittedEnd = fitted.length();
            if (fittedEnd <= 0) {
                fittedEnd = remaining.offsetByCodePoints(0, 1);
            }
            if (fittedEnd >= remaining.length()) {
                result.add(remaining.strip());
                remaining = "";
                break;
            }

            int breakAt = fittedEnd;
            while (breakAt > 0) {
                int codePoint = remaining.codePointBefore(breakAt);
                if (Character.isWhitespace(codePoint)) break;
                breakAt = remaining.offsetByCodePoints(breakAt, -1);
            }
            if (breakAt <= 0) breakAt = fittedEnd;

            String line = remaining.substring(0, breakAt).stripTrailing();
            if (line.isEmpty()) {
                line = remaining.substring(0, fittedEnd);
                breakAt = fittedEnd;
            }
            result.add(trimToWidth(font, line, lineWidth));
            while (breakAt < remaining.length()) {
                int codePoint = remaining.codePointAt(breakAt);
                if (!Character.isWhitespace(codePoint)) break;
                breakAt = remaining.offsetByCodePoints(breakAt, 1);
            }
            remaining = remaining.substring(breakAt);
        }

        if (!remaining.isEmpty() && !result.isEmpty()) {
            int last = result.size() - 1;
            result.set(last, trimWithEllipsis(font, result.get(last), lineWidth));
        }

        return result;
    }

    private static String trimWithEllipsis(Font font, String text, int maxWidth) {
        if (text.endsWith("…")) {
            return trimToWidth(font, text, maxWidth);
        }
        String ellipsis = "…";
        int target = Math.max(0, maxWidth - font.width(ellipsis));
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > target) {
            end = text.offsetByCodePoints(end, -1);
        }
        if (end == 0 && font.width(ellipsis) > maxWidth) {
            return trimToWidth(font, text, maxWidth);
        }
        return text.substring(0, end) + ellipsis;
    }

    private static String trimToWidth(Font font, String text, int maxWidth) {
        if (maxWidth <= 0) return "";
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

    private static void drawScaled(GuiGraphicsExtractor graphics, Font font, String text,
                                   int x, int y, float scale, int color, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(Math.round(x), Math.round(y));
        graphics.pose().scale(scale, scale);
        graphics.text(font, Component.literal(text), 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }
}
