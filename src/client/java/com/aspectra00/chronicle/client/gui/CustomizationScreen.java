package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleI18n;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class CustomizationScreen extends Screen {
    private static final int CARD = 0xFF141A22;
    private static final int CARD_HOVER = 0xFF19212B;

    private record Layout(
            int panelW, int left, int top, int bottom, int inset,
            int headerH, int contentTop, int cardH, int cardGap,
            int footerTop, int footerY, int footerH
    ) {}

    private final Screen parent;
    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private Button notificationButton;
    private Button soundButton;
    private Button aboutButton;
    private Button closeButton;
    private long lastPressedAt = -1L;
    private int lastPressedX;
    private int lastPressedY;
    private int lastPressedW;
    private int lastPressedH;

    public CustomizationScreen(Screen parent) {
        super(ChronicleI18n.component("customize.title"));
        this.parent = parent;
    }

    @Override
    public void added() {
        super.added();
        transition.added();
    }

    @Override
    protected void rebuildWidgets() {
        clearFocus();
        setDragging(false);
        init();
    }

    @Override
    public void resize(int width, int height) {
        lastPressedAt = -1L;
        clearFocus();
        setDragging(false);
        super.resize(width, height);
    }

    @Override
    public void tick() {
        super.tick();
        transition.tick(this.minecraft);
    }

    @Override
    protected void init() {
        clearFocus();
        setDragging(false);
        clearWidgets();
        Layout layout = layout();
        int contentX = layout.left() + layout.inset();
        int contentW = Math.max(1, layout.panelW() - layout.inset() * 2);
        int y = layout.contentTop();
        notificationButton = choice("customize.notification.title", contentX, y,
                contentW, layout.cardH(), b -> openNotification());
        y += layout.cardH() + layout.cardGap();
        soundButton = choice("customize.sound.title", contentX, y,
                contentW, layout.cardH(), b -> openSound());
        y += layout.cardH() + layout.cardGap();
        aboutButton = choice("customize.about.title", contentX, y,
                contentW, layout.cardH(), b -> openAbout());
        int closeW = Math.min(132, Math.max(86, contentW / 4));
        closeButton = button(ChronicleI18n.tr("action.done"),
                layout.left() + layout.panelW() - layout.inset() - closeW,
                layout.footerY(), closeW, layout.footerH(), b -> onClose());
        addRenderableWidget(notificationButton);
        addRenderableWidget(soundButton);
        addRenderableWidget(aboutButton);
        addRenderableWidget(closeButton);
    }

    private Layout layout() {
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        int bottom = Math.max(top + 1, this.height - 12);
        int inset = UiMetrics.contentInset(panelW);
        boolean compact = panelW < 520 || this.height < 390;
        int headerH = UiMetrics.headerHeight(compact);
        int footerH = compact ? UiMetrics.COMPACT_CONTROL_HEIGHT : UiMetrics.PRIMARY_BUTTON_HEIGHT;
        int footerInset = compact ? UiMetrics.GAP_SM : UiMetrics.GAP_MD;
        int footerTop = Math.max(top + headerH + 1, bottom - footerH - footerInset * 2);
        int footerY = Math.max(0, Math.min(bottom - footerH, footerTop + footerInset));
        int preferredCardH = compact ? 46 : 58;
        int cardGap = compact ? UiMetrics.GAP_XS : UiMetrics.GAP_SM;
        int contentTop = top + headerH + (compact ? UiMetrics.GAP_SM : UiMetrics.GAP_MD);
        int available = Math.max(3, footerTop - UiMetrics.GAP_SM - contentTop - cardGap * 2);
        int cardH = Math.max(1, Math.min(preferredCardH, available / 3));
        return new Layout(panelW, left, top, bottom, inset, headerH,
                contentTop, cardH, cardGap, footerTop, footerY, footerH);
    }

    private Button choice(String key, int x, int y, int width, int height, Button.OnPress action) {
        return button(ChronicleI18n.tr(key), x, y, width, height, action);
    }

    private Button button(String label, int x, int y, int width, int height, Button.OnPress action) {
        return Button.builder(Component.literal(label), button -> {
            if (transition.isClosing()) return;
            lastPressedAt = Util.getMillis();
            lastPressedX = button.getX();
            lastPressedY = button.getY();
            lastPressedW = button.getWidth();
            lastPressedH = button.getHeight();
            action.onPress(button);
        }).bounds(x, y, width, height).build();
    }

    private void openNotification() {
        transition.start(this.minecraft, new ToastCustomizerScreen(this));
    }

    private void openSound() {
        transition.start(this.minecraft, new NotificationSoundScreen(this));
    }

    private void openAbout() {
        transition.start(this.minecraft, new SupportersScreen(this));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        long now = Util.getMillis();
        transition.begin(graphics, this.width, this.height);
        Layout layout = layout();
        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);
        UiFrame.drawWindow(graphics, layout.left(), layout.top(), layout.panelW(), layout.bottom());
        int contentX = layout.left() + layout.inset();
        int contentW = Math.max(1, layout.panelW() - layout.inset() * 2);
        boolean compact = layout.panelW() < 520 || this.height < 390;
        String title = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("customize.title"), contentW);
        String subtitle = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("customize.subtitle"), contentW);
        graphics.text(this.font, Component.literal(title), contentX,
                UiMetrics.headerTitleY(layout.top(), compact), UiFrame.TEXT, true);
        graphics.text(this.font, Component.literal(subtitle), contentX,
                UiMetrics.headerSubtitleY(layout.top(), compact), UiFrame.MUTED, false);
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                UiMetrics.headerDividerY(layout.top(), layout.headerH()));
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                layout.footerTop());

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        drawChoice(graphics, notificationButton, ChronicleI18n.tr("customize.notification.description"),
                mouseX, mouseY);
        drawChoice(graphics, soundButton, ChronicleI18n.tr("customize.sound.description"),
                mouseX, mouseY);
        drawChoice(graphics, aboutButton, ChronicleI18n.tr("customize.about.description"),
                mouseX, mouseY);
        UiFrame.drawButton(graphics, this.font, closeButton, UiFrame.ACCENT,
                true, mouseX, mouseY);

        if (lastPressedAt >= 0L) {
            float pulse = UiAnimation.pressProgress(lastPressedAt, now, 170L);
            if (pulse > 0.0f) {
                int inset = UiAnimation.pressInset(pulse);
                int alpha = Math.round(pulse * 58.0f);
                graphics.fill(lastPressedX - inset, lastPressedY - inset,
                        lastPressedX + lastPressedW + inset,
                        lastPressedY + lastPressedH + inset,
                        (alpha << 24) | 0x008FB3E8);
            }
        }
        transition.end(graphics, this.width, this.height);
    }

    private void drawChoice(GuiGraphicsExtractor graphics, Button button, String description,
                            int mouseX, int mouseY) {
        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        boolean hovered = button.active && mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
        boolean focused = button.active && button.isFocused()
                && this.minecraft.getLastInputType().isKeyboard();
        int border = hovered || focused ? UiFrame.ACCENT : UiFrame.INNER_LINE;
        int surface = hovered ? CARD_HOVER : CARD;
        graphics.fill(x, y, x + width, y + height, border);
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, surface);
        }
        if (width > 4 && height > 3) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + 4, border);
        }
        int textX = x + Math.min(14, Math.max(6, width / 12));
        int textW = Math.max(1, width - (textX - x) * 2);
        String title = UiFrame.trimToWidth(this.font, button.getMessage().getString(), textW);
        String detail = UiFrame.trimToWidth(this.font, description, textW);
        if (height >= this.font.lineHeight * 2 + 12) {
            int titleY = y + Math.max(6, (height - this.font.lineHeight * 2 - 5) / 2);
            graphics.text(this.font, Component.literal(title), textX, titleY,
                    UiFrame.TEXT, true);
            graphics.text(this.font, Component.literal(detail), textX,
                    titleY + this.font.lineHeight + 5, UiFrame.MUTED, false);
        } else {
            graphics.text(this.font, Component.literal(title), textX,
                    UiMetrics.centeredTextY(y, height, this.font.lineHeight),
                    hovered || focused ? UiFrame.TEXT : UiFrame.MUTED, false);
        }
    }

    @Override
    public void onClose() {
        transition.start(this.minecraft, parent);
    }
}
