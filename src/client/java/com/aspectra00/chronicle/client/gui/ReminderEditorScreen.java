package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.config.Reminder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unified minimal editor for Daily / Weekly / Interval reminders.
 * Widgets stay at their logical coordinates; only the visual layer animates.
 */
public final class ReminderEditorScreen extends Screen {
    private static final int FIELD = 0xFF0F141A;
    private static final int BORDER = 0xFF252E3A;
    private static final int HOVER = 0xFF3A4655;
    private static final int ACCENT = 0xFF8FB3E8;
    private static final int TEXT = 0xFFE7ECF2;
    private static final int MUTED = 0xFF8995A4;
    private static final int ERROR = 0xFFD69A9A;

    private static final String[] DAY_KEYS = {
            "day.mon.short", "day.tue.short", "day.wed.short", "day.thu.short",
            "day.fri.short", "day.sat.short", "day.sun.short"
    };

    private final Screen parent;
    private final Reminder original;
    private final Reminder draft;
    private final boolean isNew;

    private Reminder.ScheduleType scheduleType;
    private boolean use24HourFormat;
    private boolean isPm;
    private boolean intervalUsesHours;
    private Reminder.AfterTriggerAction afterTriggerAction;
    private final boolean[] weeklyDays = new boolean[7];

    private EditBox hourBox;
    private EditBox minuteBox;
    private EditBox intervalBox;
    private VerticallyCenteredEditBox messageBox;

    private Button dailyButton;
    private Button weeklyButton;
    private Button intervalButton;
    private Button timeFormatButton;
    private Button periodButton;
    private Button intervalUnitButton;
    private Button afterTriggerButton;
    private final List<Button> dayButtons = new ArrayList<>();
    private Button saveButton;
    private Button cancelButton;

    private String hourText;
    private String minuteText;
    private String intervalText;
    private String messageText;

    private int scrollOffset;
    private int maxScroll;
    private String validationError;

    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private long pressedAt = -1L;
    private int pressedX, pressedY, pressedW, pressedH;
    private Button pressedButton;

    public ReminderEditorScreen(Screen parent, Reminder reminder, boolean isNew) {
        super(ChronicleI18n.component(isNew ? "editor.title.add" : "editor.title.edit"));
        this.parent = parent;
        this.isNew = isNew;
        this.original = isNew ? null : reminder;
        this.draft = reminder.copy();

        this.scheduleType = draft.scheduleType == null ? Reminder.ScheduleType.DAILY : draft.scheduleType;
        this.use24HourFormat = ChronicleClient.CONFIG.use24HourFormat;
        this.isPm = draft.hour >= 12;
        this.intervalUsesHours = draft.intervalMinutes >= 60 && draft.intervalMinutes % 60 == 0;
        this.afterTriggerAction = draft.afterTriggerAction == null
                ? Reminder.AfterTriggerAction.KEEP : draft.afterTriggerAction;

        if (draft.weeklyDays == null) {
            Arrays.fill(weeklyDays, true);
        } else {
            for (int i = 0; i < 7; i++) {
                weeklyDays[i] = i < draft.weeklyDays.length && draft.weeklyDays[i];
            }
        }

        if (!hasAnyWeeklyDay()) {
            Arrays.fill(weeklyDays, true);
        }

        this.hourText = displayHour();
        this.minuteText = String.format("%02d", draft.minute);
        this.intervalText = intervalUsesHours
                ? Integer.toString(Math.max(1, draft.intervalMinutes / 60))
                : Integer.toString(Math.max(1, draft.intervalMinutes));
        this.messageText = draft.message == null || draft.message.isBlank()
                ? ChronicleI18n.tr("default.reminder") : draft.message;
    }

    @Override
    public void added() {
        super.added();
        transition.added();
    }

    @Override
    public void tick() {
        super.tick();
        transition.tick(this.minecraft);
    }

    @Override
    protected void init() {
        rebuildWidgetsInternal();
    }

    @Override
    protected void rebuildWidgets() {
        // Screen.resize()/clearAndInit() rebuilds widgets after a GUI-scale or
        // fullscreen change. clearWidgets() removes the children but does not
        // clear Screen's focused-child reference, so doing a blind rebuild can
        // leave keyboard input attached to a detached EditBox. Preserve the
        // logical field, clear stale focus/dragging, rebuild, then restore it.
        captureValues();
        String focusedField = focusedFieldKey();
        clearFocus();
        setDragging(false);
        rebuildWidgetsInternal();
        restoreFocusedField(focusedField);
    }

    @Override
    public void resize(int width, int height) {
        // Capture live text before Minecraft recreates the widgets. This is
        // especially important while typing when the player toggles fullscreen.
        captureValues();
        pressedAt = -1L;
        clearFocus();
        setDragging(false);
        super.resize(width, height);
    }

    private String focusedFieldKey() {
        GuiEventListener focused = getFocused();
        if (focused == hourBox) return "hour";
        if (focused == minuteBox) return "minute";
        if (focused == intervalBox) return "interval";
        if (focused == messageBox) return "message";
        return null;
    }

    private void restoreFocusedField(String key) {
        if (key == null) return;
        EditBox target = switch (key) {
            case "hour" -> hourBox;
            case "minute" -> minuteBox;
            case "interval" -> intervalBox;
            case "message" -> messageBox;
            default -> null;
        };
        if (target != null && isInContentViewport(target)) {
            setFocused(target);
            target.setFocused(true);
        }
    }

    private boolean isInContentViewport(net.minecraft.client.gui.components.AbstractWidget widget) {
        int panelTop = UiMetrics.panelTop(this.height);
        int clipTop = panelTop + UiMetrics.HEADER_HEIGHT;
        int clipBottom = saveButton == null ? this.height - 64 : saveButton.getY() - footerContentGap();
        clipBottom = Math.max(clipTop + 20, clipBottom);
        return widget.getY() + widget.getHeight() > clipTop && widget.getY() < clipBottom;
    }

    private void rebuildWidgetsInternal() {
        clearFocus();
        setDragging(false);
        clearWidgets();
        dayButtons.clear();

        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int panelTop = UiMetrics.panelTop(this.height);
        boolean compact = panelW < 600;
        int horizontalPad = UiMetrics.contentInset(panelW);
        int innerLeft = left + horizontalPad;
        int innerRight = Math.max(innerLeft, left + panelW - horizontalPad);
        int innerW = Math.max(1, innerRight - innerLeft);

        int scheduleY = panelTop + UiMetrics.HEADER_HEIGHT
                + UiMetrics.GAP_SM + UiMetrics.LABEL_OFFSET;
        int modeGap = UiMetrics.GAP_SM;
        // Three 64px actions plus the shared 8px gaps fit at Minecraft's
        // supported 320x240 logical floor. Stacking them at 248px inner width
        // pushed INTERVAL below the viewport and left only seven pixels visible.
        boolean stackedModes = innerW < 3 * 64 + modeGap * 2;
        int modeW = stackedModes ? innerW : Math.max(44, (innerW - modeGap * 2) / 3);
        int modeX = innerLeft;

        dailyButton = uiButton(ChronicleI18n.tr("schedule.daily"), modeX, scheduleY, modeW, 30,
                () -> switchSchedule(Reminder.ScheduleType.DAILY));
        addRenderableWidget(dailyButton);
        if (stackedModes) {
            weeklyButton = uiButton(ChronicleI18n.tr("schedule.weekly"), modeX,
                    scheduleY + UiMetrics.PRIMARY_BUTTON_HEIGHT + modeGap, modeW, 30,
                    () -> switchSchedule(Reminder.ScheduleType.WEEKLY));
            intervalButton = uiButton(ChronicleI18n.tr("schedule.interval"), modeX,
                    scheduleY + (UiMetrics.PRIMARY_BUTTON_HEIGHT + modeGap) * 2, modeW, 30,
                    () -> switchSchedule(Reminder.ScheduleType.INTERVAL));
        } else {
            weeklyButton = uiButton(ChronicleI18n.tr("schedule.weekly"), modeX + modeW + modeGap, scheduleY, modeW, 30,
                    () -> switchSchedule(Reminder.ScheduleType.WEEKLY));
            intervalButton = uiButton(ChronicleI18n.tr("schedule.interval"), modeX + (modeW + modeGap) * 2, scheduleY, modeW, 30,
                    () -> switchSchedule(Reminder.ScheduleType.INTERVAL));
        }
        addRenderableWidget(weeklyButton);
        addRenderableWidget(intervalButton);

        int modeBottom = scheduleY + (stackedModes ? (UiMetrics.PRIMARY_BUTTON_HEIGHT + modeGap) * 2 : 0)
                + UiMetrics.PRIMARY_BUTTON_HEIGHT;
        // Section labels are rendered LABEL_OFFSET pixels above their first control.
        int cursorY = modeBottom + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;

        if (scheduleType == Reminder.ScheduleType.INTERVAL) {
            boolean stackedInterval = innerW < 150;
            int intervalW = stackedInterval ? innerW : Math.min(120, Math.max(1, (innerW - UiMetrics.GAP_SM) / 2));
            intervalBox = editBox(innerLeft, cursorY, Math.max(1, intervalW), UiMetrics.CONTROL_HEIGHT,
                    intervalText, 6, "editor.field.interval");
            addRenderableWidget(intervalBox);
            if (stackedInterval) {
                intervalUnitButton = uiButton(ChronicleI18n.tr(intervalUsesHours ? "unit.hours" : "unit.minutes"),
                        innerLeft, cursorY + UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM,
                        innerW, UiMetrics.CONTROL_HEIGHT, this::toggleIntervalUnit);
                addRenderableWidget(intervalUnitButton);
                cursorY = intervalUnitButton.getY() + intervalUnitButton.getHeight();
            } else {
                int unitX = intervalBox.getX() + intervalBox.getWidth() + UiMetrics.GAP_SM;
                int unitW = Math.max(1, innerW - intervalW - UiMetrics.GAP_SM);
                intervalUnitButton = uiButton(ChronicleI18n.tr(intervalUsesHours ? "unit.hours" : "unit.minutes"),
                        Math.min(unitX, innerRight - unitW), cursorY, unitW,
                        UiMetrics.CONTROL_HEIGHT, this::toggleIntervalUnit);
                addRenderableWidget(intervalUnitButton);
                cursorY += UiMetrics.CONTROL_HEIGHT;
            }
            hourBox = null;
            minuteBox = null;
            timeFormatButton = null;
            periodButton = null;
        } else {
            int gap = UiMetrics.GAP_SM;
            // 64 + 8 + 64 + 8 + 76 fits comfortably in the 248px inner
            // width at 320x240, so keep time and format on one scan line there.
            boolean stackedTime = innerW < 220;
            boolean verticalTime = innerW < 104;
            int timeW = verticalTime
                    ? innerW
                    : stackedTime
                    ? Math.max(1, Math.min(64, Math.max(1, (innerW - gap) / 2)))
                    : Math.max(1, Math.min(compact ? 64 : 82, Math.max(1, (innerW - gap * 2) / 3)));
            int x = innerLeft;
            hourBox = editBox(x, cursorY, timeW, UiMetrics.CONTROL_HEIGHT,
                    hourText, 2, "editor.field.hour");
            addRenderableWidget(hourBox);
            int timeControlsBottom = cursorY + UiMetrics.CONTROL_HEIGHT;
            if (verticalTime) {
                minuteBox = editBox(x, cursorY + UiMetrics.CONTROL_HEIGHT + gap,
                        timeW, UiMetrics.CONTROL_HEIGHT, minuteText, 2, "editor.field.minute");
                addRenderableWidget(minuteBox);
                timeControlsBottom = minuteBox.getY() + minuteBox.getHeight();
            } else {
                minuteBox = editBox(x + timeW + gap, cursorY, timeW, UiMetrics.CONTROL_HEIGHT,
                        minuteText, 2, "editor.field.minute");
                addRenderableWidget(minuteBox);
            }

            if (stackedTime) {
                int nextY = timeControlsBottom + UiMetrics.GAP_SM;
                int formatW = innerW;
                timeFormatButton = uiButton(ChronicleI18n.tr(use24HourFormat ? "time.mode.24" : "time.mode.12"),
                        innerLeft, nextY, formatW, UiMetrics.CONTROL_HEIGHT, this::toggleTimeFormat);
                addRenderableWidget(timeFormatButton);
                timeControlsBottom = nextY + UiMetrics.CONTROL_HEIGHT;
                periodButton = null;
                if (!use24HourFormat) {
                    int periodY = timeControlsBottom + UiMetrics.GAP_SM;
                    int periodW = Math.min(54, Math.max(1, innerW));
                    periodButton = uiButton(isPm ? "PM" : "AM", innerLeft, periodY,
                            periodW, UiMetrics.CONTROL_HEIGHT, this::togglePeriod);
                    addRenderableWidget(periodButton);
                    timeControlsBottom = periodY + UiMetrics.CONTROL_HEIGHT;
                }
            } else {
                int formatX = x + (timeW + gap) * 2;
                int formatW = Math.max(1, Math.min(compact ? 94 : 126, innerRight - formatX));
                timeFormatButton = uiButton(ChronicleI18n.tr(use24HourFormat ? "time.mode.24" : "time.mode.12"),
                        Math.min(formatX, Math.max(innerLeft, innerRight - formatW)), cursorY,
                        formatW, UiMetrics.CONTROL_HEIGHT, this::toggleTimeFormat);
                addRenderableWidget(timeFormatButton);
                periodButton = null;
                if (!use24HourFormat) {
                    int periodX = formatX + formatW + UiMetrics.GAP_SM;
                    if (periodX + 44 <= innerRight) {
                        periodButton = uiButton(isPm ? "PM" : "AM", periodX, cursorY,
                                Math.min(54, innerRight - periodX), UiMetrics.CONTROL_HEIGHT, this::togglePeriod);
                        addRenderableWidget(periodButton);
                    } else {
                        int secondY = timeControlsBottom + UiMetrics.GAP_SM;
                        periodButton = uiButton(isPm ? "PM" : "AM", innerLeft, secondY,
                                Math.min(54, innerW), UiMetrics.CONTROL_HEIGHT, this::togglePeriod);
                        addRenderableWidget(periodButton);
                        timeControlsBottom = secondY + UiMetrics.CONTROL_HEIGHT;
                    }
                }
            }
            cursorY = timeControlsBottom;
        }

        if (scheduleType == Reminder.ScheduleType.WEEKLY) {
            int dayY = cursorY + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;
            int gap = UiMetrics.GAP_SM;
            int columns;
            if (innerW < 120) columns = 1;
            else if (innerW < 208) columns = 2;
            else if (innerW < 320) columns = 3;
            else columns = 4;
            int dayW = Math.max(1, (innerW - gap * (columns - 1)) / columns);
            int dayStep = UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM;
            for (int i = 0; i < 7; i++) {
                int row = i / columns;
                int col = i % columns;
                int x = innerLeft + col * (dayW + gap);
                int y = dayY + row * dayStep;
                final int day = i;
                Button b = uiButton(ChronicleI18n.tr(DAY_KEYS[i]), x, y, dayW, UiMetrics.CONTROL_HEIGHT, () -> {
                    weeklyDays[day] = !weeklyDays[day];
                    validationError = null;
                });
                addRenderableWidget(b);
                dayButtons.add(b);
            }
            int rows = (7 + columns - 1) / columns;
            cursorY = dayY + (rows - 1) * dayStep + UiMetrics.CONTROL_HEIGHT;
        }

        int messageY = cursorY + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;
        messageBox = editBox(innerLeft, messageY, innerW, 32,
                messageText, 80, "editor.field.message");
        // A sentence is easier to scan and edit from a stable left edge. Keep
        // the same four-pixel content inset used by the custom-sound path.
        messageBox.setCentered(false);
        messageBox.setHorizontalPadding(UiMetrics.GAP_XS);
        addRenderableWidget(messageBox);
        int afterY = messageY + messageBox.getHeight() + this.font.lineHeight
                + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;
        afterTriggerButton = uiButton(afterTriggerLabel(), innerLeft, afterY, innerW,
                UiMetrics.CONTROL_HEIGHT, this::cycleAfterTrigger);
        addRenderableWidget(afterTriggerButton);
        cursorY = afterY + afterTriggerButton.getHeight() + 18;

        int panelBottom = Math.max(panelTop + 1, panelBottomForLayout());
        panelBottom = Math.min(this.height, panelBottom);
        int footerInset = 12;
        boolean stackedFooter = panelW < 190;
        int footerHeight = stackedFooter
                ? UiMetrics.PRIMARY_BUTTON_HEIGHT * 2 + UiMetrics.GAP_SM
                : UiMetrics.PRIMARY_BUTTON_HEIGHT;
        int visibleFooterY = Math.max(panelTop + 40, panelBottom - footerInset - footerHeight);
        int contentViewportBottom = Math.max(panelTop + UiMetrics.HEADER_HEIGHT + UiMetrics.GAP_XS,
                visibleFooterY - footerContentGap());
        maxScroll = Math.max(0, cursorY - contentViewportBottom);
        int buttonGap = UiMetrics.GAP_SM;
        int footerAvailable = innerW;
        int buttonW = stackedFooter
                ? Math.max(1, Math.min(120, footerAvailable))
                : Math.max(1, Math.min(120, Math.max(1, (footerAvailable - buttonGap) / 2)));
        int footerCenterX = left + Math.max(0, (panelW - buttonW) / 2);
        int footerRight = innerRight;
        int cancelX = stackedFooter ? footerCenterX : Math.max(innerLeft, footerRight - buttonW);
        int saveX = stackedFooter ? footerCenterX : Math.max(innerLeft, cancelX - buttonGap - buttonW);
        saveButton = uiButton(ChronicleI18n.tr("action.save"), saveX, visibleFooterY, buttonW,
                UiMetrics.PRIMARY_BUTTON_HEIGHT, this::saveAndClose);
        cancelButton = uiButton(ChronicleI18n.tr("action.cancel"), cancelX,
                stackedFooter ? visibleFooterY + UiMetrics.PRIMARY_BUTTON_HEIGHT + UiMetrics.GAP_SM : visibleFooterY,
                buttonW, UiMetrics.PRIMARY_BUTTON_HEIGHT, this::onClose);
        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);

        if (maxScroll == 0) {
            scrollOffset = 0;
        } else {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
            if (scrollOffset > 0) {
                shiftNonFooterWidgets(-scrollOffset);
            }
        }
    }

    private void togglePeriod() {
        captureValues();
        isPm = !isPm;
        if (periodButton != null) {
            periodButton.setMessage(Component.literal(isPm ? "PM" : "AM"));
        }
        validationError = null;
    }

    private void cycleAfterTrigger() {
        afterTriggerAction = switch (afterTriggerAction == null
                ? Reminder.AfterTriggerAction.KEEP : afterTriggerAction) {
            case KEEP -> Reminder.AfterTriggerAction.DISABLE;
            case DISABLE -> Reminder.AfterTriggerAction.DELETE;
            case DELETE -> Reminder.AfterTriggerAction.KEEP;
        };
        if (afterTriggerButton != null) {
            afterTriggerButton.setMessage(Component.literal(afterTriggerLabel()));
        }
        validationError = null;
    }

    private String afterTriggerLabel() {
        Reminder.AfterTriggerAction safe = afterTriggerAction == null
                ? Reminder.AfterTriggerAction.KEEP : afterTriggerAction;
        return ChronicleI18n.tr("editor.after.value",
                ChronicleI18n.tr("editor.after." + safe.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private int panelBottomForLayout() {
        return this.height - 12;
    }

    private int footerContentGap() {
        return validationError == null ? UiMetrics.GAP_MD : 42;
    }

    // The label positions are recalculated inside extractRenderState from the same layout values,
    // avoiding labels becoming detached during scrolling.
    private void shiftNonFooterWidgets(int delta) {
        for (var child : children()) {
            if (child instanceof Button button && (button == saveButton || button == cancelButton)) {
                continue;
            }
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                widget.setY(widget.getY() + delta);
            }
        }
    }

    private VerticallyCenteredEditBox editBox(int x, int y, int width, int height,
                                               String value, int maxLength, String narrationKey) {
        VerticallyCenteredEditBox box = new VerticallyCenteredEditBox(this.font, x, y, width, height,
                ChronicleI18n.component(narrationKey));
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setBordered(false);
        box.setCentered(true);
        box.setTextColor(0xFFE7ECF2);
        box.setTextShadow(false);
        return box;
    }

    private Button uiButton(String label, int x, int y, int width, int height, Runnable action) {
        return Button.builder(Component.literal(label), button -> {
            if (transition.isClosing()) return;
            pressedAt = Util.getMillis();
            pressedX = button.getX();
            pressedY = button.getY();
            pressedW = button.getWidth();
            pressedH = button.getHeight();
            pressedButton = button;
            action.run();
        }).bounds(x, y, width, height).build();
    }

    private void switchSchedule(Reminder.ScheduleType type) {
        captureValues();
        scheduleType = type;
        scrollOffset = 0;
        validationError = null;
        if (scheduleType == Reminder.ScheduleType.WEEKLY && !hasAnyWeeklyDay()) {
            Arrays.fill(weeklyDays, true);
        }
        rebuildWidgetsInternal();
    }

    private void captureValues() {
        if (hourBox != null) hourText = hourBox.getValue();
        if (minuteBox != null) minuteText = minuteBox.getValue();
        if (intervalBox != null) intervalText = intervalBox.getValue();
        if (messageBox != null) messageText = messageBox.getValue();
    }

    private String displayHour() {
        int h24 = Math.max(0, Math.min(23, draft.hour));
        isPm = h24 >= 12;
        if (use24HourFormat) {
            return String.format("%02d", h24);
        }
        int h = h24 % 12;
        if (h == 0) h = 12;
        return String.format("%02d", h);
    }

    private void toggleTimeFormat() {
        captureValues();
        try {
            int shownHour = Integer.parseInt(hourText.trim());
            int minute = Integer.parseInt(minuteText.trim());
            if (minute < 0 || minute > 59) {
                validationError = ChronicleI18n.tr("error.minutes_range");
                rebuildWidgetsInternal();
                return;
            }

            if (use24HourFormat) {
                // 24 -> 12: preserve the value currently typed in the field.
                if (shownHour < 0 || shownHour > 23) {
                    validationError = ChronicleI18n.tr("error.hour_24_range");
                    rebuildWidgetsInternal();
                    return;
                }
                isPm = shownHour >= 12;
                int shown12 = shownHour % 12;
                if (shown12 == 0) shown12 = 12;
                hourText = String.format("%02d", shown12);
                use24HourFormat = false;
            } else {
                // 12 -> 24: convert the currently typed 12-hour value instead of
                // falling back to the original draft hour.
                if (shownHour < 1 || shownHour > 12) {
                    validationError = ChronicleI18n.tr("error.hour_12_range");
                    rebuildWidgetsInternal();
                    return;
                }
                int h24 = shownHour % 12 + (isPm ? 12 : 0);
                hourText = String.format("%02d", h24);
                use24HourFormat = true;
            }
            validationError = null;
            rebuildWidgetsInternal();
        } catch (NumberFormatException e) {
            validationError = ChronicleI18n.tr("error.valid_time_first");
            rebuildWidgetsInternal();
        }
    }

    private void toggleIntervalUnit() {
        captureValues();
        final int value;
        try {
            value = Integer.parseInt(intervalText.trim());
        } catch (NumberFormatException ignored) {
            validationError = ChronicleI18n.tr("error.valid_numbers");
            rebuildWidgetsInternal();
            return;
        }
        if (value < 1) {
            validationError = ChronicleI18n.tr("error.interval_min");
            rebuildWidgetsInternal();
            return;
        }
        if (intervalUsesHours) {
            intervalText = Integer.toString(value * 60);
            intervalUsesHours = false;
            validationError = null;
        } else {
            if (value % 60 != 0) {
                validationError = ChronicleI18n.tr("error.minutes_to_hours");
                rebuildWidgetsInternal();
                return;
            }
            intervalText = Integer.toString(Math.max(1, value / 60));
            intervalUsesHours = true;
            validationError = null;
        }
        rebuildWidgetsInternal();
    }

    private boolean hasAnyWeeklyDay() {
        for (boolean b : weeklyDays) if (b) return true;
        return false;
    }

    private void saveAndClose() {
        captureValues();
        Reminder originalSnapshot = isNew ? null : original.copy();
        int originalIndex = isNew ? -1 : ChronicleClient.CONFIG.reminders.indexOf(original);
        if (!isNew && originalIndex < 0) {
            validationError = ChronicleI18n.tr("error.reminder_changed");
            rebuildWidgetsInternal();
            return;
        }
        boolean originalUse24HourFormat = ChronicleClient.CONFIG.use24HourFormat;
        try {
            draft.message = messageText == null || messageText.isBlank()
                    ? ChronicleI18n.tr("default.reminder") : messageText.trim();
            draft.afterTriggerAction = afterTriggerAction == null
                    ? Reminder.AfterTriggerAction.KEEP : afterTriggerAction;

            if (scheduleType == Reminder.ScheduleType.INTERVAL) {
                int value = Integer.parseInt(intervalText.trim());
                if (value < 1) {
                    validationError = ChronicleI18n.tr("error.interval_min");
                    rebuildWidgetsInternal();
                    return;
                }
                long minutes = intervalUsesHours ? value * 60L : value;
                if (minutes > 7L * 24L * 60L) {
                    validationError = ChronicleI18n.tr("error.interval_max");
                    rebuildWidgetsInternal();
                    return;
                }
                draft.intervalMinutes = (int) minutes;
                draft.scheduleType = Reminder.ScheduleType.INTERVAL;
            } else {
                int hour = Integer.parseInt(hourText.trim());
                int minute = Integer.parseInt(minuteText.trim());
                if (minute < 0 || minute > 59) {
                    validationError = ChronicleI18n.tr("error.minutes_range");
                    rebuildWidgetsInternal();
                    return;
                }
                if (use24HourFormat) {
                    if (hour < 0 || hour > 23) {
                        validationError = ChronicleI18n.tr("error.hour_24_range");
                        rebuildWidgetsInternal();
                        return;
                    }
                    draft.hour = hour;
                } else {
                    if (hour < 1 || hour > 12) {
                        validationError = ChronicleI18n.tr("error.hour_12_range");
                        rebuildWidgetsInternal();
                        return;
                    }
                    draft.hour = hour % 12 + (isPm ? 12 : 0);
                }
                draft.minute = minute;
                draft.scheduleType = scheduleType;
                if (scheduleType == Reminder.ScheduleType.WEEKLY) {
                    if (!hasAnyWeeklyDay()) {
                        validationError = ChronicleI18n.tr("error.select_day");
                        rebuildWidgetsInternal();
                        return;
                    }
                    draft.weeklyDays = Arrays.copyOf(weeklyDays, 7);
                }
            }

            if (isNew) {
                draft.enabled = true;
                if (draft.scheduleType == Reminder.ScheduleType.INTERVAL) {
                    draft.resetIntervalTimer();
                }
                ChronicleClient.CONFIG.reminders.add(draft);
            } else {
                boolean intervalTimerChanged = draft.scheduleType == Reminder.ScheduleType.INTERVAL
                        && (originalSnapshot.scheduleType != Reminder.ScheduleType.INTERVAL
                        || originalSnapshot.intervalMinutes != draft.intervalMinutes);
                copyEditableFields(draft, original);
                if (intervalTimerChanged) {
                    original.resetIntervalTimer();
                }
            }

            ChronicleClient.CONFIG.use24HourFormat = use24HourFormat;
            ChronicleClient.CONFIG.ensureValid();
            if (ChronicleClient.CONFIG.save()) {
                onClose();
            } else {
                if (isNew) {
                    ChronicleClient.CONFIG.reminders.remove(draft);
                } else if (originalSnapshot != null && originalIndex >= 0
                        && originalIndex < ChronicleClient.CONFIG.reminders.size()) {
                    copyAllFields(originalSnapshot, original);
                }
                ChronicleClient.CONFIG.use24HourFormat = originalUse24HourFormat;
                validationError = ChronicleClient.CONFIG.getLastSaveError();
                rebuildWidgetsInternal();
            }
        } catch (NumberFormatException ignored) {
            if (isNew) {
                ChronicleClient.CONFIG.reminders.remove(draft);
            } else if (originalSnapshot != null && originalIndex >= 0
                    && originalIndex < ChronicleClient.CONFIG.reminders.size()) {
                copyAllFields(originalSnapshot, original);
            }
            ChronicleClient.CONFIG.use24HourFormat = originalUse24HourFormat;
            validationError = ChronicleI18n.tr("error.valid_numbers");
            rebuildWidgetsInternal();
        }
    }

    private static void copyEditableFields(Reminder source, Reminder target) {
        target.hour = source.hour;
        target.minute = source.minute;
        target.message = source.message;
        target.scheduleType = source.scheduleType;
        target.afterTriggerAction = source.afterTriggerAction;
        target.intervalMinutes = source.intervalMinutes;
        target.weeklyDays = source.weeklyDays == null ? null : Arrays.copyOf(source.weeklyDays, 7);
    }

    private static void copyAllFields(Reminder source, Reminder target) {
        copyEditableFields(source, target);
        target.enabled = source.enabled;
        target.lastTriggeredEpochMinute = source.lastTriggeredEpochMinute;
        target.nextTriggerEpochMillis = source.nextTriggerEpochMillis;
        target.lastTriggeredWallClockMinute = source.lastTriggeredWallClockMinute;
    }

    /**
     * Handle the time controls explicitly before vanilla child dispatch.
     *
     * The editor repositions/rebuilds widgets while switching between 12/24-hour
     * modes and scrolling. On some 26.2 client builds, the AM/PM child can stop
     * receiving the click even though it is drawn at the correct position. Keep
     * the control functional by resolving the click against its current bounds
     * here, while still letting all other widgets use the normal Screen dispatch.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        // Footer is the top visual/input layer and must win over a partially
        // clipped content widget at the viewport edge.
        if (clickButton(saveButton, event, doubleClick) || clickButton(cancelButton, event, doubleClick)) {
            return true;
        }
        if (periodButton != null && periodButton.visible && isInContentViewport(periodButton)
                && clickButton(periodButton, event, doubleClick)) {
            return true;
        }

        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int panelTop = UiMetrics.panelTop(this.height);
        int clipTop = panelTop + UiMetrics.HEADER_HEIGHT;
        int clipBottom = saveButton == null ? this.height - 64 : saveButton.getY() - footerContentGap();
        clipBottom = Math.max(clipTop + 20, clipBottom);
        if (event.x() < left || event.x() >= left + panelW
                || event.y() < clipTop || event.y() >= clipBottom) {
            return false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean clickButton(Button button, MouseButtonEvent event, boolean doubleClick) {
        return button != null && button.visible && isInside(button, event.x(), event.y())
                && button.mouseClicked(event, doubleClick);
    }

    private static boolean isInside(Button button, double mouseX, double mouseY) {
        return mouseX >= button.getX()
                && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY()
                && mouseY < button.getY() + button.getHeight();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll <= 0 || Math.abs(verticalAmount) < 1.0E-9) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int panelTop = UiMetrics.panelTop(this.height);
        int contentClipTop = panelTop + UiMetrics.HEADER_HEIGHT;
        int contentClipBottom = saveButton == null ? this.height - 64 : saveButton.getY() - footerContentGap();
        contentClipBottom = Math.max(contentClipTop + 20, contentClipBottom);
        boolean insideContent = mouseX >= left && mouseX < left + panelW
                && mouseY >= contentClipTop && mouseY < contentClipBottom;
        if (!insideContent) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int direction = verticalAmount < 0 ? 1 : -1;
        int next = Math.max(0, Math.min(maxScroll,
                scrollOffset + direction * (UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM)));
        if (next != scrollOffset) {
            captureValues();
            String focusedField = focusedFieldKey();
            scrollOffset = next;
            rebuildWidgetsInternal();
            restoreFocusedField(focusedField);
        }
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = Util.getMillis();
        transition.begin(graphics, this.width, this.height);

        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int panelTop = UiMetrics.panelTop(this.height);
        int panelBottom = Math.max(panelTop + 1, Math.min(this.height, this.height - 12));

        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);
        UiFrame.drawWindow(graphics, left, panelTop, panelW, panelBottom);

        int headerInset = UiMetrics.contentInset(panelW);
        int headerTextX = left + headerInset;
        String modeLabel = ChronicleI18n.tr("schedule." + scheduleType.name().toLowerCase(java.util.Locale.ROOT));
        int modeWidth = this.font.width(modeLabel);
        boolean showModeBadge = panelW >= 260;
        int titleMaxWidth = Math.max(1, left + panelW - headerInset - headerTextX
                - (showModeBadge ? modeWidth + UiMetrics.GAP_LG : 0));
        String shownTitle = trimToWidth(this.title.getString(), titleMaxWidth);
        graphics.text(this.font, Component.literal(shownTitle), headerTextX, panelTop + 18, TEXT, false);
        if (showModeBadge) {
            graphics.text(this.font, Component.literal(modeLabel),
                    left + panelW - headerInset - modeWidth, panelTop + 18, ACCENT, false);
        }
        String helpText = switch (scheduleType) {
            case WEEKLY -> ChronicleI18n.tr("editor.help.weekly");
            case INTERVAL -> ChronicleI18n.tr("editor.help.interval");
            default -> ChronicleI18n.tr("editor.help.daily");
        };
        graphics.text(this.font, Component.literal(trimToWidth(helpText,
                        Math.max(1, left + panelW - headerInset - headerTextX))),
                headerTextX, panelTop + 38, MUTED, false);

        int contentClipTop = panelTop + UiMetrics.HEADER_HEIGHT;
        int contentClipBottom = saveButton == null ? panelBottom - 52 : saveButton.getY() - footerContentGap();
        contentClipBottom = Math.max(contentClipTop + 20, contentClipBottom);

        // Draw content inside a real scissor region. Footer controls are temporarily hidden
        // from vanilla rendering so they can remain outside the clipped viewport.
        updateContentWidgetVisibility(contentClipTop, contentClipBottom);
        saveButton.visible = false;
        cancelButton.visible = false;
        graphics.enableScissor(left, contentClipTop, left + panelW, contentClipBottom);

        if (dailyButton != null && inVerticalClip(dailyButton.getY() - UiMetrics.LABEL_OFFSET, contentClipTop, contentClipBottom)) {
            graphics.text(this.font, ChronicleI18n.component("editor.section.schedule"), dailyButton.getX(),
                    dailyButton.getY() - UiMetrics.LABEL_OFFSET, MUTED, false);
        }
        if (scheduleType == Reminder.ScheduleType.INTERVAL && intervalBox != null
                && inVerticalClip(intervalBox.getY() - UiMetrics.LABEL_OFFSET, contentClipTop, contentClipBottom)) {
            graphics.text(this.font, ChronicleI18n.component("editor.section.interval"), intervalBox.getX(),
                    intervalBox.getY() - UiMetrics.LABEL_OFFSET, MUTED, false);
        } else if (hourBox != null && inVerticalClip(hourBox.getY() - UiMetrics.LABEL_OFFSET, contentClipTop, contentClipBottom)) {
            graphics.text(this.font, ChronicleI18n.component("editor.section.time"), hourBox.getX(),
                    hourBox.getY() - UiMetrics.LABEL_OFFSET, MUTED, false);
        }
        if (scheduleType == Reminder.ScheduleType.WEEKLY && !dayButtons.isEmpty()
                && inVerticalClip(dayButtons.get(0).getY() - UiMetrics.LABEL_OFFSET, contentClipTop, contentClipBottom)) {
            graphics.text(this.font, ChronicleI18n.component("editor.section.days"), dayButtons.get(0).getX(),
                    dayButtons.get(0).getY() - UiMetrics.LABEL_OFFSET, MUTED, false);
        }
        if (messageBox != null && inVerticalClip(messageBox.getY() - UiMetrics.LABEL_OFFSET, contentClipTop, contentClipBottom)) {
            int labelY = messageBox.getY() - UiMetrics.LABEL_OFFSET;
            String messageLabel = ChronicleI18n.tr("editor.section.message");
            graphics.text(this.font, Component.literal(messageLabel), messageBox.getX(), labelY, MUTED, false);
            String value = messageBox.getValue() == null ? "" : messageBox.getValue();
            // EditBox and config persistence both enforce an 80 UTF-16-unit
            // limit, so the counter must report that same unit for emoji too.
            int length = value.length();
            String counter = length + "/80";
            if (messageBox.getWidth() >= this.font.width(messageLabel) + UiMetrics.GAP_SM + this.font.width(counter)) {
                graphics.text(this.font, Component.literal(counter),
                        messageBox.getX() + messageBox.getWidth() - this.font.width(counter),
                        labelY, UiFrame.SUBTLE, false);
            }
            int hintY = messageBox.getY() + messageBox.getHeight() + 4;
            if (inVerticalClip(hintY, contentClipTop, contentClipBottom)) {
                String hint = trimToWidth(ChronicleI18n.tr("editor.placeholders_hint"), messageBox.getWidth());
                graphics.text(this.font, Component.literal(hint), messageBox.getX(), hintY,
                        UiFrame.SUBTLE, false);
            }
        }
        if (afterTriggerButton != null
                && inVerticalClip(afterTriggerButton.getY() - UiMetrics.LABEL_OFFSET,
                contentClipTop, contentClipBottom)) {
            graphics.text(this.font, ChronicleI18n.component("editor.section.after_trigger"),
                    afterTriggerButton.getX(), afterTriggerButton.getY() - UiMetrics.LABEL_OFFSET,
                    MUTED, false);
        }

        for (var child : children()) {
            if (child instanceof EditBox box && box.visible) {
                drawField(graphics, box, mouseX, mouseY);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Re-style content controls before removing the scissor. Drawing this
        // pass later caused partially visible buttons to bleed into the footer.
        for (var child : children()) {
            if (child instanceof Button button && button.visible) {
                drawEditorButton(graphics, button, mouseX, mouseY);
            }
        }
        if (pressedButton != saveButton && pressedButton != cancelButton) {
            drawPressOverlay(graphics, now);
        }

        if (maxScroll > 0) {
            int viewportHeight = Math.max(1, contentClipBottom - contentClipTop);
            UiFrame.drawScrollBar(graphics, left + panelW - 10,
                    contentClipTop + UiMetrics.GAP_XS, contentClipBottom - UiMetrics.GAP_XS,
                    scrollOffset / (float) maxScroll,
                    viewportHeight / (float) (viewportHeight + maxScroll));
        }

        graphics.disableScissor();
        saveButton.visible = true;
        cancelButton.visible = true;

        drawEditorButton(graphics, saveButton, mouseX, mouseY);
        drawEditorButton(graphics, cancelButton, mouseX, mouseY);

        if (validationError != null) {
            int errorAvailable = Math.max(1, panelW - headerInset * 2);
            int boxW = Math.max(1, Math.min(errorAvailable, this.font.width(validationError) + 16));
            int y = Math.max(panelTop + 4, saveButton.getY() - 34);
            int errorX = left + headerInset + Math.max(0, (errorAvailable - boxW) / 2);
            graphics.fill(errorX, y, errorX + boxW, y + 26, 0xFF2A1C20);
            int maxTextWidth = Math.max(8, boxW - 16);
            String shownError = trimToWidth(validationError, maxTextWidth);
            graphics.text(this.font, Component.literal(shownError), errorX + 8, y + 8, ERROR, false);
        }

        if (pressedButton == saveButton || pressedButton == cancelButton) {
            drawPressOverlay(graphics, now);
        }
        transition.end(graphics, this.width, this.height);
    }

    private void drawEditorButton(GuiGraphicsExtractor graphics, Button button, int mouseX, int mouseY) {
        if (button == null || !button.visible) return;
        int accent = button == cancelButton ? MUTED : ACCENT;
        boolean emphasized = button == saveButton;
        if (scheduleType == Reminder.ScheduleType.WEEKLY && dayButtons.contains(button)) {
            int idx = dayButtons.indexOf(button);
            emphasized = idx >= 0 && weeklyDays[idx];
            accent = emphasized ? ACCENT : BORDER;
        } else if (button == dailyButton && scheduleType != Reminder.ScheduleType.DAILY) {
            accent = BORDER;
        } else if (button == weeklyButton && scheduleType != Reminder.ScheduleType.WEEKLY) {
            accent = BORDER;
        } else if (button == intervalButton && scheduleType != Reminder.ScheduleType.INTERVAL) {
            accent = BORDER;
        } else if (button == dailyButton || button == weeklyButton || button == intervalButton) {
            emphasized = true;
        } else if (button == afterTriggerButton) {
            emphasized = afterTriggerAction != Reminder.AfterTriggerAction.KEEP;
        }
        UiFrame.drawButton(graphics, this.font, button, accent, emphasized, mouseX, mouseY);
    }

    private void drawPressOverlay(GuiGraphicsExtractor graphics, long now) {
        if (pressedAt < 0) return;
        float press = UiAnimation.pressProgress(pressedAt, now, 160L);
        if (press <= 0f) return;
        int alpha = Math.round(press * 60f);
        graphics.fill(pressedX, pressedY, pressedX + pressedW, pressedY + pressedH,
                (alpha << 24) | 0x008FB3E8);
    }

    private boolean inVerticalClip(int y, int top, int bottom) {
        return y + this.font.lineHeight >= top && y <= bottom;
    }

    private void updateContentWidgetVisibility(int clipTop, int clipBottom) {
        for (var child : children()) {
            if (!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)) continue;
            if (child instanceof Button button && (button == saveButton || button == cancelButton)) {
                widget.visible = true;
                continue;
            }
            // Scissor clipping handles partial visibility. Only completely off-screen widgets
            // are disabled so focus/hitboxes do not survive far outside the viewport.
            boolean visibleInViewport = widget.getY() + widget.getHeight() > clipTop
                    && widget.getY() < clipBottom;
            widget.visible = visibleInViewport;
            if (!visibleInViewport && getFocused() == widget) {
                clearFocus();
            }
        }
    }

    private void drawField(GuiGraphicsExtractor graphics, EditBox box, int mouseX, int mouseY) {
        boolean hovered = mouseX >= box.getX() && mouseX < box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY < box.getY() + box.getHeight();
        int border = (hovered || box.isFocused()) ? HOVER : BORDER;
        graphics.fill(box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(), border);
        graphics.fill(box.getX() + 1, box.getY() + 1, box.getX() + box.getWidth() - 1,
                box.getY() + box.getHeight() - 1, FIELD);
    }

    private String trimToWidth(String text, int maxWidth) {
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

    @Override
    public void onClose() {
        transition.start(this.minecraft, parent);
    }
}
