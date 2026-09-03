package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.ChronicleNeoForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

import java.net.URI;
import java.util.List;

public final class SupportersScreen extends Screen {
    private static final URI SUPPORT_URI = URI.create("https://ko-fi.com/aspectra");
    private static final int PANEL_INNER = 0xFF0C1118;
    private static final int CARD = 0xFF141A22;
    private static final int TEXT = UiFrame.TEXT;
    private static final int MUTED = UiFrame.MUTED;
    private static final int ACCENT = UiFrame.ACCENT;
    private static final int SUPPORT = 0xFFD4B165;

    private record Layout(
            int panelW, int left, int top, int bottom, int inset,
            int headerH, int guideTop, int guideBottom,
            int sectionY, int listTop, int listBottom,
            int footerTop, int footerY, int footerH,
            int columns, int gap, int rowH
    ) {}

    private final Screen parent;
    private final List<String> supporters = SupporterList.load();
    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private int scrollRow;
    private Button supportButton;
    private Button closeButton;
    private long lastPressedAt = -1L;
    private int lastPressedX;
    private int lastPressedY;
    private int lastPressedW;
    private int lastPressedH;

    public SupportersScreen(Screen parent) {
        super(ChronicleI18n.component("about.title"));
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
    public void resize(Minecraft minecraft, int width, int height) {
        lastPressedAt = -1L;
        clearFocus();
        setDragging(false);
        super.resize(minecraft, width, height);
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
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll(layout)));

        int contentW = Math.max(1, layout.panelW() - layout.inset() * 2);
        int buttonGap = UiMetrics.GAP_SM;
        boolean narrow = contentW < 360;
        int supportW = narrow ? Math.max(1, (contentW - buttonGap) / 2)
                : Math.min(180, Math.max(118, contentW / 4));
        int closeW = narrow ? Math.max(1, contentW - supportW - buttonGap)
                : Math.min(104, Math.max(76, contentW / 5));
        int supportX = layout.left() + layout.inset();
        int closeX = layout.left() + layout.panelW() - layout.inset() - closeW;
        supportButton = button(ChronicleI18n.tr("action.open_kofi"),
                supportX, layout.footerY(), supportW, layout.footerH(),
                b -> Util.getPlatform().openUri(SUPPORT_URI));
        closeButton = button(ChronicleI18n.tr("action.done"),
                closeX, layout.footerY(), closeW, layout.footerH(), b -> onClose());
        addRenderableWidget(supportButton);
        addRenderableWidget(closeButton);
    }

    private Layout layout() {
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        int bottom = Math.max(top + 1, this.height - 12);
        int inset = UiMetrics.contentInset(panelW);
        boolean compact = panelW < 520 || this.height < 360;
        int headerH = UiMetrics.headerHeight(compact);
        int guideTop = top + headerH + (compact ? UiMetrics.GAP_SM : UiMetrics.GAP_MD);
        int guideBottom = guideTop + (compact ? 66 : 72);
        int sectionY = guideBottom + (compact ? UiMetrics.GAP_XS : UiMetrics.GAP_SM);
        int listTop = sectionY + this.font.lineHeight + UiMetrics.GAP_SM;
        int footerH = compact ? UiMetrics.COMPACT_CONTROL_HEIGHT : UiMetrics.PRIMARY_BUTTON_HEIGHT;
        int footerInset = compact ? UiMetrics.GAP_SM : UiMetrics.GAP_MD;
        int footerTop = Math.max(listTop, bottom - footerH - footerInset * 2);
        int listBottom = Math.max(listTop, footerTop - UiMetrics.GAP_SM);
        int footerY = Math.max(0, Math.min(bottom - footerH, footerTop + footerInset));
        int columns = panelW < 420 ? 1 : panelW < 720 ? 2 : 3;
        int gap = compact ? UiMetrics.GAP_XS : UiMetrics.GAP_SM;
        int rowH = compact ? 24 : UiMetrics.CONTROL_HEIGHT;
        return new Layout(panelW, left, top, bottom, inset,
                headerH, guideTop, guideBottom, sectionY, listTop, listBottom,
                footerTop, footerY, footerH, columns, gap, rowH);
    }

    private int totalRows(Layout layout) {
        return (supporters.size() + layout.columns() - 1) / layout.columns();
    }

    private int visibleRows(Layout layout) {
        int available = Math.max(0, layout.listBottom() - layout.listTop());
        if (available < layout.rowH()) return 0;
        return 1 + (available - layout.rowH()) / (layout.rowH() + layout.gap());
    }

    private int maxScroll(Layout layout) {
        return Math.max(0, totalRows(layout) - visibleRows(layout));
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        Layout layout = layout();
        int maxScroll = maxScroll(layout);
        boolean inside = mouseX >= layout.left() && mouseX < layout.left() + layout.panelW()
                && mouseY >= layout.listTop() && mouseY < layout.listBottom();
        if (inside && maxScroll > 0 && Math.abs(amount) >= 1.0E-9) {
            scrollRow = amount < 0
                    ? Math.min(maxScroll, scrollRow + 1)
                    : Math.max(0, scrollRow - 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        long now = Util.getMillis();
        transition.begin(graphics, this.width, this.height);
        Layout layout = layout();
        int maxScroll = maxScroll(layout);
        if (scrollRow > maxScroll) scrollRow = maxScroll;

        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);
        UiFrame.drawWindow(graphics, layout.left(), layout.top(), layout.panelW(), layout.bottom());
        int contentX = layout.left() + layout.inset();
        int contentW = Math.max(1, layout.panelW() - layout.inset() * 2);
        boolean compact = layout.panelW() < 520 || this.height < 360;
        String title = UiFrame.trimToWidth(this.font, ChronicleI18n.tr("about.title"), contentW);
        graphics.drawString(this.font, Component.literal(title), contentX,
                UiMetrics.headerTitleY(layout.top(), compact), TEXT, true);
        String subtitle = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("about.subtitle", version()), contentW);
        graphics.drawString(this.font, Component.literal(subtitle), contentX,
                UiMetrics.headerSubtitleY(layout.top(), compact), MUTED, false);
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                UiMetrics.headerDividerY(layout.top(), layout.headerH()));
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                layout.guideBottom());
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                layout.footerTop());

        String guideTitle = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("about.guide.title"), contentW);
        graphics.drawString(this.font, Component.literal(guideTitle), contentX,
                layout.guideTop(), SUPPORT, false);
        int guideY = layout.guideTop() + this.font.lineHeight + 5;
        String stepOne = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("about.guide.step1"), contentW);
        String stepTwo = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("about.guide.step2"), contentW);
        String stepThree = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("about.guide.step3"), contentW);
        graphics.drawString(this.font, Component.literal(stepOne), contentX, guideY, TEXT, false);
        graphics.drawString(this.font, Component.literal(stepTwo), contentX,
                guideY + this.font.lineHeight + 2, TEXT, false);
        graphics.drawString(this.font, Component.literal(stepThree), contentX,
                guideY + (this.font.lineHeight + 2) * 2, TEXT, false);
        String note = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("about.guide.note"), contentW);
        graphics.drawString(this.font, Component.literal(note), contentX,
                guideY + (this.font.lineHeight + 2) * 3, MUTED, false);

        String section = ChronicleI18n.tr("about.supporters");
        graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                        section, contentW)), contentX, layout.sectionY(), ACCENT, false);
        graphics.fill(layout.left() + layout.inset(), layout.listTop() - UiMetrics.GAP_XS,
                layout.left() + layout.panelW() - layout.inset(),
                layout.listBottom(), PANEL_INNER);

        int visibleRows = visibleRows(layout);
        int start = scrollRow * layout.columns();
        int end = Math.min(supporters.size(),
                (scrollRow + visibleRows) * layout.columns());
        int cardAreaW = Math.max(1, contentW - layout.gap() * (layout.columns() - 1));
        int cardW = Math.max(1, cardAreaW / layout.columns());
        for (int index = start; index < end; index++) {
            int visibleIndex = index - start;
            int row = visibleIndex / layout.columns();
            int column = visibleIndex % layout.columns();
            int x = contentX + column * (cardW + layout.gap());
            int right = column == layout.columns() - 1
                    ? contentX + contentW : x + cardW;
            int y = layout.listTop() + row * (layout.rowH() + layout.gap());
            graphics.fill(x, y, right, y + layout.rowH(), CARD);
            graphics.fill(x, y, x + 2, y + layout.rowH(), ACCENT);
            String name = UiFrame.trimToWidth(this.font, supporters.get(index),
                    Math.max(1, right - x - 16));
            int textX = x + Math.max(8, (right - x - this.font.width(name)) / 2);
            int textY = UiMetrics.centeredTextY(y, layout.rowH(), this.font.lineHeight);
            graphics.drawString(this.font, Component.literal(name), textX, textY, TEXT, false);
        }

        if (supporters.isEmpty()) {
            int emptyY = layout.listTop() + Math.max(6,
                    (Math.max(1, layout.listBottom() - layout.listTop()) - 30) / 2);
            String emptyTitle = UiFrame.trimToWidth(this.font,
                    ChronicleI18n.tr("about.empty"), Math.max(1, contentW - 16));
            String emptyHint = UiFrame.trimToWidth(this.font,
                    ChronicleI18n.tr("about.empty_hint"), Math.max(1, contentW - 16));
            graphics.drawString(this.font, Component.literal(emptyTitle), contentX + 8,
                    emptyY, TEXT, false);
            graphics.drawString(this.font, Component.literal(emptyHint), contentX + 8,
                    emptyY + 18, MUTED, false);
        }

        if (maxScroll > 0) {
            UiFrame.drawScrollBar(graphics,
                    Math.max(layout.left(),
                            layout.left() + layout.panelW() - layout.inset() - 3),
                    layout.listTop() + 4, layout.listBottom() - 4,
                    scrollRow / (float) maxScroll,
                    visibleRows / (float) Math.max(1, totalRows(layout)));
        }

        super.render(graphics, mouseX, mouseY, delta);
        for (var child : children()) {
            if (child instanceof Button button) {
                int accent = button == supportButton ? SUPPORT : ACCENT;
                UiFrame.drawButton(graphics, this.font, button, accent,
                        button == closeButton, mouseX, mouseY);
            }
        }

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

    private static String version() {
        return ChronicleNeoForge.version();
    }

    private void clearFocus() {
        setFocused(null);
    }

    @Override
    public void onClose() {
        transition.start(this.minecraft, parent);
    }
}
