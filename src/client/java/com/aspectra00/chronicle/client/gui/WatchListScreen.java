package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.WatchManager;
import com.aspectra00.chronicle.client.config.WatchTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WatchListScreen extends Screen {
    private static final int ROW = 0xFF141A22;
    private static final int ROW_HOVER = 0xFF18212C;
    private static final int PANEL_INNER = 0xFF0C1118;
    private static final int NEGATIVE = 0xFFB88189;
    private static final int POSITIVE = 0xFF8FC7AA;
    private static final int ACCENT = UiFrame.ACCENT;
    private static final int TEXT = UiFrame.TEXT;
    private static final int MUTED = UiFrame.MUTED;

    private record Layout(
            int panelW, int left, int top, int bottom, int inset,
            int listTop, int listBottom, int rowHeight, int rowStep,
            int footerTop, int footerY, int footerH, boolean compact
    ) {}

    private final Screen parent;
    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private final Map<Button, WatchTarget> removeButtons = new IdentityHashMap<>();
    private int scrollOffset;
    private long watchRevision;
    private String lastConfigError;
    private String saveError;
    private long clearConfirmationUntil;
    private Button clearButton;
    private Button closeButton;
    private long lastPressedAt = -1L;
    private int lastPressedX;
    private int lastPressedY;
    private int lastPressedW;
    private int lastPressedH;

    public WatchListScreen(Screen parent) {
        super(ChronicleI18n.component("watch.title"));
        this.parent = parent;
        this.lastConfigError = ChronicleClient.getRuntimeConfigError();
        this.saveError = lastConfigError;
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
        if (transition.tick(this.minecraft)) return;
        String runtimeError = ChronicleClient.getRuntimeConfigError();
        if (!Objects.equals(runtimeError, lastConfigError)) {
            if (saveError == null || Objects.equals(saveError, lastConfigError)) {
                saveError = runtimeError;
            }
            lastConfigError = runtimeError;
        }
        if (clearConfirmationUntil > 0L && Util.getMillis() >= clearConfirmationUntil) {
            clearConfirmationUntil = 0L;
            init(this.width, this.height);
        } else if (watchRevision != ChronicleClient.WATCH_REVISION) {
            init(this.width, this.height);
        }
    }

    @Override
    protected void init() {
        clearFocus();
        setDragging(false);
        clearWidgets();
        removeButtons.clear();
        watchRevision = ChronicleClient.WATCH_REVISION;
        Layout layout = layout();
        List<WatchTarget> watches = WatchManager.watchesForCurrentWorld(this.minecraft);
        int visibleRows = visibleRows(layout);
        int maxScroll = Math.max(0, watches.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int start = Math.min(scrollOffset, watches.size());
        int end = Math.min(watches.size(), start + visibleRows);
        int buttonW = layout.compact() ? 62 : 78;
        int buttonH = layout.compact() ? 22 : 24;
        for (int index = start; index < end; index++) {
            WatchTarget watch = watches.get(index);
            int y = layout.listTop() + (index - start) * layout.rowStep();
            int x = layout.left() + layout.panelW() - layout.inset() - buttonW - 8;
            Button remove = button(ChronicleI18n.tr("action.remove"), x,
                    y + (layout.rowHeight() - buttonH) / 2, buttonW, buttonH,
                    value -> removeWatch(watch));
            removeButtons.put(remove, watch);
            addRenderableWidget(remove);
        }

        int gap = UiMetrics.GAP_SM;
        int contentW = Math.max(1, layout.panelW() - layout.inset() * 2);
        int clearW = Math.min(156, Math.max(96, (contentW - gap) / 2));
        int doneW = Math.min(104, Math.max(72, (contentW - gap) / 3));
        String clearLabel = clearConfirmationUntil > Util.getMillis()
                ? ChronicleI18n.tr("watch.clear.confirm")
                : ChronicleI18n.tr("watch.clear");
        clearButton = button(clearLabel, layout.left() + layout.inset(), layout.footerY(),
                clearW, layout.footerH(), value -> clearWatches());
        clearButton.active = !watches.isEmpty();
        closeButton = button(ChronicleI18n.tr("action.done"),
                layout.left() + layout.panelW() - layout.inset() - doneW,
                layout.footerY(), doneW, layout.footerH(), value -> onClose());
        addRenderableWidget(clearButton);
        addRenderableWidget(closeButton);
    }

    private Layout layout() {
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        int bottom = Math.max(top + 1, this.height - 12);
        int inset = UiMetrics.contentInset(panelW);
        boolean compact = panelW < 520 || this.height < 340;
        int headerH = UiMetrics.headerHeight(compact);
        int footerH = compact ? UiMetrics.COMPACT_CONTROL_HEIGHT : UiMetrics.PRIMARY_BUTTON_HEIGHT;
        int footerPadding = compact ? UiMetrics.GAP_SM : UiMetrics.GAP_MD;
        int footerTop = Math.max(top + headerH, bottom - footerH - footerPadding * 2);
        int listTop = Math.min(footerTop, top + headerH + UiMetrics.GAP_MD);
        int errorStrip = saveError == null ? 0 : this.font.lineHeight + UiMetrics.GAP_SM;
        int listBottom = Math.max(listTop, footerTop - UiMetrics.GAP_SM - errorStrip);
        int rowHeight = compact ? 50 : 54;
        int rowStep = rowHeight + UiMetrics.GAP_SM;
        int footerY = Math.max(0, Math.min(bottom - footerH, footerTop + footerPadding));
        return new Layout(panelW, left, top, bottom, inset, listTop, listBottom,
                rowHeight, rowStep, footerTop, footerY, footerH, compact);
    }

    private int visibleRows(Layout layout) {
        int available = Math.max(0, layout.listBottom() - layout.listTop());
        if (available < layout.rowHeight()) return 0;
        return 1 + (available - layout.rowHeight()) / layout.rowStep();
    }

    private void removeWatch(WatchTarget watch) {
        if (!WatchManager.removeWatch(watch)) {
            saveError = ChronicleClient.getRuntimeConfigError();
        } else {
            saveError = null;
        }
        init(this.width, this.height);
    }

    private void clearWatches() {
        long now = Util.getMillis();
        if (clearConfirmationUntil <= now) {
            clearConfirmationUntil = now + 3_000L;
            clearButton.setMessage(ChronicleI18n.component("watch.clear.confirm"));
            return;
        }
        clearConfirmationUntil = 0L;
        if (!WatchManager.clearCurrentWatches(this.minecraft)) {
            saveError = ChronicleClient.getRuntimeConfigError();
        } else {
            saveError = null;
            scrollOffset = 0;
        }
        init(this.width, this.height);
    }

    private Button button(String label, int x, int y, int width, int height,
                          Button.OnPress action) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        Layout layout = layout();
        List<WatchTarget> watches = WatchManager.watchesForCurrentWorld(this.minecraft);
        int maxScroll = Math.max(0, watches.size() - visibleRows(layout));
        boolean inside = mouseX >= layout.left() && mouseX < layout.left() + layout.panelW()
                && mouseY >= layout.listTop() && mouseY < layout.listBottom();
        if (inside && maxScroll > 0 && Math.abs(verticalAmount) >= 1.0E-9) {
            scrollOffset = verticalAmount < 0
                    ? Math.min(maxScroll, scrollOffset + 1)
                    : Math.max(0, scrollOffset - 1);
            init(this.width, this.height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        long now = Util.getMillis();
        transition.begin(graphics, this.width, this.height);
        Layout layout = layout();
        List<WatchTarget> watches = WatchManager.watchesForCurrentWorld(this.minecraft);
        int visibleRows = visibleRows(layout);
        int maxScroll = Math.max(0, watches.size() - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);
        UiFrame.drawWindow(graphics, layout.left(), layout.top(), layout.panelW(), layout.bottom());
        int headerX = layout.left() + layout.inset();
        int headerW = Math.max(1, layout.panelW() - layout.inset() * 2);
        graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                        ChronicleI18n.tr("watch.title"), headerW)), headerX,
                UiMetrics.headerTitleY(layout.top(), layout.compact()), TEXT, true);
        String subtitle = ChronicleI18n.tr(watches.size() == 1
                ? "watch.subtitle.one" : "watch.subtitle.many", watches.size());
        graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                        subtitle, headerW)), headerX,
                UiMetrics.headerSubtitleY(layout.top(), layout.compact()), MUTED, false);
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                UiMetrics.headerDividerY(layout.top(), UiMetrics.headerHeight(layout.compact())));
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                layout.footerTop());

        graphics.fill(layout.left() + layout.inset(), layout.listTop() - UiMetrics.GAP_SM,
                layout.left() + layout.panelW() - layout.inset(),
                layout.listBottom(), PANEL_INNER);
        int start = Math.min(scrollOffset, watches.size());
        int end = Math.min(watches.size(), start + visibleRows);
        for (int index = start; index < end; index++) {
            WatchTarget watch = watches.get(index);
            int y = layout.listTop() + (index - start) * layout.rowStep();
            drawWatch(graphics, watch, layout, y, mouseX, mouseY);
        }

        if (watches.isEmpty()) {
            int emptyY = layout.listTop() + Math.max(6,
                    (Math.max(1, layout.listBottom() - layout.listTop()) - 30) / 2);
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            ChronicleI18n.tr("watch.empty"), headerW - 16)),
                    headerX + 8, emptyY, TEXT, false);
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            ChronicleI18n.tr("watch.empty_hint"), headerW - 16)),
                    headerX + 8, emptyY + 18, MUTED, false);
        }

        if (maxScroll > 0) {
            UiFrame.drawScrollBar(graphics,
                    Math.max(layout.left(),
                            layout.left() + layout.panelW() - layout.inset() - 3),
                    layout.listTop() + 4, layout.listBottom() - 4,
                    scrollOffset / (float) maxScroll,
                    visibleRows / (float) Math.max(1, watches.size()));
        }
        if (saveError != null) {
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            saveError, headerW)), headerX,
                    layout.listBottom() + 3, NEGATIVE, false);
        }

        super.render(graphics, mouseX, mouseY, delta);
        for (var child : children()) {
            if (child instanceof Button button) {
                boolean remove = removeButtons.containsKey(button) || button == clearButton;
                boolean emphasized = button == closeButton
                        || button == clearButton && clearConfirmationUntil > now;
                UiFrame.drawButton(graphics, this.font, button,
                        remove ? NEGATIVE : ACCENT, emphasized, mouseX, mouseY);
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

    private void drawWatch(GuiGraphics graphics, WatchTarget watch,
                           Layout layout, int y, int mouseX, int mouseY) {
        int x = layout.left() + layout.inset();
        int width = layout.panelW() - layout.inset() * 2;
        boolean hovered = mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + layout.rowHeight();
        graphics.fill(x, y, x + width, y + layout.rowHeight(), hovered ? ROW_HOVER : ROW);
        graphics.fill(x, y, x + 2, y + layout.rowHeight(), POSITIVE);
        int textX = x + 10;
        int removeW = layout.compact() ? 78 : 94;
        int textW = Math.max(1, width - 20 - removeW);
        graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                        watch.label, textW)), textX, y + 7, TEXT, true);
        graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                        WatchManager.condition(watch), textW)),
                textX, y + 24, POSITIVE, false);
        if (!layout.compact()) {
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            WatchManager.detail(watch), textW)),
                    textX, y + 38, MUTED, false);
        }
    }

    @Override
    public void onClose() {
        transition.start(this.minecraft, parent);
    }
}
