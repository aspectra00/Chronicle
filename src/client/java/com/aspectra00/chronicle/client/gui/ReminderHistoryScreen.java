package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.config.ReminderHistoryEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ReminderHistoryScreen extends Screen {
    private static final int ROW = 0xFF141A22;
    private static final int PANEL_INNER = 0xFF0C1118;
    private static final int NEGATIVE = 0xFFB88189;
    private static final int POSITIVE = 0xFF8FC7AA;
    private static final int ACCENT = UiFrame.ACCENT;
    private static final int TEXT = UiFrame.TEXT;
    private static final int MUTED = UiFrame.MUTED;

    private enum Filter {
        ALL,
        MISSED,
        COMPLETED,
        SNOOZED
    }

    private record HistoryLayout(
            int panelW, int left, int top, int bottom, int inset,
            int filterY, int filterH, int filterGap, int filterColumns,
            int listTop, int listBottom, int rowHeight, int rowStep,
            int footerTop, int footerY, int footerH
    ) {}

    private final Screen parent;
    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private final Map<Button, Filter> filterButtons = new IdentityHashMap<>();
    private Filter filter = Filter.ALL;
    private int scrollOffset;
    private long historyRevision;
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

    public ReminderHistoryScreen(Screen parent) {
        super(ChronicleI18n.component("history.title"));
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
        } else if (historyRevision != ChronicleClient.HISTORY_REVISION) {
            init(this.width, this.height);
        }
    }

    @Override
    protected void init() {
        clearFocus();
        setDragging(false);
        clearWidgets();
        filterButtons.clear();
        historyRevision = ChronicleClient.HISTORY_REVISION;
        HistoryLayout layout = layout();
        List<ReminderHistoryEntry> entries = filteredEntries();
        int visibleRows = visibleRows(layout);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        Filter[] filters = Filter.values();
        int filterW = Math.max(1, (layout.panelW() - layout.inset() * 2
                - layout.filterGap() * (layout.filterColumns() - 1)) / layout.filterColumns());
        for (int index = 0; index < filters.length; index++) {
            Filter value = filters[index];
            int column = index % layout.filterColumns();
            int row = index / layout.filterColumns();
            int x = layout.left() + layout.inset()
                    + column * (filterW + layout.filterGap());
            int y = layout.filterY() + row * (layout.filterH() + layout.filterGap());
            int width = column == layout.filterColumns() - 1
                    ? layout.left() + layout.panelW() - layout.inset() - x : filterW;
            Button button = button(filterLabel(value), x, y, Math.max(1, width),
                    layout.filterH(), b -> selectFilter(value));
            filterButtons.put(button, value);
            addRenderableWidget(button);
        }

        int footerGap = UiMetrics.GAP_SM;
        int contentW = Math.max(1, layout.panelW() - layout.inset() * 2);
        boolean narrow = contentW < 360;
        int clearW = narrow ? Math.max(1, (contentW - footerGap) / 2)
                : Math.min(150, Math.max(104, contentW / 4));
        int closeW = narrow ? Math.max(1, contentW - clearW - footerGap)
                : Math.min(104, Math.max(76, contentW / 5));
        int clearX = layout.left() + layout.inset();
        int closeX = layout.left() + layout.panelW() - layout.inset() - closeW;
        String clearLabel = clearConfirmationUntil > Util.getMillis()
                ? ChronicleI18n.tr("history.clear.confirm")
                : ChronicleI18n.tr("history.clear");
        clearButton = button(clearLabel, clearX, layout.footerY(), clearW,
                layout.footerH(), b -> clearHistory());
        clearButton.active = ChronicleClient.CONFIG != null
                && ChronicleClient.CONFIG.history != null
                && !ChronicleClient.CONFIG.history.isEmpty();
        closeButton = button(ChronicleI18n.tr("action.done"), closeX, layout.footerY(),
                closeW, layout.footerH(), b -> onClose());
        addRenderableWidget(clearButton);
        addRenderableWidget(closeButton);
    }

    private HistoryLayout layout() {
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        int bottom = Math.max(top + 1, this.height - 12);
        int inset = UiMetrics.contentInset(panelW);
        boolean compact = panelW < 520 || this.height < 360;
        int headerH = UiMetrics.headerHeight(compact);
        int filterColumns = panelW < 520 ? 2 : 4;
        int filterH = compact ? 24 : UiMetrics.CONTROL_HEIGHT;
        int filterGap = compact ? UiMetrics.GAP_XS : UiMetrics.GAP_SM;
        int filterY = top + headerH + (compact ? UiMetrics.GAP_XS : UiMetrics.GAP_SM);
        int filterRows = (Filter.values().length + filterColumns - 1) / filterColumns;
        int filtersBottom = filterY + filterRows * filterH + (filterRows - 1) * filterGap;
        int footerH = compact ? UiMetrics.COMPACT_CONTROL_HEIGHT : UiMetrics.PRIMARY_BUTTON_HEIGHT;
        int footerInset = compact ? UiMetrics.GAP_SM : UiMetrics.GAP_MD;
        int footerTop = Math.max(filtersBottom, bottom - footerH - footerInset * 2);
        int errorStrip = saveError == null ? 0 : this.font.lineHeight + UiMetrics.GAP_SM;
        int listTop = Math.min(footerTop, filtersBottom + (compact ? UiMetrics.GAP_SM : UiMetrics.GAP_MD));
        int listBottom = Math.max(listTop, footerTop - UiMetrics.GAP_SM - errorStrip);
        int rowHeight = compact ? 50 : 52;
        int rowStep = rowHeight + UiMetrics.GAP_SM;
        int footerY = Math.max(0, Math.min(bottom - footerH, footerTop + footerInset));
        return new HistoryLayout(panelW, left, top, bottom, inset,
                filterY, filterH, filterGap, filterColumns,
                listTop, listBottom, rowHeight, rowStep,
                footerTop, footerY, footerH);
    }

    private int visibleRows(HistoryLayout layout) {
        int available = Math.max(0, layout.listBottom() - layout.listTop());
        if (available < layout.rowHeight()) return 0;
        return 1 + (available - layout.rowHeight()) / layout.rowStep();
    }

    private List<ReminderHistoryEntry> filteredEntries() {
        List<ReminderHistoryEntry> result = new ArrayList<>();
        if (ChronicleClient.CONFIG == null || ChronicleClient.CONFIG.history == null) return result;
        ReminderHistoryEntry.Status required = statusForFilter(filter);
        for (ReminderHistoryEntry entry : ChronicleClient.CONFIG.history) {
            if (entry != null && (required == null || entry.status == required)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static ReminderHistoryEntry.Status statusForFilter(Filter value) {
        return switch (value) {
            case MISSED -> ReminderHistoryEntry.Status.MISSED;
            case COMPLETED -> ReminderHistoryEntry.Status.COMPLETED;
            case SNOOZED -> ReminderHistoryEntry.Status.SNOOZED;
            default -> null;
        };
    }

    private static String filterLabel(Filter value) {
        return ChronicleI18n.tr("history.filter." + value.name().toLowerCase(Locale.ROOT));
    }

    private void selectFilter(Filter value) {
        filter = value == null ? Filter.ALL : value;
        scrollOffset = 0;
        clearConfirmationUntil = 0L;
        init(this.width, this.height);
    }

    private void clearHistory() {
        long now = Util.getMillis();
        if (clearConfirmationUntil <= now) {
            clearConfirmationUntil = now + 3_000L;
            clearButton.setMessage(ChronicleI18n.component("history.clear.confirm"));
            return;
        }
        clearConfirmationUntil = 0L;
        if (!ChronicleClient.clearReminderHistory()) {
            saveError = ChronicleClient.CONFIG == null
                    ? ChronicleI18n.tr("error.config_path")
                    : ChronicleClient.CONFIG.getLastSaveError();
        } else {
            saveError = null;
            scrollOffset = 0;
        }
        init(this.width, this.height);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        HistoryLayout layout = layout();
        List<ReminderHistoryEntry> entries = filteredEntries();
        int maxScroll = Math.max(0, entries.size() - visibleRows(layout));
        boolean inside = mouseX >= layout.left() && mouseX < layout.left() + layout.panelW()
                && mouseY >= layout.listTop() && mouseY < layout.listBottom();
        if (inside && maxScroll > 0 && Math.abs(verticalAmount) >= 1.0E-9) {
            scrollOffset = verticalAmount < 0
                    ? Math.min(maxScroll, scrollOffset + 1)
                    : Math.max(0, scrollOffset - 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        long now = Util.getMillis();
        transition.begin(graphics, this.width, this.height);
        HistoryLayout layout = layout();
        List<ReminderHistoryEntry> entries = filteredEntries();
        int visibleRows = visibleRows(layout);
        int maxScroll = Math.max(0, entries.size() - visibleRows);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);
        UiFrame.drawWindow(graphics, layout.left(), layout.top(), layout.panelW(), layout.bottom());
        boolean compact = layout.panelW() < 520 || this.height < 360;
        int headerX = layout.left() + layout.inset();
        int headerW = Math.max(1, layout.panelW() - layout.inset() * 2);
        String title = UiFrame.trimToWidth(this.font, ChronicleI18n.tr("history.title"), headerW);
        graphics.drawString(this.font, Component.literal(title), headerX,
                UiMetrics.headerTitleY(layout.top(), compact), TEXT, true);
        int total = ChronicleClient.CONFIG == null || ChronicleClient.CONFIG.history == null
                ? 0 : ChronicleClient.CONFIG.history.size();
        String count = ChronicleI18n.tr(total == 1
                ? "history.count.one" : "history.count.many", total);
        String subtitle = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("history.subtitle", count), headerW);
        graphics.drawString(this.font, Component.literal(subtitle), headerX,
                UiMetrics.headerSubtitleY(layout.top(), compact), MUTED, false);
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                UiMetrics.headerDividerY(layout.top(), UiMetrics.headerHeight(compact)));
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                layout.footerTop());

        graphics.fill(layout.left() + layout.inset(), layout.listTop() - UiMetrics.GAP_SM,
                layout.left() + layout.panelW() - layout.inset(),
                layout.listBottom(), PANEL_INNER);
        int start = Math.min(scrollOffset, entries.size());
        int end = Math.min(entries.size(), start + visibleRows);
        for (int index = start; index < end; index++) {
            ReminderHistoryEntry entry = entries.get(index);
            int y = layout.listTop() + (index - start) * layout.rowStep();
            drawEntry(graphics, entry, layout.left() + layout.inset(), y,
                    layout.panelW() - layout.inset() * 2, layout.rowHeight());
        }

        if (entries.isEmpty()) {
            int emptyY = layout.listTop() + Math.max(6,
                    (Math.max(1, layout.listBottom() - layout.listTop()) - 30) / 2);
            String emptyTitle = ChronicleI18n.tr(filter == Filter.ALL
                    ? "history.empty" : "history.empty.filtered");
            String emptyHint = ChronicleI18n.tr(filter == Filter.ALL
                    ? "history.empty_hint" : "history.empty.filtered_hint");
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            emptyTitle, headerW - 16)), headerX + 8, emptyY, TEXT, false);
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            emptyHint, headerW - 16)), headerX + 8,
                    emptyY + 18, MUTED, false);
        }

        if (maxScroll > 0) {
            UiFrame.drawScrollBar(graphics,
                    Math.max(layout.left(),
                            layout.left() + layout.panelW() - layout.inset() - 3),
                    layout.listTop() + 4, layout.listBottom() - 4,
                    scrollOffset / (float) maxScroll,
                    visibleRows / (float) Math.max(1, entries.size()));
        }

        if (saveError != null) {
            int errorY = layout.listBottom() + 3;
            String error = UiFrame.trimToWidth(this.font, saveError, headerW);
            graphics.drawString(this.font, Component.literal(error), headerX, errorY, NEGATIVE, false);
        }

        super.render(graphics, mouseX, mouseY, delta);
        for (var child : children()) {
            if (child instanceof Button button) {
                Filter buttonFilter = filterButtons.get(button);
                boolean selected = buttonFilter != null && buttonFilter == filter;
                int accent = button == clearButton ? NEGATIVE : ACCENT;
                boolean emphasized = selected || button == closeButton
                        || button == clearButton && clearConfirmationUntil > now;
                UiFrame.drawButton(graphics, this.font, button, accent,
                        emphasized, mouseX, mouseY);
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

    private void drawEntry(GuiGraphics graphics, ReminderHistoryEntry entry,
                           int x, int y, int width, int height) {
        ReminderHistoryEntry.Status status = entry.status == null
                ? ReminderHistoryEntry.Status.MISSED : entry.status;
        int statusColor = statusColor(status);
        graphics.fill(x, y, x + width, y + height, ROW);
        graphics.fill(x, y, x + 2, y + height, statusColor);
        int textX = x + 10;
        int right = x + width - 10;
        String statusLabel = ChronicleI18n.tr(
                "history.filter." + status.name().toLowerCase(Locale.ROOT));
        boolean narrow = width < 320;
        int messageRight = narrow ? right
                : right - this.font.width(statusLabel) - UiMetrics.GAP_MD;
        String message = UiFrame.trimToWidth(this.font, entry.message,
                Math.max(1, messageRight - textX));
        graphics.drawString(this.font, Component.literal(message), textX, y + 8, TEXT, true);
        String timestamp = formatTimestamp(entry.occurredAtEpochMillis);
        String detail = status == ReminderHistoryEntry.Status.SNOOZED
                ? ChronicleI18n.tr("history.snoozed_for",
                ChronicleClient.formatInterval(entry.snoozeMinutes), timestamp)
                : timestamp;
        if (narrow) {
            String combined = statusLabel + " • " + detail;
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            combined, Math.max(1, right - textX))),
                    textX, y + 29, statusColor, false);
        } else {
            graphics.drawString(this.font, Component.literal(statusLabel),
                    right - this.font.width(statusLabel), y + 8, statusColor, false);
            graphics.drawString(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            detail, Math.max(1, right - textX))),
                    textX, y + 29, MUTED, false);
        }
    }

    private static int statusColor(ReminderHistoryEntry.Status status) {
        return switch (status) {
            case MISSED -> NEGATIVE;
            case COMPLETED -> POSITIVE;
            case SNOOZED -> ACCENT;
        };
    }

    private static String formatTimestamp(long epochMillis) {
        try {
            LocalDateTime time = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            if (ChronicleClient.CONFIG != null && !ChronicleClient.CONFIG.use24HourFormat) {
                int hour = time.getHour() % 12;
                if (hour == 0) hour = 12;
                String period = time.getHour() >= 12 ? "PM" : "AM";
                return String.format(Locale.ROOT, "%04d-%02d-%02d %d:%02d %s",
                        time.getYear(), time.getMonthValue(), time.getDayOfMonth(),
                        hour, time.getMinute(), period);
            }
            return String.format(Locale.ROOT, "%04d-%02d-%02d %02d:%02d",
                    time.getYear(), time.getMonthValue(), time.getDayOfMonth(),
                    time.getHour(), time.getMinute());
        } catch (RuntimeException ignored) {
            return "—";
        }
    }

    @Override
    public void onClose() {
        transition.start(this.minecraft, parent);
    }
}
