package com.aspectra00.chronicle.client.gui;

import net.minecraft.client.renderer.RenderPipelines;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.config.ReminderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;


/**
 * Responsive Chronicle notification.
 * The shell follows Minecraft's logical GUI size while typography remains user-configurable.
 */
public final class CustomReminderToast implements Toast {
    private static final long DISPLAY_TIME_MS = 7000L;
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
    private final boolean animationsEnabled;
    private final Object token = new Object();

    private Visibility visibility = Visibility.HIDE;
    private long shownAt;
    private long elapsedDisplayMs;
    private long displayDurationMs = DISPLAY_TIME_MS;

    public CustomReminderToast(ReminderConfig config, String message) {
        this(config, message, config == null ? "CHRONICLE" : config.toastTitle);
    }

    public CustomReminderToast(ReminderConfig config, String message, String resolvedTitle) {
        this(message,
                resolvedTitle,
                config == null ? "!" : config.toastIcon,
                config == null ? ReminderToastTheme.defaultMinimal() : ReminderToastTheme.fromConfig(config),
                config == null ? 1.00f : config.toastTitleScale,
                config == null ? 1.00f : config.toastMessageScale,
                config == null ? 2.00f : config.toastIconScale,
                config == null ? "MODERN" : config.toastFrameStyle,
                config == null || config.animationsEnabled);
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
        this.message = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        this.title = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        this.icon = icon == null || icon.isBlank() ? "!" : icon.trim();
        this.theme = theme == null ? ReminderToastTheme.defaultMinimal() : theme;
        this.titleScale = crispScale(titleScale, 1.00f);
        this.messageScale = crispScale(messageScale, 1.00f);
        this.iconScale = crispScale(iconScale, 2.00f);
        this.frameStyle = "VANILLA".equalsIgnoreCase(frameStyle) ? "VANILLA" : "MODERN";
        this.animationsEnabled = animationsEnabled;

        this.shownAt = 0L;
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
        return clampInt(calculated, Math.min(68, available), Math.min(92, available));
    }

    private static int vanillaWidthWithin(Font font, String title, String message, int availableWidth) {
        int available = Math.max(1, availableWidth);
        if (available < VANILLA_MIN_WIDTH) {
            return available;
        }
        String safeTitle = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        String safeMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        int longest = Math.max(font.width(safeTitle), font.width(safeMessage));
        int textWidth = Math.min(VANILLA_MAX_LINE_WIDTH, longest);
        return Math.min(available, Math.max(VANILLA_MIN_WIDTH,
                textWidth + VANILLA_TEXT_X + VANILLA_TEXT_RIGHT_MARGIN));
    }

    private static int vanillaHeightWithin(Font font, String message, int width, int availableHeight) {
        int available = Math.max(1, availableHeight);
        String safeMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        int textWidth = Math.max(1, Math.min(VANILLA_MAX_LINE_WIDTH,
                width - VANILLA_TEXT_X - VANILLA_TEXT_RIGHT_MARGIN));
        int messageLines = Math.max(1, wrapText(font, safeMessage, textWidth, 4).size());
        return Math.min(available, 20 + messageLines * VANILLA_LINE_SPACING);
    }


    @Override
    public int width() {
        Minecraft minecraft = Minecraft.getInstance();
        return layoutWidth(minecraft.font, title, message, frameStyle,
                minecraft.getWindow().getGuiScaledWidth());
    }

    @Override
    public int height() {
        Minecraft minecraft = Minecraft.getInstance();
        int currentWidth = layoutWidth(minecraft.font, title, message, frameStyle,
                minecraft.getWindow().getGuiScaledWidth());
        return layoutHeight(minecraft.font, message, frameStyle, currentWidth,
                minecraft.getWindow().getGuiScaledHeight());
    }

    /** All Chronicle layouts are at most 92px high, so reserve three stable 32px slots. */
    @Override
    public int occcupiedSlotCount() {
        return 3;
    }

    /** ToastManager allocates physical 32px slots; never multiply the slot index by this toast's height. */
    @Override
    public float yPos(int firstSlotIndex) {
        return firstSlotIndex * (float) Toast.SLOT_HEIGHT;
    }

    /** Minecraft owns the slide animation. Disabling motion makes show/hide instantaneous. */
    @Override
    public float xPos(int guiWidth, float visiblePortion) {
        int currentWidth = width();
        if (animationsEnabled) {
            return guiWidth - currentWidth * visiblePortion;
        }
        return visibility == Visibility.SHOW ? guiWidth - currentWidth : guiWidth;
    }

    static int layoutWidth(Font font, String title, String message, String frameStyle, int guiWidth) {
        int available = Math.max(1, guiWidth - 12);
        return "VANILLA".equalsIgnoreCase(frameStyle)
                ? vanillaWidthWithin(font, title, message, available)
                : responsiveWidth(guiWidth);
    }

    static int layoutHeight(Font font, String message, String frameStyle, int width, int guiHeight) {
        int available = Math.max(1, guiHeight - 12);
        return "VANILLA".equalsIgnoreCase(frameStyle)
                ? vanillaHeightWithin(font, message, width, available)
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
        if (shownAt == 0L) {
            shownAt = time;
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
        renderCard(
                graphics, font, currentWidth, currentHeight,
                message, title, icon, theme,
                titleScale, messageScale, iconScale,
                frameStyle,
                true, elapsedDisplayMs, displayDurationMs
        );
    }

    /**
     * Single source of truth for toast geometry and typography.
     * The editor preview calls this same renderer, so preview and the real toast
     * cannot drift apart visually.
     */
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
        boolean vanilla = "VANILLA".equalsIgnoreCase(frameStyle);
        int cardWidth = width;
        int cardHeight = height;
        if (vanilla) {
            cardWidth = vanillaWidthWithin(font, title, message, width);
            cardHeight = vanillaHeightWithin(font, message, cardWidth, height);
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
                !vanilla, 0L, DISPLAY_TIME_MS
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
            long displayDurationMs
    ) {
        int h = Math.max(1, height);
        int w = Math.max(1, width);
        ReminderToastTheme safeTheme = theme == null ? ReminderToastTheme.defaultMinimal() : theme;
        boolean vanilla = "VANILLA".equalsIgnoreCase(frameStyle);

        float progress = showProgress
                ? Math.max(0.0f, Math.min(1.0f,
                progressTimeMs / (float) Math.max(1L, displayDurationMs)))
                : 1.0f;

        if (vanilla) {
            renderVanillaCard(graphics, font, w, h, message, title);
            return;
        }
        String safeIcon = icon == null || icon.isBlank()
                ? "!"
                : icon.codePoints().limit(2)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        String safeTitle = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        String safeMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();

        // ToastManager already performs the complete 600ms edge slide. Rendering
        // another motion from fullyVisibleForMs creates a second, delayed nudge.
        renderModernFrame(graphics, w, h, safeTheme, showProgress, progress);

        if (w < 120 || h < 40) {
            String compactMessage = trimToWidth(font, safeMessage, Math.max(1, w - 12));
            graphics.text(font, Component.literal(compactMessage), 6,
                    Math.max(2, (h - font.lineHeight) / 2), safeTheme.message(), false);
            return;
        }

        int cardW = Math.max(1, w - 3);
        int cardH = Math.max(1, h - 3);
        boolean showIcon = cardW >= 150 && cardH >= 52;
        int textX = 14;
        if (showIcon) {
            int iconBox = Math.max(28, Math.min(34, cardH - 28));
            int iconX = 14;
            int iconY = Math.max(9, (cardH - iconBox) / 2);
            drawModernIconTile(graphics, iconX, iconY, iconBox, safeTheme);

            int iconTextWidth = font.width(Component.literal(safeIcon));
            int fittedIconScale = Math.max(1, Math.min(2,
                    Math.min((iconBox - 10) / Math.max(1, iconTextWidth),
                            (iconBox - 10) / Math.max(1, font.lineHeight))));
            float iconDrawScale = Math.min(iconScale, fittedIconScale);
            int iconTextX = iconX + Math.max(1,
                    (iconBox - Math.round(iconTextWidth * iconDrawScale)) / 2);
            int iconTextY = iconY + Math.max(1,
                    (iconBox - Math.round(font.lineHeight * iconDrawScale)) / 2);
            drawScaled(graphics, font, safeIcon, iconTextX, iconTextY, iconDrawScale,
                    safeTheme.icon(), false);
            textX = iconX + iconBox + 12;
        }

        int rightPad = 14;
        int contentRight = cardW;
        int available = Math.max(1, contentRight - rightPad - textX);

        int titlePixelHeight = Math.max(font.lineHeight, (int) Math.ceil(font.lineHeight * titleScale));
        int bodyPixelHeight = Math.max(font.lineHeight, (int) Math.ceil(font.lineHeight * messageScale));
        int titleMaxWidth = Math.max(1, (int) (available / titleScale));
        String clippedTitle = trimToWidth(font, safeTitle, titleMaxWidth);

        int maxLines = cardH >= titlePixelHeight + bodyPixelHeight * 2 + 26 ? 2 : 1;
        int bodyMaxWidth = Math.max(1, (int) (available / messageScale));
        List<String> lines = wrapText(font, safeMessage, bodyMaxWidth, maxLines);

        int bodyLineHeight = bodyPixelHeight + 1;
        int totalBodyHeight = lines.size() * bodyLineHeight;
        int totalTextHeight = titlePixelHeight + 5 + totalBodyHeight;
        int titleY = Math.max(9, (cardH - totalTextHeight) / 2);
        int bodyStartY = titleY + titlePixelHeight + 5;

        drawScaled(graphics, font, clippedTitle, textX, titleY, titleScale,
                safeTheme.title(), false);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = bodyStartY + i * bodyLineHeight;
            if (lineY + bodyPixelHeight > cardH - 7) {
                break;
            }
            drawScaled(graphics, font, lines.get(i), textX, lineY,
                    messageScale, safeTheme.message(), false);
        }
    }

    private static void renderModernFrame(GuiGraphicsExtractor graphics, int width, int height,
                                           ReminderToastTheme theme, boolean showProgress, float progress) {
        if (width < 6 || height < 6) {
            graphics.fill(0, 0, width, height, theme.background());
            return;
        }
        int cardW = Math.max(1, width - 3);
        int cardH = Math.max(1, height - 3);
        // A stepped two-pixel corner reads as rounded in Minecraft's pixel UI and
        // avoids introducing a blurred texture that would fight resource packs.
        fillSteppedRect(graphics, 3, 3, cardW, cardH, 0x52000000);
        fillSteppedRect(graphics, 0, 0, cardW, cardH, theme.border());
        int surface = blendColor(theme.background(), theme.border(), 0.08f);
        fillSteppedRect(graphics, 1, 1, Math.max(1, cardW - 2), Math.max(1, cardH - 2), surface);

        if (cardW > 10 && cardH > 8) {
            int highlight = blendColor(theme.border(), theme.accent(), 0.24f);
            graphics.fill(4, 2, cardW - 4, 3, highlight);
        }
        if (cardH > 26) {
            graphics.fill(2, 12, 4, cardH - 12, theme.accent());
        }
        if (showProgress && cardW > 8) {
            float remaining = 1.0f - UiAnimation.clamp01(progress);
            int progressEnd = 4 + Math.round((cardW - 8) * remaining);
            graphics.fill(4, cardH - 3, Math.max(4, Math.min(cardW - 4, progressEnd)),
                    cardH - 2, theme.accent());
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
                                          String message, String title) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_TOAST_BACKGROUND, 0, 0, width, height);
        if (width < VANILLA_TEXT_X + 8 || height < font.lineHeight + 4) {
            return;
        }

        String safeTitle = title == null || title.isBlank() ? "CHRONICLE" : title.trim();
        String safeMessage = message == null || message.isBlank() ? fallbackMessage() : message.trim();
        int available = Math.max(1, Math.min(VANILLA_MAX_LINE_WIDTH,
                width - VANILLA_TEXT_X - VANILLA_TEXT_RIGHT_MARGIN));
        String clippedTitle = trimToWidth(font, safeTitle, available);
        int maxMessageLines = Math.max(1, (height - 20) / VANILLA_LINE_SPACING);
        List<String> lines = wrapText(font, safeMessage, available, maxMessageLines);

        // Match Minecraft 26.2 SystemToast's native text inset and vertical metrics.
        graphics.text(font, Component.literal(clippedTitle), VANILLA_TEXT_X, 7, 0xFFFFFF00, false);
        for (int i = 0; i < lines.size(); i++) {
            int y = 7 + VANILLA_LINE_SPACING * (i + 1);
            if (y + font.lineHeight > height - 3) break;
            graphics.text(font, Component.literal(lines.get(i)), VANILLA_TEXT_X, y, 0xFFFFFFFF, false);
        }
    }

    private static void drawModernIconTile(GuiGraphicsExtractor graphics, int x, int y, int size,
                                            ReminderToastTheme theme) {
        int surface = blendColor(theme.background(), theme.accent(), 0.14f);
        fillSteppedRect(graphics, x, y, size, size, theme.border());
        fillSteppedRect(graphics, x + 1, y + 1, size - 2, size - 2, surface);
        if (size > 12) {
            graphics.fill(x + 2, y + 5, x + 4, y + size - 5, theme.accent());
        }
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
        int safeWidth = Math.max(1, maxWidth);
        int safeLines = Math.max(1, maxLines);
        List<String> result = new ArrayList<>();
        if (normalized.isEmpty()) {
            result.add(fallbackMessage());
            return result;
        }

        String remaining = normalized;
        while (!remaining.isEmpty() && result.size() < safeLines) {
            String fitted = font.plainSubstrByWidth(remaining, safeWidth);
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
            result.add(trimToWidth(font, line, safeWidth));
            while (breakAt < remaining.length()) {
                int codePoint = remaining.codePointAt(breakAt);
                if (!Character.isWhitespace(codePoint)) break;
                breakAt = remaining.offsetByCodePoints(breakAt, 1);
            }
            remaining = remaining.substring(breakAt);
        }

        if (!remaining.isEmpty() && !result.isEmpty()) {
            int last = result.size() - 1;
            result.set(last, trimWithEllipsis(font, result.get(last), safeWidth));
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
