package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.config.Reminder;
import com.aspectra00.chronicle.client.config.ReminderConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.time.LocalDateTime;
import java.util.Objects;

public class ReminderConfigScreen extends Screen {
    private static final int PANEL_INNER = 0xFF0C1118;
    private static final int ROW = 0xFF141A22;
    private static final int ROW_HOVER = 0xFF19212B;
    private static final int ROW_DISABLED = 0xFF11161D;
    private static final int ROW_DISABLED_HOVER = 0xFF161C24;
    private static final int BORDER = 0xFF252E3A;
    private static final int TEXT = 0xFFE7ECF2;
    private static final int MUTED = 0xFF8995A4;
    private static final int SUBTLE = 0xFF5F6B79;
    private static final int ACCENT = 0xFF8FB3E8;
    private static final int POSITIVE = 0xFF8FC7AA;
    private static final int NEGATIVE = 0xFFB88189;

    private final Screen parent;
    private int scrollOffset;
    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private String saveError;
    private long lastPressedAt = -1L;
    private int lastPressedX;
    private int lastPressedY;
    private int lastPressedW;
    private int lastPressedH;
    private Reminder pendingRemoval;
    private long pendingRemovalUntil;
    private long configRevision;
    private Button testButton;
    private String lastConfigError;

    public ReminderConfigScreen(Screen parent) {
        super(ChronicleI18n.component("main.title"));
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
        if (transition.tick(this.minecraft)) {
            return;
        }
        String runtimeError = ChronicleClient.getRuntimeConfigError();
        if (!Objects.equals(runtimeError, lastConfigError)) {
            if (saveError == null || Objects.equals(saveError, lastConfigError)) {
                saveError = runtimeError;
            }
            lastConfigError = runtimeError;
        }
        if (pendingRemoval != null && Util.getMillis() >= pendingRemovalUntil) {
            pendingRemoval = null;
            init(this.width, this.height);
        } else if (configRevision != ChronicleClient.CONFIG_REVISION) {
            init(this.width, this.height);
        }
    }

    @Override
    protected void init() {
        clearFocus();
        setDragging(false);
        clearWidgets();
        configRevision = ChronicleClient.CONFIG_REVISION;

        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int panelTop = UiMetrics.panelTop(this.height);
        int contentInset = UiMetrics.contentInset(panelW);
        boolean tiny = this.width < 380 || this.height < 260;
        boolean compact = panelW < 760 || tiny;

        int footerHeight = compact ? 82 : UiMetrics.FOOTER_AREA_HEIGHT;
        int footerTop = Math.max(0, this.height - footerHeight);
        int headerHeight = UiMetrics.headerHeight(tiny);
        int listTop = Math.min(panelTop + headerHeight + (tiny ? UiMetrics.GAP_MD : 16), footerTop);
        int rowH = compact ? 76 : 60;
        int listBottom = Math.max(listTop, footerTop - 12);
        int visibleRows = Math.max(0, (listBottom - listTop) / rowH);

        int count = ChronicleClient.CONFIG.reminders.size();
        int maxScroll = Math.max(0, count - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int start = scrollOffset;
        int end = Math.min(count, start + visibleRows);

        testButton = null;
        if (panelW >= 220) {
            int testW = Math.min(64, Math.max(48, panelW / 5));
            int testH = 24;
            testButton = button(ChronicleI18n.tr("action.show_toast"),
                    left + panelW - contentInset - testW,
                    panelTop + (tiny ? 9 : 13), testW, testH,
                    b -> ChronicleClient.showTestReminder(this.minecraft));
            addRenderableWidget(testButton);
        }

        for (int index = start; index < end; index++) {
            Reminder reminder = ChronicleClient.CONFIG.reminders.get(index);
            int y = listTop + (index - start) * rowH;

            int availableControls = Math.max(1, panelW - contentInset * 2);
            int compactButtonHeight = compact ? 24 : UiMetrics.CONTROL_HEIGHT;
            int buttonGap = UiMetrics.GAP_SM;
            int compactButtonW = Math.max(1, (availableControls - buttonGap * 2) / 3);
            int buttonY = compact ? y + 40 : y + 11;
            int toggleW = compact ? compactButtonW : 56;
            int editW = compact ? compactButtonW : 60;
            int removeW = compact ? compactButtonW : 100;
            int toggleX = compact ? left + contentInset : left + panelW - 286;
            int editX = compact ? toggleX + toggleW + buttonGap : left + panelW - 216;
            int removeX = compact ? editX + editW + buttonGap : left + panelW - 146;
            String toggleLabel = ChronicleI18n.tr(reminder.enabled ? "action.on" : "action.off");
            String editLabel = ChronicleI18n.tr("action.edit");
            String confirmationLabel = ChronicleI18n.tr(removeW < 55 ? "action.sure" : "action.confirm");
            boolean awaitingConfirmation = pendingRemoval == reminder && Util.getMillis() < pendingRemovalUntil;
            String removeLabel = awaitingConfirmation ? confirmationLabel
                    : ChronicleI18n.tr(tiny ? "action.delete.short" : "action.remove");

            Button toggle = button(
                    toggleLabel,
                    toggleX, buttonY, toggleW, compactButtonHeight,
                    b -> {
                        boolean previousEnabled = reminder.enabled;
                        long previousIntervalEpochMinute = reminder.lastTriggeredEpochMinute;
                        long previousIntervalDeadline = reminder.nextTriggerEpochMillis;
                        reminder.enabled = !previousEnabled;
                        if (reminder.enabled && reminder.scheduleType == Reminder.ScheduleType.INTERVAL) {
                            ChronicleClient.resetIntervalTimer(reminder);
                        }
                        if (!ChronicleClient.CONFIG.save()) {
                            reminder.enabled = previousEnabled;
                            reminder.lastTriggeredEpochMinute = previousIntervalEpochMinute;
                            reminder.nextTriggerEpochMillis = previousIntervalDeadline;
                            saveError = ChronicleClient.CONFIG.getLastSaveError();
                        } else {
                            saveError = null;
                            ChronicleClient.resetTriggerState(reminder);
                        }
                        b.setMessage(ChronicleI18n.component(reminder.enabled ? "action.on" : "action.off"));
                    }
            );
            addRenderableWidget(toggle);

            Button edit = button(editLabel, editX, buttonY, editW, compactButtonHeight,
                    b -> openEditor(reminder, false));
            addRenderableWidget(edit);

            Button remove = button(removeLabel, removeX, buttonY, removeW, compactButtonHeight,
                    b -> {
                        long now = Util.getMillis();
                        if (pendingRemoval != reminder || now >= pendingRemovalUntil) {
                            pendingRemoval = reminder;
                            pendingRemovalUntil = now + 3000L;
                            b.setMessage(Component.literal(confirmationLabel));
                            saveError = null;
                            return;
                        }
                        pendingRemoval = null;
                        int reminderIndex = ChronicleClient.CONFIG.reminders.indexOf(reminder);
                        if (reminderIndex >= 0) {
                            Reminder removed = ChronicleClient.CONFIG.reminders.remove(reminderIndex);
                            if (!ChronicleClient.CONFIG.save()) {
                                int restoreIndex = Math.max(0, Math.min(reminderIndex, ChronicleClient.CONFIG.reminders.size()));
                                ChronicleClient.CONFIG.reminders.add(restoreIndex, removed);
                                saveError = ChronicleClient.CONFIG.getLastSaveError();
                            } else {
                                saveError = null;
                                ChronicleClient.resetTriggerState(removed);
                            }
                            init(this.width, this.height);
                        }
                    });
            addRenderableWidget(remove);
        }

        if (!compact) {
            int gap = 10;
            int available = Math.max(1, panelW - contentInset * 2);
            int buttonW = Math.max(1, (available - gap * 5) / 6);
            int x = left + contentInset;
            int footerY = footerTop + UiMetrics.GAP_MD;
            addRenderableWidget(button("+  " + ChronicleI18n.tr("action.add_reminder"), x, footerY, buttonW, 30,
                    b -> openNewReminder()));
            x += buttonW + gap;
            addRenderableWidget(button(ChronicleI18n.tr("action.watches"), x, footerY, buttonW, 30,
                    b -> openWatches()));
            x += buttonW + gap;
            addRenderableWidget(button(ChronicleI18n.tr("action.history"), x, footerY, buttonW, 30,
                    b -> openHistory()));
            x += buttonW + gap;
            addRenderableWidget(button(ChronicleI18n.tr("action.sound"), x, footerY, buttonW, 30,
                    b -> openSoundSettings()));
            x += buttonW + gap;
            addRenderableWidget(button(ChronicleI18n.tr("action.toast_style"), x, footerY, buttonW, 30,
                    b -> openToastCustomizer()));
            x += buttonW + gap;
            addRenderableWidget(button(ChronicleI18n.tr("action.done"), x, footerY,
                    Math.max(1, left + panelW - contentInset - x), 30, b -> onClose()));
        } else {
            int gap = 8;
            int footerRowH = UiMetrics.COMPACT_CONTROL_HEIGHT;
            int contentW = Math.max(1, panelW - contentInset * 2);
            int topButtonW = Math.max(1, (contentW - gap * 2) / 3);
            int bottomButtonW = Math.max(1, (contentW - gap * 2) / 3);
            int x1 = left + contentInset;
            int x2 = x1 + topButtonW + gap;
            int x3 = x2 + topButtonW + gap;
            int row1 = footerTop + 7;
            int row2 = footerTop + 39;
            addRenderableWidget(button("+ " + ChronicleI18n.tr("action.add"), x1, row1, topButtonW, footerRowH,
                    b -> openNewReminder()));
            addRenderableWidget(button(ChronicleI18n.tr("action.watches"), x2, row1, topButtonW, footerRowH,
                    b -> openWatches()));
            addRenderableWidget(button(ChronicleI18n.tr("action.history"), x3, row1,
                    Math.max(1, left + panelW - contentInset - x3), footerRowH,
                    b -> openHistory()));
            addRenderableWidget(button(ChronicleI18n.tr("action.sound"), x1, row2,
                    bottomButtonW, footerRowH,
                    b -> openSoundSettings()));
            addRenderableWidget(button(ChronicleI18n.tr("action.toast_style"),
                    x1 + bottomButtonW + gap, row2,
                    bottomButtonW, footerRowH,
                    b -> openToastCustomizer()));
            addRenderableWidget(button(ChronicleI18n.tr("action.done"),
                    x1 + (bottomButtonW + gap) * 2, row2,
                    Math.max(1, contentW - bottomButtonW * 2 - gap * 2),
                    footerRowH, b -> onClose()));
        }

    }

    private Button button(String text, int x, int y, int width, int height, Button.OnPress press) {
        return Button.builder(Component.literal(text), b -> {
            if (transition.isClosing()) return;
            markPressed(b);
            press.onPress(b);
        }).bounds(x, y, width, height).build();
    }

    private void markPressed(Button button) {
        lastPressedAt = Util.getMillis();
        lastPressedX = button.getX();
        lastPressedY = button.getY();
        lastPressedW = button.getWidth();
        lastPressedH = button.getHeight();
    }

    private void openEditor(Reminder reminder, boolean isNew) {
        transition.start(this.minecraft, new ReminderEditorScreen(this, reminder, isNew));
    }

    private void openNewReminder() {
        if (ChronicleClient.CONFIG.reminders.size() >= ReminderConfig.MAX_REMINDERS) {
            saveError = ChronicleI18n.tr("error.reminder_limit", ReminderConfig.MAX_REMINDERS);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        openEditor(new Reminder(now.getHour(), now.getMinute(),
                ChronicleI18n.tr("default.new_reminder"), true), true);
    }

    private void openToastCustomizer() {
        transition.start(this.minecraft, new ToastCustomizerScreen(this));
    }

    private void openHistory() {
        transition.start(this.minecraft, new ReminderHistoryScreen(this));
    }

    private void openWatches() {
        transition.start(this.minecraft, new WatchListScreen(this));
    }

    private void openSoundSettings() {
        transition.start(this.minecraft, new NotificationSoundScreen(this));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0 || transition.isClosing()) {
            return false;
        }
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int panelTop = UiMetrics.panelTop(this.height);
        int contentInset = UiMetrics.contentInset(panelW);
        boolean tiny = this.width < 380 || this.height < 260;
        boolean compact = panelW < 760 || tiny;
        int footerTop = Math.max(0, this.height
                - (compact ? 82 : UiMetrics.FOOTER_AREA_HEIGHT));
        int headerHeight = UiMetrics.headerHeight(tiny);
        int listTop = Math.min(panelTop + headerHeight + (tiny ? UiMetrics.GAP_MD : 16), footerTop);
        int listBottom = Math.max(listTop, footerTop - 12);
        int rowH = compact ? 76 : 60;
        int rowHeight = compact ? 68 : 50;
        if (event.x() < left + contentInset || event.x() >= left + panelW - contentInset
                || event.y() < listTop || event.y() >= listBottom) {
            return false;
        }
        int relativeY = (int) event.y() - listTop;
        if (relativeY % rowH >= rowHeight) {
            return false;
        }
        int index = scrollOffset + relativeY / rowH;
        if (index < 0 || index >= ChronicleClient.CONFIG.reminders.size()) {
            return false;
        }
        openEditor(ChronicleClient.CONFIG.reminders.get(index), false);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelW = UiMetrics.panelWidth(this.width);
        boolean tiny = this.width < 380 || this.height < 260;
        boolean compact = panelW < 760 || tiny;
        int panelTop = UiMetrics.panelTop(this.height);
        int footerHeight = compact ? 82 : UiMetrics.FOOTER_AREA_HEIGHT;
        int footerTop = Math.max(0, this.height - footerHeight);
        int headerHeight = UiMetrics.headerHeight(tiny);
        int listTop = Math.min(panelTop + headerHeight + (tiny ? UiMetrics.GAP_MD : 16), footerTop);
        int listBottom = Math.max(listTop, footerTop - 12);
        int rowH = compact ? 76 : 60;
        int visibleRows = Math.max(0, (listBottom - listTop) / rowH);
        int maxScroll = Math.max(0, ChronicleClient.CONFIG.reminders.size() - visibleRows);

        int left = UiMetrics.panelLeft(this.width, panelW);
        if (mouseX >= left && mouseX < left + panelW
                && mouseY >= Math.max(0, listTop - 12) && mouseY < listBottom
                && maxScroll > 0 && Math.abs(verticalAmount) >= 1.0E-9) {
            scrollOffset = verticalAmount < 0
                    ? Math.min(maxScroll, scrollOffset + 1)
                    : Math.max(0, scrollOffset - 1);
            init(this.width, this.height);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = Util.getMillis();
        transition.begin(graphics, this.width, this.height);

        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        boolean tiny = this.width < 380 || this.height < 260;
        boolean compact = panelW < 760 || tiny;
        int footerHeight = compact ? 82 : UiMetrics.FOOTER_AREA_HEIGHT;
        int footerTop = Math.max(0, this.height - footerHeight);
        int panelTop = UiMetrics.panelTop(this.height);
        int panelBottom = Math.max(panelTop + 1, Math.min(this.height, this.height - 12));
        int contentInset = UiMetrics.contentInset(panelW);
        int headerHeight = UiMetrics.headerHeight(tiny);
        int listTop = Math.min(panelTop + headerHeight + (tiny ? UiMetrics.GAP_MD : 16), footerTop);
        int listBottom = Math.max(listTop, footerTop - 12);
        int rowH = compact ? 76 : 60;

        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);

        UiFrame.drawWindow(graphics, left, panelTop, panelW, panelBottom);
        UiFrame.drawInsetDivider(graphics, left, panelW, contentInset,
                UiMetrics.headerDividerY(panelTop, headerHeight));
        UiFrame.drawInsetDivider(graphics, left, panelW, contentInset, footerTop);

        int headerTextX = left + contentInset;
        int headerTextRight = testButton == null
                ? left + panelW - contentInset
                : testButton.getX() - UiMetrics.GAP_SM;
        int headerTextWidth = Math.max(1, headerTextRight - headerTextX);
        String headerTitle = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("main.title"), headerTextWidth);
        graphics.text(this.font, Component.literal(headerTitle),
                headerTextX, UiMetrics.headerTitleY(panelTop, tiny), TEXT, true);
        String timeMode = ChronicleI18n.tr(ChronicleClient.CONFIG.use24HourFormat
                ? "time.mode.24" : "time.mode.12");
        int reminderCount = ChronicleClient.CONFIG.reminders.size();
        String countLabel = ChronicleI18n.tr(reminderCount == 1
                ? "main.reminder_count.one" : "main.reminder_count.many", reminderCount);
        String subtitle = trimToWidth(ChronicleI18n.tr("main.subtitle", timeMode, countLabel),
                headerTextWidth);
        graphics.text(this.font, Component.literal(subtitle),
                headerTextX, UiMetrics.headerSubtitleY(panelTop, tiny), MUTED, false);

        int listSurfaceInset = Math.max(2, contentInset - 6);
        graphics.fill(left + listSurfaceInset, listTop - 10,
                left + panelW - listSurfaceInset, listBottom, PANEL_INNER);

        int start = Math.min(scrollOffset, ChronicleClient.CONFIG.reminders.size());
        int visibleRows = Math.max(0, (listBottom - listTop) / rowH);
        int end = Math.min(ChronicleClient.CONFIG.reminders.size(), start + visibleRows);

        for (int index = start; index < end; index++) {
            Reminder reminder = ChronicleClient.CONFIG.reminders.get(index);
            int y = listTop + (index - start) * rowH;
            int rowHeight = compact ? 68 : 50;

            int rowLeft = left + contentInset;
            int rowRight = left + panelW - contentInset;
            boolean rowHovered = mouseX >= rowLeft && mouseX < rowRight
                    && mouseY >= y && mouseY < y + rowHeight;
            graphics.fill(rowLeft, y, rowRight, y + rowHeight,
                    reminder.enabled
                            ? rowHovered ? ROW_HOVER : ROW
                            : rowHovered ? ROW_DISABLED_HOVER : ROW_DISABLED);
            graphics.fill(rowLeft, y, rowLeft + 2, y + rowHeight,
                    reminder.enabled ? ACCENT : BORDER);

            Reminder.ScheduleType type = reminder.scheduleType == null
                    ? Reminder.ScheduleType.DAILY : reminder.scheduleType;
            String time = switch (type) {
                case INTERVAL -> "↻";
                case TRIGGER -> ChronicleI18n.tr("summary.trigger.badge");
                default -> ChronicleClient.displayTime(reminder, ChronicleClient.CONFIG.use24HourFormat);
            };
            boolean stackedText = rowRight - rowLeft < 150;
            int timeY = y + (stackedText ? 3 : 8);
            graphics.text(this.font, Component.literal(time), rowLeft + 10, timeY,
                    reminder.enabled ? TEXT : MUTED, true);

            int messageX = stackedText ? rowLeft + 10 : rowLeft + 80;
            int messageRight = stackedText
                    ? rowRight - 10
                    : compact ? rowRight - UiMetrics.GAP_SM : left + panelW - 314;
            int messageWidth = Math.max(1, messageRight - messageX);
            String message = trimToWidth(reminder.message, messageWidth);
            graphics.text(this.font, Component.literal(message),
                    messageX, y + (stackedText ? 16 : 8), TEXT, true);

            String summary = trimToWidth(
                    ChronicleClient.scheduleSummary(reminder, ChronicleClient.CONFIG.use24HourFormat),
                    messageWidth
            );
            graphics.text(this.font, Component.literal(summary),
                    messageX, y + (stackedText ? 29 : 26), reminder.enabled ? POSITIVE : SUBTLE, false);
        }

        if (ChronicleClient.CONFIG.reminders.isEmpty()) {
            int emptyY = listTop + Math.max(12, (Math.max(1, listBottom - listTop) - 30) / 2);
            int emptyWidth = Math.max(1, panelW - contentInset * 2 - 20);
            String emptyTitle = trimToWidth(ChronicleI18n.tr("main.empty"), emptyWidth);
            graphics.text(this.font, Component.literal(emptyTitle),
                    left + contentInset + 10, emptyY, TEXT, false);
            String emptyHint = trimToWidth(ChronicleI18n.tr("main.empty_hint"),
                    emptyWidth);
            graphics.text(this.font, Component.literal(emptyHint),
                    left + contentInset + 10, emptyY + 18, MUTED, false);
        }

        if (saveError != null) {
            int errorW = Math.max(1, Math.min(Math.max(1, panelW - contentInset * 2),
                    this.font.width(saveError) + 16));
            int renderedRows = Math.max(0, end - start);
            int lastRowBottom = renderedRows == 0 ? listTop - 2
                    : listTop + (renderedRows - 1) * rowH + (compact ? 68 : 50);
            int preferredTop = footerTop - this.font.lineHeight - 8;
            int errorTop = Math.max(lastRowBottom + 2, preferredTop);
            errorTop = Math.min(errorTop, Math.max(panelTop + 56, preferredTop));
            int errorY = errorTop + 4;
            String shownError = trimToWidth(saveError, Math.max(1, errorW - 16));
            int errorX = left + contentInset;
            graphics.fill(errorX, errorY - 4, errorX + errorW,
                    errorY + this.font.lineHeight + 4, 0xFF2A1C20);
            graphics.text(this.font, Component.literal(shownError), errorX + 8, errorY, NEGATIVE, false);
        }

        int maxScroll = Math.max(0, ChronicleClient.CONFIG.reminders.size() - visibleRows);
        if (maxScroll > 0) {
            float progress = scrollOffset / (float) maxScroll;
            float visibleFraction = visibleRows / (float) Math.max(1, ChronicleClient.CONFIG.reminders.size());
            UiFrame.drawScrollBar(graphics, left + panelW - Math.max(4, contentInset - 4),
                    listTop + 4, listBottom - 4,
                    progress, visibleFraction);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        for (var child : this.children()) {
            if (!(child instanceof Button button)) {
                continue;
            }
            String label = button.getMessage().getString();
            boolean isOn = label.equals(ChronicleI18n.tr("action.on"));
            boolean destructive = label.equals(ChronicleI18n.tr("action.remove"))
                    || label.equals(ChronicleI18n.tr("action.delete.short"))
                    || label.equals(ChronicleI18n.tr("action.confirm"))
                    || label.equals(ChronicleI18n.tr("action.sure"));
            int accent = isOn ? POSITIVE : (destructive ? NEGATIVE : ACCENT);
            boolean emphasized = isOn || label.equals(ChronicleI18n.tr("action.confirm"))
                    || label.equals(ChronicleI18n.tr("action.sure"))
                    || label.startsWith("+") || label.equals(ChronicleI18n.tr("action.done"));
            UiFrame.drawButton(graphics, this.font, button, accent, emphasized, mouseX, mouseY);
        }

        if (lastPressedAt >= 0) {
            float pulse = UiAnimation.pressProgress(lastPressedAt, now, 170L);
            if (pulse > 0.0f) {
                int inset = UiAnimation.pressInset(pulse);
                int alpha = Math.round(pulse * 58.0f);
                graphics.fill(lastPressedX - inset, lastPressedY - inset,
                        lastPressedX + lastPressedW + inset, lastPressedY + lastPressedH + inset,
                        (alpha << 24) | 0x008FB3E8);
            }
        }

        transition.end(graphics, this.width, this.height);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        if (this.font.width(ellipsis) > maxWidth) {
            int end = text.length();
            while (end > 0 && this.font.width(text.substring(0, end)) > maxWidth) {
                end = text.offsetByCodePoints(end, -1);
            }
            return text.substring(0, end);
        }
        int target = Math.max(0, maxWidth - this.font.width(ellipsis));
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) > target) {
            end = text.offsetByCodePoints(end, -1);
        }
        return text.substring(0, end) + ellipsis;
    }

    @Override
    public void onClose() {
        transition.start(this.minecraft, parent);
    }
}
