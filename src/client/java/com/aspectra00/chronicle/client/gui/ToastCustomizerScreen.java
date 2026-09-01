package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.ChroniclePlaceholders;
import com.aspectra00.chronicle.client.CustomSoundPlayer;
import com.aspectra00.chronicle.client.CustomToastBackground;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ToastCustomizerScreen extends Screen {
    private static final int DESKTOP_LAYOUT_MIN_HEIGHT = 820;
    private static final long DRAFT_TEST_DEBOUNCE_MS = 500L;
    private static final int[] SNOOZE_OPTIONS = {5, 10, 15, 30, 60};
    private static final int ACCENT = 0xFF8FB3E8;
    private static final int ACCENT_ALT = 0xFF8995A4;
    private static final int MUTED = 0xFF8995A4;

    private final Screen parent;
    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private long lastPressedAt = -1L;
    private int lastPressedX;
    private int lastPressedY;
    private int lastPressedW;
    private int lastPressedH;
    private Button lastPressedButton;
    private long lastDraftTestAt = Long.MIN_VALUE;
    private long previewTitleResolvedAt = Long.MIN_VALUE;
    private String previewTitleSource;
    private String resolvedPreviewTitle;
    private EditBox iconBox;
    private EditBox titleBox;
    private EditBox backgroundBox;
    private EditBox borderBox;
    private EditBox accentBox;
    private EditBox titleColorBox;
    private EditBox messageColorBox;
    private EditBox iconColorBox;
    private String draftStyle;
    private int draftBackground;
    private int draftBorder;
    private int draftAccent;
    private int draftTitleColor;
    private int draftMessageColor;
    private int draftIconColor;
    private float draftTitleScale;
    private float draftMessageScale;
    private float draftIconScale;
    private String draftFrameStyle;
    private String draftBackgroundImagePath;
    private boolean draftAnimationsEnabled;
    private boolean draftToastActionsEnabled;
    private int draftToastSnoozeMinutes;
    private boolean advancedColorsVisible;
    private Button titleMinus, titleSize, titlePlus, messageMinus, messageSize, messagePlus, iconMinus, iconSize, iconPlus;
    private Button frameStyleButton, animationsButton, actionsButton, snoozeDurationButton;
    private Button backgroundImageButton, removeBackgroundImageButton;
    private Button advancedColorsButton;
    private Button applyButton, testButton, cancelButton;
    private String paletteTarget = "BACKGROUND";
    private Button paletteTargetButton;
    private int scrollOffset;
    private String savedIconText, savedTitleText, savedBackgroundText, savedBorderText;
    private String savedAccentText, savedTitleColorText, savedMessageColorText, savedIconColorText;
    private boolean draggingColorPicker;
    private boolean draggingHue;
    private float pickerHue;
    private float pickerSaturation;
    private float pickerValue;
    private String validationError;
    private String lastHexValidationError;
    private boolean settingsApplied;
    private final Map<AbstractWidget, Integer> scrollBaseY = new IdentityHashMap<>();
    private final Map<Button, String> styleButtonValues = new IdentityHashMap<>();
    private final Map<Button, String> paletteButtonValues = new IdentityHashMap<>();

    public ToastCustomizerScreen(Screen parent) {
        super(ChronicleI18n.component("toast.title"));
        this.parent = parent;
        this.draftStyle = ChronicleClient.CONFIG.toastStyle;
        this.draftBackground = ChronicleClient.CONFIG.toastBackgroundColor;
        this.draftBorder = ChronicleClient.CONFIG.toastBorderColor;
        this.draftAccent = ChronicleClient.CONFIG.toastAccentColor;
        this.draftTitleColor = ChronicleClient.CONFIG.toastTitleColor;
        this.draftMessageColor = ChronicleClient.CONFIG.toastMessageColor;
        this.draftIconColor = ChronicleClient.CONFIG.toastIconColor;
        this.draftTitleScale = ChronicleClient.CONFIG.toastTitleScale;
        this.draftMessageScale = ChronicleClient.CONFIG.toastMessageScale;
        this.draftIconScale = ChronicleClient.CONFIG.toastIconScale;
        this.draftFrameStyle = ChronicleClient.CONFIG.toastFrameStyle;
        this.draftBackgroundImagePath = ChronicleClient.CONFIG.toastBackgroundImagePath;
        this.draftAnimationsEnabled = ChronicleClient.CONFIG.animationsEnabled;
        this.draftToastActionsEnabled = ChronicleClient.CONFIG.toastActionsEnabled;
        this.draftToastSnoozeMinutes = ChronicleClient.CONFIG.toastSnoozeMinutes;
        updatePickerFromTarget();
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
    protected void rebuildWidgets() {
        GuiEventListener focused = getFocused();
        String focusKey = focusedFieldKey(focused);
        clearFocus();
        setDragging(false);
        draggingColorPicker = false;
        draggingHue = false;
        init();
        restoreFocusedField(focusKey);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        lastPressedAt = -1L;
        setDragging(false);
        draggingColorPicker = false;
        draggingHue = false;
        super.resize(minecraft, width, height);
    }

    private String focusedFieldKey(GuiEventListener focused) {
        if (focused == iconBox) return "icon";
        if (focused == titleBox) return "title";
        if (focused == backgroundBox) return "background";
        if (focused == borderBox) return "border";
        if (focused == accentBox) return "accent";
        if (focused == titleColorBox) return "titleColor";
        if (focused == messageColorBox) return "messageColor";
        if (focused == iconColorBox) return "iconColor";
        return null;
    }

    private void restoreFocusedField(String key) {
        if (key == null) return;
        EditBox target = switch (key) {
            case "icon" -> iconBox;
            case "title" -> titleBox;
            case "background" -> backgroundBox;
            case "border" -> borderBox;
            case "accent" -> accentBox;
            case "titleColor" -> titleColorBox;
            case "messageColor" -> messageColorBox;
            case "iconColor" -> iconColorBox;
            default -> null;
        };
        if (target != null && target.active && isInsideContentViewport(
                target.getX() + target.getWidth() / 2.0,
                target.getY() + target.getHeight() / 2.0)) {
            setFocused(target);
            target.setFocused(true);
        }
    }

    @Override
    protected void init() {
        if (iconBox != null) {
            savedIconText = iconBox.getValue();
            savedTitleText = titleBox.getValue();
            savedBackgroundText = backgroundBox.getValue();
            savedBorderText = borderBox.getValue();
            savedAccentText = accentBox.getValue();
            savedTitleColorText = titleColorBox.getValue();
            savedMessageColorText = messageColorBox.getValue();
            savedIconColorText = iconColorBox.getValue();
        }
        clearFocus();
        setDragging(false);
        clearWidgets();
        styleButtonValues.clear();
        paletteButtonValues.clear();
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        int previewW = preferredPreviewColumnWidth(panelW);
        boolean compactStyles = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        int columnGap = UiMetrics.GAP_LG;
        int sideInset = UiMetrics.contentInset(panelW);
        int controlsLeft = left + sideInset;
        int usablePanelW = Math.max(1, panelW - sideInset * 2);
        int controlW = controlAreaWidth(compactStyles, usablePanelW, previewW);
        boolean twoColumnFields = usesTwoColumnFields(controlW);
        int boxW = twoColumnFields ? Math.max(1, (controlW - 16) / 2) : controlW;
        int styleColumns = compactStyles ? 2 : 4;
        int styleGap = UiMetrics.GAP_SM;
        int styleW = Math.max(1, (controlW - styleGap * (styleColumns - 1)) / styleColumns);
        scrollOffset = compactStyles
                ? Math.max(0, Math.min(scrollOffset, maxScrollAmount()))
                : 0;
        int previewFirstShift = compactStyles ? compactPreviewFirstShift(this.height) : 0;
        int stylesY = top + UiMetrics.HEADER_HEIGHT + UiMetrics.GAP_SM
                + UiMetrics.LABEL_OFFSET + previewFirstShift;
        int styleX = controlsLeft;
        int styleRowStep = UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM;
        Button neon = styleButton(ChronicleI18n.tr("toast.style.neon"), styleX, stylesY, styleW, UiMetrics.CONTROL_HEIGHT, "NEON");
        Button minimal = styleButton(ChronicleI18n.tr("toast.style.minimal"),
                styleX + styleW + styleGap, stylesY, styleW, UiMetrics.CONTROL_HEIGHT, "MINIMAL");
        Button glass = styleButton(ChronicleI18n.tr("toast.style.glass"),
                compactStyles ? styleX : styleX + (styleW + styleGap) * 2,
                compactStyles ? stylesY + styleRowStep : stylesY,
                styleW, UiMetrics.CONTROL_HEIGHT, "GLASS");
        Button matrix = styleButton(ChronicleI18n.tr("toast.style.matrix"),
                compactStyles ? styleX + styleW + styleGap : styleX + (styleW + styleGap) * 3,
                compactStyles ? stylesY + styleRowStep : stylesY,
                styleW, UiMetrics.CONTROL_HEIGHT, "MATRIX");
        addRenderableWidget(neon);
        addRenderableWidget(minimal);
        addRenderableWidget(glass);
        addRenderableWidget(matrix);

        int appearanceY = stylesY
                + (compactStyles ? styleRowStep + UiMetrics.CONTROL_HEIGHT : UiMetrics.CONTROL_HEIGHT)
                + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;
        int appearanceColumns = appearanceColumnCount(controlW);
        int appearanceGap = UiMetrics.GAP_SM;
        int appearanceW = Math.max(1,
                (controlW - appearanceGap * (appearanceColumns - 1)) / appearanceColumns);
        frameStyleButton = styledButton(frameStyleLabel(), controlsLeft, appearanceY,
                appearanceW, UiMetrics.CONTROL_HEIGHT, b -> toggleFrameStyle());
        animationsButton = styledButton(animationsLabel(),
                appearanceColumns == 1 ? controlsLeft : controlsLeft + appearanceW + appearanceGap,
                appearanceColumns == 1 ? appearanceY + UiMetrics.CONTROL_HEIGHT + appearanceGap : appearanceY,
                appearanceW, UiMetrics.CONTROL_HEIGHT, b -> toggleAnimations());
        int actionsX = appearanceColumns == 3
                ? controlsLeft + (appearanceW + appearanceGap) * 2 : controlsLeft;
        int actionsY = appearanceColumns == 3 ? appearanceY
                : appearanceY + (UiMetrics.CONTROL_HEIGHT + appearanceGap)
                * (appearanceColumns == 2 ? 1 : 2);
        actionsButton = styledButton(actionsLabel(), actionsX, actionsY,
                appearanceColumns == 2 ? controlW : appearanceW,
                UiMetrics.CONTROL_HEIGHT, b -> toggleToastActions());
        int finalAppearanceY = actionsY + UiMetrics.CONTROL_HEIGHT + appearanceGap;
        boolean splitFinalAppearanceRow = controlW >= 220;
        int finalAppearanceW = splitFinalAppearanceRow
                ? Math.max(1, (controlW - appearanceGap) / 2) : controlW;
        snoozeDurationButton = styledButton(snoozeDurationLabel(), controlsLeft,
                finalAppearanceY, finalAppearanceW, UiMetrics.CONTROL_HEIGHT,
                b -> cycleSnoozeDuration());
        advancedColorsButton = styledButton(advancedColorsLabel(),
                splitFinalAppearanceRow ? controlsLeft + finalAppearanceW + appearanceGap : controlsLeft,
                splitFinalAppearanceRow ? finalAppearanceY
                        : finalAppearanceY + UiMetrics.CONTROL_HEIGHT + appearanceGap,
                finalAppearanceW, UiMetrics.CONTROL_HEIGHT, b -> toggleAdvancedColors());
        snoozeDurationButton.active = draftToastActionsEnabled;
        addRenderableWidget(frameStyleButton);
        addRenderableWidget(animationsButton);
        addRenderableWidget(actionsButton);
        addRenderableWidget(snoozeDurationButton);
        addRenderableWidget(advancedColorsButton);

        int backgroundRowY = finalAppearanceY + UiMetrics.CONTROL_HEIGHT + appearanceGap;
        int removeImageW = Math.max(1, Math.min(Math.min(96, Math.max(72, controlW / 3)),
                Math.max(1, controlW - appearanceGap - 1)));
        int chooseImageW = Math.max(1, controlW - removeImageW - appearanceGap);
        backgroundImageButton = styledButton(backgroundImageLabel(), controlsLeft,
                backgroundRowY, chooseImageW, UiMetrics.CONTROL_HEIGHT,
                b -> browseForBackgroundImage());
        removeBackgroundImageButton = styledButton(ChronicleI18n.tr("action.remove"),
                controlsLeft + chooseImageW + appearanceGap, backgroundRowY,
                removeImageW, UiMetrics.CONTROL_HEIGHT, b -> removeBackgroundImage());
        addRenderableWidget(backgroundImageButton);
        addRenderableWidget(removeBackgroundImageButton);

        int contentShift = (compactStyles ? styleRowStep + previewFirstShift : 0)
                + appearanceContentShift(compactStyles, controlW);
        int col2 = twoColumnFields ? controlsLeft + boxW + 16 : controlsLeft;
        int iconY = top + 142 + contentShift;
        int titleX = compactStyles ? controlsLeft : controlsLeft + 84;
        int titleW = compactStyles ? controlW : Math.max(1, controlW - 84);
        int iconWidth = Math.max(1, Math.min(72, controlW));
        iconBox = box(controlsLeft, iconY, iconWidth, UiMetrics.CONTROL_HEIGHT,
                savedIconText != null ? savedIconText : ChronicleClient.CONFIG.toastIcon, 4);
        titleBox = box(titleX, iconY, titleW, UiMetrics.CONTROL_HEIGHT,
                savedTitleText != null ? savedTitleText : ChronicleClient.CONFIG.toastTitle, 40);
        if (compactStyles) {
            iconBox.setX(controlsLeft);
            titleBox.setX(controlsLeft);
            titleBox.setY(iconY + UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM);
        }
        addRenderableWidget(iconBox);
        addRenderableWidget(titleBox);

        int sizeY = top + (compactStyles ? 214 : 180) + contentShift;
        boolean twoColumnSizes = usesTwoColumnSizeControls(controlW);
        int sizeGroupW = twoColumnSizes ? boxW : controlW;
        int sizeSideW = Math.min(28, Math.max(1, (sizeGroupW - 10) / 3));
        int sizeControlW = Math.max(1, sizeGroupW - sizeSideW * 2 - 8);
        int titlePlusX = controlsLeft + sizeSideW + 4 + sizeControlW + 4;
        titleMinus = styledButton("−", controlsLeft, sizeY, sizeSideW, 24, b -> adjustTitleScale(-1.00f));
        titleSize = readoutButton(scaleLabel("title", draftTitleScale), controlsLeft + sizeSideW + 4,
                sizeY, sizeControlW, 24);
        titlePlus = styledButton("+", titlePlusX, sizeY, sizeSideW, 24, b -> adjustTitleScale(1.00f));
        int messageBaseX = twoColumnSizes ? col2 : controlsLeft;
        int sizeRowStep = 24 + UiMetrics.GAP_SM;
        int messageSizeY = twoColumnSizes ? sizeY : sizeY + sizeRowStep;
        int messagePlusX = messageBaseX + sizeSideW + 4 + sizeControlW + 4;
        messageMinus = styledButton("−", messageBaseX, messageSizeY, sizeSideW, 24, b -> adjustMessageScale(-1.00f));
        messageSize = readoutButton(scaleLabel("message", draftMessageScale), messageBaseX + sizeSideW + 4,
                messageSizeY, sizeControlW, 24);
        messagePlus = styledButton("+", messagePlusX, messageSizeY, sizeSideW, 24, b -> adjustMessageScale(1.00f));
        addRenderableWidget(titleMinus);
        addRenderableWidget(titleSize);
        addRenderableWidget(titlePlus);
        addRenderableWidget(messageMinus);
        addRenderableWidget(messageSize);
        addRenderableWidget(messagePlus);

        int iconSizeY = sizeY + (twoColumnSizes ? sizeRowStep : sizeRowStep * 2);
        iconMinus = styledButton("−", controlsLeft, iconSizeY, sizeSideW, 24, b -> adjustIconScale(-1.00f));
        iconSize = readoutButton(scaleLabel("icon", draftIconScale), controlsLeft + sizeSideW + 4,
                iconSizeY, sizeControlW, 24);
        iconPlus = styledButton("+", controlsLeft + sizeSideW + 4 + sizeControlW + 4,
                iconSizeY, sizeSideW, 24, b -> adjustIconScale(1.00f));
        addRenderableWidget(iconMinus);
        addRenderableWidget(iconSize);
        addRenderableWidget(iconPlus);

        int rowBase = colorFieldsBase(compactStyles, controlW);
        int fieldRowGap = UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM + UiMetrics.LABEL_OFFSET;
        int row1Y = top + rowBase + contentShift;
        int borderY = twoColumnFields ? row1Y : row1Y + fieldRowGap;
        int accentY = row1Y + fieldRowGap * (twoColumnFields ? 1 : 2);
        int titleColorY = row1Y + fieldRowGap * (twoColumnFields ? 1 : 3);
        int messageColorY = row1Y + fieldRowGap * (twoColumnFields ? 2 : 4);
        int iconColorY = row1Y + fieldRowGap * (twoColumnFields ? 2 : 5);
        backgroundBox = box(controlsLeft, row1Y, boxW, UiMetrics.CONTROL_HEIGHT,
                savedBackgroundText != null ? savedBackgroundText : hex(draftBackground), 9);
        borderBox = box(twoColumnFields ? col2 : controlsLeft, borderY, boxW, UiMetrics.CONTROL_HEIGHT,
                savedBorderText != null ? savedBorderText : hex(draftBorder), 9);
        accentBox = box(controlsLeft, accentY, boxW, UiMetrics.CONTROL_HEIGHT,
                savedAccentText != null ? savedAccentText : hex(draftAccent), 9);
        titleColorBox = box(twoColumnFields ? col2 : controlsLeft, titleColorY, boxW, UiMetrics.CONTROL_HEIGHT,
                savedTitleColorText != null ? savedTitleColorText : hex(draftTitleColor), 9);
        messageColorBox = box(controlsLeft, messageColorY, boxW, UiMetrics.CONTROL_HEIGHT,
                savedMessageColorText != null ? savedMessageColorText : hex(draftMessageColor), 9);
        iconColorBox = box(twoColumnFields ? col2 : controlsLeft, iconColorY, boxW, UiMetrics.CONTROL_HEIGHT,
                savedIconColorText != null ? savedIconColorText : hex(draftIconColor), 9);
        addRenderableWidget(backgroundBox);
        addRenderableWidget(borderBox);
        addRenderableWidget(accentBox);
        addRenderableWidget(titleColorBox);
        addRenderableWidget(messageColorBox);
        addRenderableWidget(iconColorBox);

        int[] palette = {
                0xFF0B2034, 0xFF090D18, 0xFF171B2A, 0xFF07130D,
                0xFF102D3A, 0xFF24142A, 0xFF2A2010, 0xFF2B0D18
        };
        String[] paletteNames = {"ocean", "midnight", "slate", "forest", "teal", "plum", "amber", "ruby"};
        int paletteBase = paletteBase(compactStyles, controlW);
        int paletteY = top + paletteBase + contentShift;
        boolean sideColorWorkspace = usesSideColorWorkspace(compactStyles, controlW);
        int pickerColumnW = compactPickerWidth(controlW);
        int paletteX = sideColorWorkspace ? controlsLeft + pickerColumnW + UiMetrics.GAP_LG : controlsLeft;
        int paletteW = sideColorWorkspace
                ? Math.max(1, controlW - pickerColumnW - UiMetrics.GAP_LG)
                : controlW;
        int targetW = Math.max(1, Math.min(200, paletteW));
        int targetY = sideColorWorkspace ? paletteY : paletteY - UiMetrics.GAP_XS;
        paletteTargetButton = styledButton(paletteTargetLabel(),
                paletteX + Math.max(0, (paletteW - targetW) / 2), targetY, targetW, 24,
                b -> cyclePaletteTarget());
        addRenderableWidget(paletteTargetButton);

        int cardColumns = paletteColumns(paletteW);
        int cardGap = UiMetrics.GAP_SM;
        int cardW = cardColumns == 2 ? (paletteW - cardGap) / 2 : paletteW;
        int cardH = 30;
        int paletteBaseY = paletteY + (sideColorWorkspace ? 32 : 28);
        for (int i = 0; i < palette.length; i++) {
            final int color = palette[i];
            final String name = paletteNames[i];
            int col = cardColumns == 2 ? i % 2 : 0;
            int row = cardColumns == 2 ? i / 2 : i;
            int cardX = paletteX + col * (cardW + cardGap);
            int cardY = paletteBaseY + row * (cardH + cardGap);
            Button colorButton = styledButton(ChronicleI18n.tr("toast.palette." + name), cardX, cardY, Math.max(1, cardW), cardH,
                    b -> setPaletteColor(color));
            paletteButtonValues.put(colorButton, name);
            addRenderableWidget(colorButton);
        }

        int footerY;
        if (compactStyles) {
            int footerGap = UiMetrics.GAP_SM;
            int footerX = left + sideInset;
            if (panelW < 260) {
                footerY = Math.max(0, this.height - 120);
                int footerW = usablePanelW;
                int footerStep = UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM;
                applyButton = styledButton(ChronicleI18n.tr("action.apply"), footerX, footerY, footerW, UiMetrics.CONTROL_HEIGHT, b -> applyAndClose());
                testButton = styledButton(ChronicleI18n.tr("action.show_toast"), footerX, footerY + footerStep, footerW, UiMetrics.CONTROL_HEIGHT, b -> previewFromDraft());
                cancelButton = styledButton(ChronicleI18n.tr("action.cancel"), footerX, footerY + footerStep * 2, footerW, UiMetrics.CONTROL_HEIGHT, b -> onClose());
            } else {
                int footerW = Math.max(1, (usablePanelW - footerGap * 2) / 3);
                footerY = Math.max(0, this.height - 48);
                applyButton = styledButton(ChronicleI18n.tr("action.apply"), footerX, footerY, footerW, UiMetrics.CONTROL_HEIGHT, b -> applyAndClose());
                testButton = styledButton(ChronicleI18n.tr("action.show_toast"), footerX + footerW + footerGap, footerY, footerW, UiMetrics.CONTROL_HEIGHT, b -> previewFromDraft());
                cancelButton = styledButton(ChronicleI18n.tr("action.cancel"), footerX + (footerW + footerGap) * 2, footerY, footerW, UiMetrics.CONTROL_HEIGHT, b -> onClose());
            }
        } else {
            footerY = Math.max(0, this.height - 54);
            int footerRight = left + panelW - sideInset;
            cancelButton = styledButton(ChronicleI18n.tr("action.cancel"), footerRight - 112, footerY, 112,
                    UiMetrics.PRIMARY_BUTTON_HEIGHT, b -> onClose());
            testButton = styledButton(ChronicleI18n.tr("action.show_toast"), cancelButton.getX() - UiMetrics.GAP_SM - 126,
                    footerY, 126, UiMetrics.PRIMARY_BUTTON_HEIGHT, b -> previewFromDraft());
            applyButton = styledButton(ChronicleI18n.tr("action.apply"), testButton.getX() - UiMetrics.GAP_SM - 112,
                    footerY, 112, UiMetrics.PRIMARY_BUTTON_HEIGHT, b -> applyAndClose());
        }
        addRenderableWidget(applyButton);
        addRenderableWidget(testButton);
        addRenderableWidget(cancelButton);

        captureScrollBasePositions();
        applyScrollPositions();
        syncFrameSpecificControls();
        if (draftBackgroundImagePath != null && !draftBackgroundImagePath.isBlank()
                && !CustomToastBackground.prepare(this.minecraft, draftBackgroundImagePath)) {
            validationError = CustomToastBackground.getLastError();
        }
    }

    private EditBox box(int x, int y, int width, int height, String value, int max) {
        VerticallyCenteredEditBox b = new VerticallyCenteredEditBox(this.font, x, y, width, height, Component.literal(""));
        b.setMaxLength(max);
        b.setValue(value);
        b.setBordered(false);
        b.setCentered(true);
        b.setTextColor(0xFFE7ECF2);
        return b;
    }

    private void cyclePaletteTarget() {
        if (!readColors()) return;
        String[] targets = {"BACKGROUND", "BORDER", "ACCENT", "TITLE", "MESSAGE", "ICON"};
        int current = 0;
        for (int i = 0; i < targets.length; i++) {
            if (targets[i].equals(paletteTarget)) {
                current = i;
                break;
            }
        }
        paletteTarget = targets[(current + 1) % targets.length];
        updatePickerFromTarget();
        if (paletteTargetButton != null) {
            paletteTargetButton.setMessage(Component.literal(paletteTargetLabel()));
        }
    }

    private String paletteTargetLabel() {
        return ChronicleI18n.tr("toast.edit_color", targetName(paletteTarget)) + " >";
    }

    private String frameStyleLabel() {
        return ChronicleI18n.tr("toast.frame.label", ChronicleI18n.tr(
                "VANILLA".equals(draftFrameStyle) ? "toast.frame.vanilla" : "toast.frame.modern"));
    }

    private String animationsLabel() {
        return ChronicleI18n.tr("toast.animations.label",
                ChronicleI18n.tr(draftAnimationsEnabled ? "action.on" : "action.off"));
    }

    private String actionsLabel() {
        return ChronicleI18n.tr("toast.actions.label",
                ChronicleI18n.tr(draftToastActionsEnabled ? "action.on" : "action.off"));
    }

    private String snoozeDurationLabel() {
        return ChronicleI18n.tr("toast.snooze.label",
                ChronicleClient.formatInterval(draftToastSnoozeMinutes));
    }

    private String advancedColorsLabel() {
        return ChronicleI18n.tr(advancedColorsVisible
                ? "toast.advanced.hide" : "toast.advanced.show");
    }

    private String backgroundImageLabel() {
        if ("VANILLA".equals(draftFrameStyle)) {
            return ChronicleI18n.tr("toast.background.modern_only");
        }
        String value = ChronicleI18n.tr("toast.background.none");
        if (draftBackgroundImagePath != null && !draftBackgroundImagePath.isBlank()) {
            try {
                Path fileName = Path.of(draftBackgroundImagePath).getFileName();
                if (fileName != null && !fileName.toString().isBlank()) value = fileName.toString();
            } catch (RuntimeException ignored) {
                value = ChronicleI18n.tr("toast.background.selected");
            }
        }
        return ChronicleI18n.tr("toast.background.label", value);
    }

    private void browseForBackgroundImage() {
        String defaultPath = draftBackgroundImagePath == null || draftBackgroundImagePath.isBlank()
                ? Path.of(System.getProperty("user.home", ".")).toAbsolutePath().toString()
                : draftBackgroundImagePath;
        String selected;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String[] patterns = {"*.png", "*.jpg", "*.jpeg"};
            PointerBuffer nativePatterns = stack.mallocPointer(patterns.length);
            for (String pattern : patterns) nativePatterns.put(stack.UTF8(pattern));
            nativePatterns.flip();
            selected = TinyFileDialogs.tinyfd_openFileDialog(
                    ChronicleI18n.tr("toast.background.dialog.title"),
                    defaultPath, nativePatterns,
                    ChronicleI18n.tr("toast.background.dialog.formats"), false);
        }
        if (selected == null || selected.isBlank()) return;
        String path;
        try {
            path = Path.of(selected).toAbsolutePath().normalize().toString();
        } catch (RuntimeException failure) {
            validationError = ChronicleI18n.tr("error.image_missing");
            return;
        }
        String error = CustomToastBackground.validateFile(path);
        if (error != null) {
            validationError = error;
            return;
        }
        if (!CustomToastBackground.prepare(this.minecraft, path, true)) {
            validationError = CustomToastBackground.getLastError();
            return;
        }
        draftBackgroundImagePath = path;
        validationError = null;
        backgroundImageButton.setMessage(Component.literal(backgroundImageLabel()));
        removeBackgroundImageButton.active = true;
    }

    private void removeBackgroundImage() {
        draftBackgroundImagePath = "";
        validationError = null;
        if (backgroundImageButton != null) {
            backgroundImageButton.setMessage(Component.literal(backgroundImageLabel()));
        }
        if (removeBackgroundImageButton != null) removeBackgroundImageButton.active = false;
    }

    private void toggleAdvancedColors() {
        if (advancedColorsVisible && !readColors()) {
            return;
        }
        advancedColorsVisible = !advancedColorsVisible;
        draggingColorPicker = false;
        draggingHue = false;
        if (!advancedColorsVisible && isAdvancedColorWidget(getFocused())) {
            clearFocus();
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollAmount()));
        init();
    }

    private boolean draftActionsVisible() {
        return draftToastActionsEnabled && !"VANILLA".equals(draftFrameStyle);
    }

    private void toggleFrameStyle() {
        boolean switchingToVanilla = !"VANILLA".equals(draftFrameStyle);
        if (switchingToVanilla && advancedColorsVisible && !readColors()) {
            return;
        }
        draftFrameStyle = switchingToVanilla ? "VANILLA" : "MODERN";
        if (switchingToVanilla) {
            advancedColorsVisible = false;
            draggingColorPicker = false;
            draggingHue = false;
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollAmount()));
        init();
    }

    private void syncFrameSpecificControls() {
        boolean modern = !"VANILLA".equals(draftFrameStyle);
        boolean advanced = modern && advancedColorsVisible;
        if (actionsButton != null) actionsButton.active = modern;
        if (advancedColorsButton != null) advancedColorsButton.active = modern;
        if (backgroundImageButton != null) {
            backgroundImageButton.active = modern;
            backgroundImageButton.setMessage(Component.literal(backgroundImageLabel()));
        }
        if (removeBackgroundImageButton != null) {
            removeBackgroundImageButton.active = draftBackgroundImagePath != null
                    && !draftBackgroundImagePath.isBlank();
        }
        if (snoozeDurationButton != null) {
            snoozeDurationButton.active = modern && draftToastActionsEnabled;
        }
        for (Button button : java.util.List.of(titleMinus, titlePlus, messageMinus, messagePlus,
                iconMinus, iconPlus)) {
            if (button != null) button.active = modern;
        }
        for (Button button : styleButtonValues.keySet()) {
            if (button != null) button.active = modern;
        }
        for (Button button : paletteButtonValues.keySet()) {
            if (button != null) {
                button.active = advanced;
                button.visible = advancedColorsVisible;
            }
        }
        if (paletteTargetButton != null) {
            paletteTargetButton.active = advanced;
            paletteTargetButton.visible = advancedColorsVisible;
        }
        if (iconBox != null) iconBox.active = modern;
        for (EditBox box : java.util.List.of(backgroundBox, borderBox, accentBox,
                titleColorBox, messageColorBox, iconColorBox)) {
            if (box != null) {
                box.active = advanced;
                box.visible = advancedColorsVisible;
            }
        }
        if (!modern) {
            draggingColorPicker = false;
            draggingHue = false;
            if (getFocused() != null && getFocused() != titleBox) clearFocus();
        }
        if (titleSize != null) {
            titleSize.active = false;
            titleSize.setMessage(Component.literal(modern
                    ? scaleLabel("title", draftTitleScale) : scaleLabel("title", 1.00f)));
        }
        if (messageSize != null) {
            messageSize.active = false;
            messageSize.setMessage(Component.literal(modern
                    ? scaleLabel("message", draftMessageScale) : scaleLabel("message", 1.00f)));
        }
        if (iconSize != null) {
            iconSize.active = false;
            iconSize.setMessage(Component.literal(modern
                    ? scaleLabel("icon", draftIconScale) : ChronicleI18n.tr("toast.no_icon")));
        }
    }

    private void toggleAnimations() {
        draftAnimationsEnabled = !draftAnimationsEnabled;
        if (animationsButton != null) {
            animationsButton.setMessage(Component.literal(animationsLabel()));
        }
    }

    private boolean isAdvancedColorWidget(Object widget) {
        if (widget == null) return false;
        if (widget == backgroundBox || widget == borderBox || widget == accentBox
                || widget == titleColorBox || widget == messageColorBox || widget == iconColorBox
                || widget == paletteTargetButton) {
            return true;
        }
        return widget instanceof Button button && paletteButtonValues.containsKey(button);
    }

    private void toggleToastActions() {
        draftToastActionsEnabled = !draftToastActionsEnabled;
        if (actionsButton != null) {
            actionsButton.setMessage(Component.literal(actionsLabel()));
        }
        if (snoozeDurationButton != null) {
            snoozeDurationButton.active = draftActionsVisible();
        }
    }

    private void cycleSnoozeDuration() {
        int next = 0;
        for (int i = 0; i < SNOOZE_OPTIONS.length; i++) {
            if (SNOOZE_OPTIONS[i] == draftToastSnoozeMinutes) {
                next = (i + 1) % SNOOZE_OPTIONS.length;
                break;
            }
        }
        draftToastSnoozeMinutes = SNOOZE_OPTIONS[next];
        if (snoozeDurationButton != null) {
            snoozeDurationButton.setMessage(Component.literal(snoozeDurationLabel()));
        }
    }

    private void setPaletteColor(int color) {
        if (!readColors()) return;
        applyColorToTarget(color);
        updatePickerFromTarget();
    }

    private void applyColorToTarget(int color) {
        switch (paletteTarget) {
            case "BORDER" -> {
                draftBorder = color;
                borderBox.setValue(hex(color));
            }
            case "ACCENT" -> {
                draftAccent = color;
                accentBox.setValue(hex(color));
            }
            case "TITLE" -> {
                draftTitleColor = color;
                titleColorBox.setValue(hex(color));
            }
            case "MESSAGE" -> {
                draftMessageColor = color;
                messageColorBox.setValue(hex(color));
            }
            case "ICON" -> {
                draftIconColor = color;
                iconColorBox.setValue(hex(color));
            }
            default -> {
                draftBackground = color;
                backgroundBox.setValue(hex(color));
            }
        }
    }

    private Button styleButton(String label, int x, int y, int width, int height, String style) {
        Button button = styledButton(label, x, y, width, height, b -> {
            draftStyle = style;
            switch (style) {
                case "MINIMAL" -> {
                    draftBackground = 0xFF111827;
                    draftBorder = 0xFF9AA7BD;
                    draftAccent = 0xFFB9C3D7;
                    draftTitleColor = 0xFFE8EEF8;
                    draftMessageColor = 0xFFFFFFFF;
                    draftIconColor = 0xFFDDE6F5;
                }
                case "GLASS" -> {
                    draftBackground = 0xD91A2238;
                    draftBorder = 0xFF6EA8FF;
                    draftAccent = 0xFFB36BFF;
                    draftTitleColor = 0xFFBFE4FF;
                    draftMessageColor = 0xFFF2F5FF;
                    draftIconColor = 0xFFFFD86B;
                }
                case "MATRIX" -> {
                    draftBackground = 0xFF07130D;
                    draftBorder = 0xFF26FF78;
                    draftAccent = 0xFF26FF78;
                    draftTitleColor = 0xFF26FF78;
                    draftMessageColor = 0xFFC7FFD9;
                    draftIconColor = 0xFF26FF78;
                }
                default -> {
                    draftBackground = 0xFF0B2034;
                    draftBorder = 0xFF19D7FF;
                    draftAccent = 0xFF8FB3E8;
                    draftTitleColor = 0xFF16F4FF;
                    draftMessageColor = 0xFFFFFFFF;
                    draftIconColor = 0xFFFFF200;
                }
            }
            syncColorBoxes();
        });
        styleButtonValues.put(button, style);
        return button;
    }

    private boolean styleMatches(String style) {
        return switch (style) {
            case "MINIMAL" -> draftBackground == 0xFF111827 && draftBorder == 0xFF9AA7BD
                    && draftAccent == 0xFFB9C3D7 && draftTitleColor == 0xFFE8EEF8
                    && draftMessageColor == 0xFFFFFFFF && draftIconColor == 0xFFDDE6F5;
            case "GLASS" -> draftBackground == 0xD91A2238 && draftBorder == 0xFF6EA8FF
                    && draftAccent == 0xFFB36BFF && draftTitleColor == 0xFFBFE4FF
                    && draftMessageColor == 0xFFF2F5FF && draftIconColor == 0xFFFFD86B;
            case "MATRIX" -> draftBackground == 0xFF07130D && draftBorder == 0xFF26FF78
                    && draftAccent == 0xFF26FF78 && draftTitleColor == 0xFF26FF78
                    && draftMessageColor == 0xFFC7FFD9 && draftIconColor == 0xFF26FF78;
            case "NEON" -> draftBackground == 0xFF0B2034 && draftBorder == 0xFF19D7FF
                    && draftAccent == 0xFF8FB3E8 && draftTitleColor == 0xFF16F4FF
                    && draftMessageColor == 0xFFFFFFFF && draftIconColor == 0xFFFFF200;
            default -> false;
        };
    }

    private Button styledButton(String label, int x, int y, int width, int height, Button.OnPress press) {
        return Button.builder(Component.literal(label), button -> {
            if (transition.isClosing()) return;
            lastPressedAt = Util.getMillis();
            lastPressedX = button.getX();
            lastPressedY = button.getY();
            lastPressedW = button.getWidth();
            lastPressedH = button.getHeight();
            lastPressedButton = button;
            press.onPress(button);
        }).bounds(x, y, width, height).build();
    }

    private Button readoutButton(String label, int x, int y, int width, int height) {
        Button button = Button.builder(Component.literal(label), ignored -> {})
                .bounds(x, y, width, height).build();
        button.active = false;
        return button;
    }

    private void syncColorBoxes() {
        backgroundBox.setValue(hex(draftBackground));
        borderBox.setValue(hex(draftBorder));
        accentBox.setValue(hex(draftAccent));
        titleColorBox.setValue(hex(draftTitleColor));
        messageColorBox.setValue(hex(draftMessageColor));
        iconColorBox.setValue(hex(draftIconColor));
    }

    private boolean readColors() {
        int[] parsed = new int[6];
        EditBox[] boxes = {backgroundBox, borderBox, accentBox, titleColorBox, messageColorBox, iconColorBox};
        int[] fallbacks = {draftBackground, draftBorder, draftAccent, draftTitleColor, draftMessageColor, draftIconColor};
        String[] names = {targetName("BACKGROUND"), targetName("BORDER"), targetName("ACCENT"),
                targetName("TITLE"), targetName("MESSAGE"), targetName("ICON")};
        for (int i = 0; i < boxes.length; i++) {
            Integer value = parseHexStrict(boxes[i] == null ? "" : boxes[i].getValue());
            if (value == null) {
                validationError = ChronicleI18n.tr("error.invalid_color", names[i]);
                lastHexValidationError = validationError;
                return false;
            }
            parsed[i] = value;
        }
        draftBackground = parsed[0];
        draftBorder = parsed[1];
        draftAccent = parsed[2];
        draftTitleColor = parsed[3];
        draftMessageColor = parsed[4];
        draftIconColor = parsed[5];
        validationError = null;
        return true;
    }

    private void applyAndClose() {
        if (!readColors()) return;
        if (draftBackgroundImagePath != null && !draftBackgroundImagePath.isBlank()) {
            String imageError = CustomToastBackground.validateFile(draftBackgroundImagePath);
            if (imageError != null) {
                validationError = imageError;
                return;
            }
            if (!CustomToastBackground.prepare(this.minecraft, draftBackgroundImagePath, true)) {
                validationError = CustomToastBackground.getLastError();
                return;
            }
        }

        var config = ChronicleClient.CONFIG;
        String oldStyle = config.toastStyle;
        int oldBackground = config.toastBackgroundColor;
        int oldBorder = config.toastBorderColor;
        int oldAccent = config.toastAccentColor;
        int oldTitleColor = config.toastTitleColor;
        int oldMessageColor = config.toastMessageColor;
        int oldIconColor = config.toastIconColor;
        String oldIcon = config.toastIcon;
        String oldTitle = config.toastTitle;
        float oldTitleScale = config.toastTitleScale;
        float oldMessageScale = config.toastMessageScale;
        float oldIconScale = config.toastIconScale;
        String oldFrameStyle = config.toastFrameStyle;
        String oldBackgroundImagePath = config.toastBackgroundImagePath;
        boolean oldAnimationsEnabled = config.animationsEnabled;
        boolean oldToastActionsEnabled = config.toastActionsEnabled;
        int oldToastSnoozeMinutes = config.toastSnoozeMinutes;

        config.toastStyle = draftStyle;
        config.toastBackgroundColor = draftBackground;
        config.toastBorderColor = draftBorder;
        config.toastAccentColor = draftAccent;
        config.toastTitleColor = draftTitleColor;
        config.toastMessageColor = draftMessageColor;
        config.toastIconColor = draftIconColor;
        config.toastIcon = sanitizeIcon(iconBox.getValue());
        config.toastTitle = sanitizeTitle(titleBox.getValue());
        config.toastTitleScale = draftTitleScale;
        config.toastMessageScale = draftMessageScale;
        config.toastIconScale = draftIconScale;
        config.toastFrameStyle = draftFrameStyle;
        config.toastBackgroundImagePath = draftBackgroundImagePath == null
                ? "" : draftBackgroundImagePath.trim();
        config.animationsEnabled = draftAnimationsEnabled;
        config.toastActionsEnabled = draftToastActionsEnabled;
        config.toastSnoozeMinutes = draftToastSnoozeMinutes;
        config.ensureValid();
        if (config.save()) {
            settingsApplied = true;
            CustomToastBackground.retain(this.minecraft, config.toastBackgroundImagePath);
            onClose();
        } else {
            config.toastStyle = oldStyle;
            config.toastBackgroundColor = oldBackground;
            config.toastBorderColor = oldBorder;
            config.toastAccentColor = oldAccent;
            config.toastTitleColor = oldTitleColor;
            config.toastMessageColor = oldMessageColor;
            config.toastIconColor = oldIconColor;
            config.toastIcon = oldIcon;
            config.toastTitle = oldTitle;
            config.toastTitleScale = oldTitleScale;
            config.toastMessageScale = oldMessageScale;
            config.toastIconScale = oldIconScale;
            config.toastFrameStyle = oldFrameStyle;
            config.toastBackgroundImagePath = oldBackgroundImagePath;
            config.animationsEnabled = oldAnimationsEnabled;
            config.toastActionsEnabled = oldToastActionsEnabled;
            config.toastSnoozeMinutes = oldToastSnoozeMinutes;
            if (oldBackgroundImagePath != null && !oldBackgroundImagePath.isBlank()) {
                CustomToastBackground.prepare(this.minecraft, oldBackgroundImagePath, true);
            }
            CustomToastBackground.retain(this.minecraft, oldBackgroundImagePath);
            validationError = config.getLastSaveError();
        }
    }

    private void previewFromDraft() {
        if (!readColors()) return;
        if (draftBackgroundImagePath != null && !draftBackgroundImagePath.isBlank()
                && !CustomToastBackground.prepare(this.minecraft, draftBackgroundImagePath, true)) {
            validationError = CustomToastBackground.getLastError();
            return;
        }
        long now = Util.getMillis();
        if (lastDraftTestAt != Long.MIN_VALUE && now - lastDraftTestAt < DRAFT_TEST_DEBOUNCE_MS) {
            return;
        }
        lastDraftTestAt = now;
        ReminderToastTheme theme = new ReminderToastTheme(
                draftBackground, draftBorder, draftAccent,
                draftTitleColor, draftMessageColor, draftIconColor
        );
        String icon = sanitizeIcon(iconBox.getValue());
        String title = ChroniclePlaceholders.resolve(sanitizeTitle(titleBox.getValue()));
        int snoozeMinutes = draftToastSnoozeMinutes;
        CustomReminderToast.SnoozeAction action = draftActionsVisible()
                ? () -> ChronicleClient.snoozeReminder(
                ChronicleI18n.tr("toast.preview.reminder"), snoozeMinutes)
                : null;
        this.minecraft.getToasts().addToast(new CustomReminderToast(
                ChronicleI18n.tr("toast.preview.reminder"), title, icon, theme,
                draftTitleScale, draftMessageScale, draftIconScale,
                draftFrameStyle, draftAnimationsEnabled,
                action, snoozeMinutes, null, draftBackgroundImagePath
        ));
        CustomSoundPlayer.playConfigured(this.minecraft, ChronicleClient.CONFIG);
    }

    private void adjustTitleScale(float delta) {
        draftTitleScale = clampScale(draftTitleScale + delta, 1.00f, 2.00f);
        if (titleSize != null) titleSize.setMessage(Component.literal(scaleLabel("title", draftTitleScale)));
    }

    private void adjustMessageScale(float delta) {
        draftMessageScale = clampScale(draftMessageScale + delta, 1.00f, 2.00f);
        if (messageSize != null) messageSize.setMessage(Component.literal(scaleLabel("message", draftMessageScale)));
    }

    private void adjustIconScale(float delta) {
        draftIconScale = clampScale(draftIconScale + delta, 1.00f, 2.00f);
        if (iconSize != null) iconSize.setMessage(Component.literal(scaleLabel("icon", draftIconScale)));
    }

    private static float clampScale(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatScale(float scale) {
        return String.format(java.util.Locale.ROOT, "%.2fx", scale);
    }

    private static String scaleLabel(String type, float scale) {
        return ChronicleI18n.tr("toast.scale." + type, formatScale(scale));
    }

    private static String targetName(String target) {
        return ChronicleI18n.tr("toast.target." + target.toLowerCase(java.util.Locale.ROOT));
    }

    private static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    private static int parseHex(String value, int fallback) {
        try {
            String v = value.trim();
            if (v.startsWith("#")) v = v.substring(1);
            if (v.length() == 6) {
                return 0xFF000000 | Integer.parseInt(v, 16);
            }
            if (v.length() == 8) {
                return (int) Long.parseLong(v, 16);
            }
            return fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Integer parseHexStrict(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.startsWith("#")) v = v.substring(1);
        if (v.length() != 6 && v.length() != 8) return null;
        for (int i = 0; i < v.length(); i++) {
            if (Character.digit(v.charAt(i), 16) < 0) return null;
        }
        try {
            return v.length() == 6
                    ? 0xFF000000 | Integer.parseInt(v, 16)
                    : (int) Long.parseLong(v, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void syncValidDraftColorsFromFields() {
        EditBox[] boxes = {backgroundBox, borderBox, accentBox, titleColorBox, messageColorBox, iconColorBox};
        int[] current = {draftBackground, draftBorder, draftAccent, draftTitleColor, draftMessageColor, draftIconColor};
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] == null) continue;
            Integer value = parseHexStrict(boxes[i].getValue());
            if (value != null) current[i] = value;
        }
        draftBackground = current[0];
        draftBorder = current[1];
        draftAccent = current[2];
        draftTitleColor = current[3];
        draftMessageColor = current[4];
        draftIconColor = current[5];
    }

    private String findFirstInvalidHexField() {
        EditBox[] boxes = {backgroundBox, borderBox, accentBox, titleColorBox, messageColorBox, iconColorBox};
        String[] names = {targetName("BACKGROUND"), targetName("BORDER"), targetName("ACCENT"),
                targetName("TITLE"), targetName("MESSAGE"), targetName("ICON")};
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] != null && parseHexStrict(boxes[i].getValue()) == null) {
                String error = ChronicleI18n.tr("error.invalid_color", names[i]);
                lastHexValidationError = error;
                return error;
            }
        }
        return null;
    }

    private static String sanitizeIcon(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return "!";
        }
        int end = normalized.offsetByCodePoints(0, Math.min(2, normalized.codePointCount(0, normalized.length())));
        return normalized.substring(0, end);
    }

    private static String sanitizeTitle(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? "CHRONICLE" : normalized;
    }

    private String resolvePreviewTitle(String value) {
        String source = sanitizeTitle(value);
        long now = Util.getMillis();
        if (!source.equals(previewTitleSource) || resolvedPreviewTitle == null
                || previewTitleResolvedAt == Long.MIN_VALUE || now - previewTitleResolvedAt >= 250L) {
            previewTitleSource = source;
            resolvedPreviewTitle = ChroniclePlaceholders.resolve(source);
            previewTitleResolvedAt = now;
        }
        return resolvedPreviewTitle;
    }

    private static boolean usesTwoColumnFields(int controlWidth) {
        return controlWidth >= 180;
    }

    private static int preferredPreviewColumnWidth(int panelWidth) {
        return Math.min(340, Math.max(180, Math.round(panelWidth * 0.34f)));
    }

    private static int controlAreaWidth(boolean compact, int usablePanelWidth, int previewColumnWidth) {
        if (compact) return Math.max(1, usablePanelWidth);
        return Math.max(1, Math.min(500,
                usablePanelWidth - previewColumnWidth - UiMetrics.GAP_LG));
    }

    private static boolean usesTwoColumnSizeControls(int controlWidth) {
        return controlWidth >= 252;
    }

    private static int appearanceColumnCount(int controlWidth) {
        if (controlWidth >= 360) return 3;
        if (controlWidth >= 220) return 2;
        return 1;
    }

    private static int appearanceContentShift(boolean compact, int controlWidth) {
        int shift = 55 + UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM;
        int rows = appearanceColumnCount(controlWidth) == 3
                ? 2 : appearanceColumnCount(controlWidth) == 2 ? 3 : 5;
        return shift + (rows - 1) * (UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM);
    }

    private static int colorFieldsBase(boolean compact, int controlWidth) {
        int base = compact ? 296 : 260;
        return usesTwoColumnSizeControls(controlWidth) ? base : base + 24 + UiMetrics.GAP_SM;
    }

    private static int paletteBase(boolean compact, int controlWidth) {
        int fieldsBase = colorFieldsBase(compact, controlWidth);
        return fieldsBase + (usesTwoColumnFields(controlWidth) ? 167 : 326);
    }

    private static boolean usesSideColorWorkspace(boolean compact, int controlWidth) {
        return compact && controlWidth >= 560;
    }

    private static int compactPickerWidth(int controlWidth) {
        if (!usesSideColorWorkspace(true, controlWidth)) {
            return Math.max(1, controlWidth);
        }
        return Math.min(300, Math.max(220, Math.round(controlWidth * 0.36f)));
    }

    private static int paletteAreaWidth(boolean compact, int controlWidth) {
        if (!usesSideColorWorkspace(compact, controlWidth)) {
            return Math.max(1, controlWidth);
        }
        return Math.max(1, controlWidth - compactPickerWidth(controlWidth) - UiMetrics.GAP_LG);
    }

    private static int paletteColumns(int paletteWidth) {
        return paletteWidth >= 300 ? 2 : 1;
    }

    private static int paletteRowsForWidth(int paletteWidth) {
        return paletteColumns(paletteWidth) == 2 ? 4 : 8;
    }

    private static int sideColorWorkspaceHeight(int controlWidth, int pickerHeight) {
        int paletteHeight = 24 + paletteRowsForWidth(paletteAreaWidth(true, controlWidth))
                * (30 + UiMetrics.GAP_SM);
        return Math.max(colorPickerVisualHeight(pickerHeight), paletteHeight);
    }

    private static int colorPickerVisualHeight(int pickerHeight) {
        int squareSize = Math.max(80, pickerHeight - 28);
        return squareSize + 44;
    }

    private static int previewVisualHeight(int screenHeight) {
        return Math.max(72, Math.min(106, CustomReminderToast.responsiveHeight(screenHeight)));
    }

    private static int compactPreviewY(int top) {
        return top + UiMetrics.HEADER_HEIGHT + UiMetrics.GAP_SM + UiMetrics.LABEL_OFFSET;
    }

    private static int compactPreviewFirstShift(int screenHeight) {
        return previewVisualHeight(screenHeight) + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;
    }

    private static int desktopPreviewY(int top) {
        return top + 126;
    }

    private static int desktopPickerLogicalY(int top, int screenHeight) {
        int previewBottom = desktopPreviewY(top) + previewVisualHeight(screenHeight);
        return previewBottom + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;
    }

    private static int compactPickerLogicalY(int top, int contentShift, int controlWidth) {
        int paletteY = top + paletteBase(true, controlWidth) + contentShift;
        if (usesSideColorWorkspace(true, controlWidth)) {
            return paletteY;
        }
        int paletteWidth = paletteAreaWidth(true, controlWidth);
        int cardsBottom = paletteY + 20 + paletteRowsForWidth(paletteWidth)
                * (30 + UiMetrics.GAP_SM);
        return cardsBottom + UiMetrics.GAP_MD + UiMetrics.LABEL_OFFSET;
    }

    private int reservedErrorStripHeight() {
        if (validationError == null) return 0;
        int panelW = UiMetrics.panelWidth(this.width);
        boolean compact = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        int desired = this.font.lineHeight + UiMetrics.GAP_SM;
        return compact ? desired : 0;
    }

    private static int footerAreaHeight(int panelW, boolean compact) {
        if (!compact) return UiMetrics.FOOTER_AREA_HEIGHT;
        return panelW < 260 ? 128 : 56;
    }

    private int maxScrollAmount() {
        int panelW = UiMetrics.panelWidth(this.width);
        int top = UiMetrics.panelTop(this.height);
        boolean compactStyles = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        if (!compactStyles) {
            return 0;
        }
        int controlW;
        int previewW = preferredPreviewColumnWidth(panelW);
        int sideInset = UiMetrics.contentInset(panelW);
        int usablePanelW = Math.max(1, panelW - sideInset * 2);
        controlW = controlAreaWidth(compactStyles, usablePanelW, previewW);
        int contentShift = UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM
                + compactPreviewFirstShift(this.height)
                + appearanceContentShift(true, controlW);
        int contentBottom;
        if (advancedColorsVisible) {
            int pickerHeight = 150;
            int pickerY = compactPickerLogicalY(top, contentShift, controlW);
            int workspaceHeight = usesSideColorWorkspace(true, controlW)
                    ? sideColorWorkspaceHeight(controlW, pickerHeight)
                    : colorPickerVisualHeight(pickerHeight);
            contentBottom = pickerY + workspaceHeight + UiMetrics.GAP_MD;
        } else {
            contentBottom = top + colorFieldsBase(true, controlW) + contentShift;
        }
        int footerHeight = footerAreaHeight(panelW, true);
        int footerTop = this.height - footerHeight - reservedErrorStripHeight();
        return Math.max(0, contentBottom - (footerTop - 12));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        boolean compactStyles = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        if (!compactStyles) {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }

        int footerHeight = footerAreaHeight(panelW, compactStyles);
        int clipTop = top + UiMetrics.HEADER_HEIGHT;
        int clipBottom = Math.min(this.height,
                this.height - footerHeight - reservedErrorStripHeight());
        boolean insideContent = mouseX >= left && mouseX < left + panelW
                && mouseY >= clipTop && mouseY < clipBottom;
        int max = maxScrollAmount();
        if (!insideContent || max <= 0 || Math.abs(amount) < 1.0E-9) {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }

        int direction = amount < 0 ? 1 : -1;
        int next = Math.max(0, Math.min(max,
                scrollOffset + direction * (UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM)));
        if (next != scrollOffset) {
            scrollOffset = next;
            applyScrollPositions();
        }
        return true;
    }

    private void captureScrollBasePositions() {
        scrollBaseY.clear();
        for (var child : this.children()) {
            if (child instanceof AbstractWidget widget
                    && widget != applyButton && widget != testButton && widget != cancelButton) {
                scrollBaseY.put(widget, widget.getY());
            }
        }
    }

    private void applyScrollPositions() {
        for (var entry : scrollBaseY.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget == null) continue;
            widget.setY(entry.getValue() - scrollOffset);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = Util.getMillis();
        transition.begin(graphics, this.width, this.height);
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        boolean compactStyles = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        int scroll;

        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);
        int panelBottom = Math.max(top + 1, this.height - 12);
        UiFrame.drawWindow(graphics, left, top, panelW, panelBottom);

        int sideInset = UiMetrics.contentInset(panelW);
        int headerTextX = left + sideInset;
        int headerTextWidth = Math.max(1, left + panelW - sideInset - headerTextX);
        String headerTitle = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("toast.header"), headerTextWidth);
        graphics.drawString(this.font, Component.literal(headerTitle), headerTextX,
                UiMetrics.headerTitleY(top, false), 0xFFE7ECF2, true);
        String subtitle = trimToWidth(ChronicleI18n.tr("toast.subtitle"),
                headerTextWidth);
        graphics.drawString(this.font, Component.literal(subtitle),
                headerTextX, UiMetrics.headerSubtitleY(top, false), MUTED, false);
        UiFrame.drawInsetDivider(graphics, left, panelW, sideInset,
                UiMetrics.headerDividerY(top, UiMetrics.HEADER_HEIGHT));
        int contentClipTop = Math.max(0, Math.min(Math.max(0, this.height - 1),
                top + UiMetrics.HEADER_HEIGHT));
        int contentFooterHeight = footerAreaHeight(panelW, compactStyles);
        int previousTargetColor = getTargetColor();
        syncValidDraftColorsFromFields();
        if (getTargetColor() != previousTargetColor) {
            updatePickerFromTarget();
        }
        String fieldError = findFirstInvalidHexField();
        if (fieldError != null) {
            validationError = fieldError;
        } else if (validationError != null && validationError.equals(lastHexValidationError)) {
            validationError = null;
            lastHexValidationError = null;
        }
        int baseContentClipBottom = Math.max(contentClipTop + 1,
                Math.min(this.height, this.height - contentFooterHeight));
        int errorStripHeight = reservedErrorStripHeight();
        int contentClipBottom = Math.max(contentClipTop + 1, baseContentClipBottom - errorStripHeight);
        if (compactStyles) {
            int clampedScroll = Math.max(0, Math.min(scrollOffset, maxScrollAmount()));
            if (clampedScroll != scrollOffset) {
                scrollOffset = clampedScroll;
                applyScrollPositions();
            }
        }
        scroll = compactStyles ? scrollOffset : 0;
        graphics.enableScissor(left, contentClipTop, left + panelW, contentClipBottom);
        int controlsLeft = left + sideInset;
        int previewFirstShift = compactStyles ? compactPreviewFirstShift(this.height) : 0;
        drawContentLabel(graphics, ChronicleI18n.tr("toast.section.style"), controlsLeft,
                top + UiMetrics.HEADER_HEIGHT + UiMetrics.GAP_SM + previewFirstShift - scroll,
                MUTED, contentClipTop, contentClipBottom);
        int previewColumnW = preferredPreviewColumnWidth(panelW);
        int columnGap = UiMetrics.GAP_LG;
        int usablePanelW = Math.max(1, panelW - sideInset * 2);
        int controlW = controlAreaWidth(compactStyles, usablePanelW, previewColumnW);
        int rightColumnW = compactStyles ? controlW : Math.max(1, usablePanelW - controlW - columnGap);
        int rightColumnX = controlsLeft + controlW + columnGap;
        boolean twoColumnFields = usesTwoColumnFields(controlW);
        int boxW = twoColumnFields ? Math.max(1, (controlW - 16) / 2) : controlW;
        int contentShift = compactStyles
                ? UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM + previewFirstShift
                : 0;
        contentShift += appearanceContentShift(compactStyles, controlW);
        int col2 = twoColumnFields ? controlsLeft + boxW + 16 : controlsLeft;
        boolean sideColorWorkspace = usesSideColorWorkspace(compactStyles, controlW);
        if (frameStyleButton != null && animationsButton != null && actionsButton != null) {
            int appearanceLabelY = Math.min(frameStyleButton.getY(),
                    Math.min(animationsButton.getY(), actionsButton.getY()))
                    - UiMetrics.LABEL_OFFSET;
            drawContentLabel(graphics, ChronicleI18n.tr("toast.section.look_motion"), controlsLeft, appearanceLabelY,
                    MUTED, contentClipTop, contentClipBottom);
        }
        int iconTitleLabelY = Math.min(iconBox.getY(), titleBox.getY()) - UiMetrics.LABEL_OFFSET;
        drawContentLabel(graphics, ChronicleI18n.tr("toast.section.icon_title"), controlsLeft, iconTitleLabelY,
                MUTED, contentClipTop, contentClipBottom);
        int paletteBase = paletteBase(compactStyles, controlW);
        int paletteLogicalY = top + paletteBase + contentShift;
        int paletteAreaX = sideColorWorkspace
                ? controlsLeft + compactPickerWidth(controlW) + UiMetrics.GAP_LG
                : controlsLeft;
        int paletteLabelY = paletteLogicalY - UiMetrics.LABEL_OFFSET
                - (sideColorWorkspace ? 0 : UiMetrics.GAP_XS) - scroll;
        if (advancedColorsVisible) {
            drawFieldLabel(graphics, targetName("BACKGROUND"), backgroundBox, controlsLeft,
                    MUTED, contentClipTop, contentClipBottom);
            drawFieldLabel(graphics, targetName("BORDER"), borderBox, col2,
                    MUTED, contentClipTop, contentClipBottom);
            drawFieldLabel(graphics, targetName("ACCENT"), accentBox, controlsLeft,
                    MUTED, contentClipTop, contentClipBottom);
            drawFieldLabel(graphics, targetName("TITLE"), titleColorBox, col2,
                    MUTED, contentClipTop, contentClipBottom);
            drawFieldLabel(graphics, targetName("MESSAGE"), messageColorBox, controlsLeft,
                    MUTED, contentClipTop, contentClipBottom);
            drawFieldLabel(graphics, targetName("ICON"), iconColorBox, col2,
                    MUTED, contentClipTop, contentClipBottom);
            drawContentLabel(graphics, ChronicleI18n.tr("toast.section.palette"), paletteAreaX,
                    paletteLabelY, MUTED, contentClipTop, contentClipBottom);
        }

        updateContentWidgetVisibility(contentClipTop, contentClipBottom);
        boolean applyVisible = applyButton != null && applyButton.visible;
        boolean testVisible = testButton != null && testButton.visible;
        boolean cancelVisible = cancelButton != null && cancelButton.visible;
        if (applyButton != null) applyButton.visible = false;
        if (testButton != null) testButton.visible = false;
        if (cancelButton != null) cancelButton.visible = false;

        if (advancedColorsVisible) {
            int pickerW = compactStyles
                    ? compactPickerWidth(controlW)
                    : Math.max(1, Math.min(350, rightColumnW));
            int pickerH = compactStyles ? 150 : 165;
            int pickerX = compactStyles
                    ? controlsLeft
                    : rightColumnX + Math.max(0, (rightColumnW - pickerW) / 2);
            int pickerLogicalY = compactStyles
                    ? compactPickerLogicalY(top, contentShift, controlW)
                    : desktopPickerLogicalY(top, this.height);
            int pickerY = pickerLogicalY - scroll;
            drawColorPicker(graphics, pickerX, pickerY, pickerW, pickerH);
            if ("VANILLA".equals(draftFrameStyle)) {
                graphics.fill(pickerX - 1, pickerY - 1, pickerX + pickerW + 1,
                        pickerY + colorPickerVisualHeight(pickerH), 0xB0090C11);
            }
            int pickerArgb = (getTargetColor() & 0xFF000000)
                    | (colorFromHsv(pickerHue, pickerSaturation, pickerValue) & 0x00FFFFFF);
            String pickerHex = hex(pickerArgb);
            int pickerHexWidth = this.font.width(pickerHex);
            int pickerHexX = pickerX + Math.max(0, pickerW - pickerHexWidth);
            int pickerLabelWidth = Math.max(0, pickerHexX - pickerX - UiMetrics.GAP_SM);
            if (pickerLabelWidth > 0) {
                String pickerLabel = trimToWidth(ChronicleI18n.tr("toast.color_picker",
                        targetName(paletteTarget)), pickerLabelWidth);
                drawContentLabel(graphics, pickerLabel, pickerX,
                        pickerY - UiMetrics.LABEL_OFFSET, 0xFF8995A4,
                        contentClipTop, contentClipBottom);
            }
            drawContentLabel(graphics, pickerHex, pickerHexX,
                    pickerY - UiMetrics.LABEL_OFFSET, 0xFF8FB3E8,
                    contentClipTop, contentClipBottom);
        }

        String previewMessage = ChronicleI18n.tr("toast.preview.reminder");
        String previewIcon = sanitizeIcon(iconBox == null ? ChronicleClient.CONFIG.toastIcon : iconBox.getValue());
        String previewTitle = resolvePreviewTitle(
                titleBox == null ? ChronicleClient.CONFIG.toastTitle : titleBox.getValue());
        int previewAreaW = compactStyles ? controlW : rightColumnW;
        int naturalPreviewW = CustomReminderToast.layoutWidth(this.font, previewTitle, previewMessage,
                draftFrameStyle, this.width, draftActionsVisible());
        int logicalPreviewW = Math.max(1, Math.min(naturalPreviewW, previewAreaW));
        int logicalPreviewH = CustomReminderToast.layoutHeight(this.font, previewMessage,
                draftFrameStyle, logicalPreviewW, this.height, draftActionsVisible());
        int previewW = logicalPreviewW;
        int px = compactStyles
                ? controlsLeft + Math.max(0, (controlW - previewW) / 2)
                : rightColumnX + Math.max(0, (rightColumnW - previewW) / 2);
        int py = compactStyles
                ? compactPreviewY(top) - scroll
                : desktopPreviewY(top);
        String previewLabel = trimToWidth("VANILLA".equals(draftFrameStyle)
                        ? ChronicleI18n.tr("toast.preview.vanilla")
                        : ChronicleI18n.tr(compactStyles ? "toast.preview.live_instant" : "toast.preview.live"),
                Math.max(1, previewW));
        drawContentLabel(graphics, previewLabel, px, py - UiMetrics.LABEL_OFFSET,
                ACCENT, contentClipTop, contentClipBottom);

        int bg = parseHex(backgroundBox == null ? hex(draftBackground) : backgroundBox.getValue(), draftBackground);
        int border = parseHex(borderBox == null ? hex(draftBorder) : borderBox.getValue(), draftBorder);
        int previewAccent = parseHex(accentBox == null ? hex(draftAccent) : accentBox.getValue(), draftAccent);
        int titleColor = parseHex(titleColorBox == null ? hex(draftTitleColor) : titleColorBox.getValue(), draftTitleColor);
        int messageColor = parseHex(messageColorBox == null ? hex(draftMessageColor) : messageColorBox.getValue(), draftMessageColor);
        int iconColor = parseHex(iconColorBox == null ? hex(draftIconColor) : iconColorBox.getValue(), draftIconColor);
        renderPreviewAt(graphics, px, py, logicalPreviewW, logicalPreviewH,
                previewMessage, previewTitle, previewIcon,
                new ReminderToastTheme(bg, border, previewAccent, titleColor, messageColor, iconColor),
                draftTitleScale, draftMessageScale, draftIconScale);

        for (EditBox box : java.util.List.of(iconBox, titleBox, backgroundBox, borderBox,
                accentBox, titleColorBox, messageColorBox, iconColorBox)) {
            if (box != null && box.visible) {
                drawField(graphics, box, mouseX, mouseY);
            }
        }

        super.render(graphics, mouseX, mouseY, delta);
        for (var widget : this.children()) {
            if (widget instanceof Button button && button.visible) {
                drawCustomizedButton(graphics, button, mouseX, mouseY);
            }
        }
        if (!isTopLayerButton(lastPressedButton)) {
            drawPressPulse(graphics, now);
        }

        int maxScroll = compactStyles ? maxScrollAmount() : 0;
        if (maxScroll > 0) {
            int viewportHeight = Math.max(1, contentClipBottom - contentClipTop);
            UiFrame.drawScrollBar(graphics, left + panelW - 10,
                    contentClipTop + UiMetrics.GAP_XS, contentClipBottom - UiMetrics.GAP_XS,
                    scrollOffset / (float) maxScroll,
                    viewportHeight / (float) (viewportHeight + maxScroll));
        }

        graphics.disableScissor();

        UiFrame.drawInsetDivider(graphics, left, panelW, sideInset, baseContentClipBottom);

        if (applyButton != null) applyButton.visible = applyVisible;
        if (testButton != null) testButton.visible = testVisible;
        if (cancelButton != null) cancelButton.visible = cancelVisible;

        if (validationError != null) {
            int errorY = compactStyles
                    ? Math.max(contentClipTop,
                    Math.min(baseContentClipBottom - this.font.lineHeight - 2,
                            contentClipBottom + UiMetrics.GAP_XS))
                    : baseContentClipBottom + 2;
            String shownError = trimToWidth(validationError, Math.max(1, panelW - sideInset * 2));
            graphics.drawString(this.font, Component.literal(shownError), controlsLeft, errorY, 0xFFD69A9A, false);
        }

        for (var widget : this.children()) {
            if (widget instanceof Button button && button.visible && isTopLayerButton(button)) {
                drawCustomizedButton(graphics, button, mouseX, mouseY);
            }
        }

        if (isTopLayerButton(lastPressedButton)) {
            drawPressPulse(graphics, now);
        }

        transition.end(graphics, this.width, this.height);

    }

    private boolean isTopLayerButton(Button button) {
        return button != null && (button == applyButton || button == testButton || button == cancelButton);
    }

    private void drawCustomizedButton(GuiGraphics graphics, Button button,
                                      int mouseX, int mouseY) {
        String style = styleButtonValues.get(button);
        String paletteName = paletteButtonValues.get(button);
        boolean activeToggle = button == animationsButton && draftAnimationsEnabled
                || button == actionsButton && draftActionsVisible()
                || button == backgroundImageButton
                && !"VANILLA".equals(draftFrameStyle)
                && draftBackgroundImagePath != null && !draftBackgroundImagePath.isBlank()
                || button == advancedColorsButton && advancedColorsVisible;
        int buttonAccent = button == applyButton || button == testButton ? ACCENT
                : button == cancelButton ? ACCENT_ALT
                : activeToggle ? ACCENT
                : style != null && styleMatches(style) ? ACCENT : 0xFF4A566A;
        if (paletteName != null) {
            drawPaletteCardButton(graphics, button, button.getMessage().getString(), paletteName, mouseX, mouseY);
        } else {
            boolean emphasized = button == applyButton || activeToggle
                    || (style != null && styleMatches(style));
            UiFrame.drawButton(graphics, this.font, button, buttonAccent, emphasized, mouseX, mouseY);
        }
    }

    private void drawPressPulse(GuiGraphics graphics, long now) {
        if (lastPressedAt < 0L) return;
        float pulse = UiAnimation.pressProgress(lastPressedAt, now, 190L);
        if (pulse <= 0.0f) return;
        int inset = UiAnimation.pressInset(pulse);
        int alpha = Math.max(0, Math.min(90, Math.round(pulse * 90.0f)));
        graphics.fill(lastPressedX - inset, lastPressedY - inset,
                lastPressedX + lastPressedW + inset, lastPressedY + lastPressedH + inset,
                (alpha << 24) | 0x009DB7D4);
    }

    private void drawContentLabel(GuiGraphics graphics, String text, int x, int y, int color,
                                  int clipTop, int clipBottom) {
        if (y + this.font.lineHeight < clipTop || y > clipBottom) return;
        int panelW = UiMetrics.panelWidth(this.width);
        int panelRight = UiMetrics.panelLeft(this.width, panelW) + panelW - UiMetrics.contentInset(panelW);
        String shown = trimToWidth(text, Math.max(1, panelRight - x));
        graphics.drawString(this.font, Component.literal(shown), x, y, color, false);
    }

    private void drawFieldLabel(GuiGraphics graphics, String text, EditBox box, int x, int color,
                                int clipTop, int clipBottom) {
        if (box == null || !box.visible) return;
        int y = box.getY() - UiMetrics.LABEL_OFFSET;
        drawContentLabel(graphics, trimToWidth(text, Math.max(1, box.getWidth())),
                x, y, color, clipTop, clipBottom);
    }

    private void updateContentWidgetVisibility(int clipTop, int clipBottom) {
        for (var child : children()) {
            if (!(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)) continue;
            if (!advancedColorsVisible && isAdvancedColorWidget(widget)) {
                widget.visible = false;
                if (getFocused() == widget) clearFocus();
                continue;
            }
            if (child instanceof Button button) {
                if (button == applyButton || button == testButton || button == cancelButton) {
                    widget.visible = true;
                    continue;
                }
            }
            boolean visibleInViewport = widget.getY() + widget.getHeight() > clipTop
                    && widget.getY() < clipBottom;
            widget.visible = visibleInViewport;
            if (!visibleInViewport && getFocused() == widget) {
                clearFocus();
            }
        }
    }

    private void renderPreviewAt(GuiGraphics graphics, int x, int y, int width, int height,
                                 String message, String title, String icon, ReminderToastTheme theme,
                                 float titleScale, float messageScale, float iconScale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        CustomReminderToast.renderPreview(
                graphics, this.font, width, height, message, title, icon, theme,
                titleScale, messageScale, iconScale, draftFrameStyle,
                draftActionsVisible(), draftToastSnoozeMinutes,
                draftBackgroundImagePath
        );
        graphics.pose().popPose();
    }

    private void drawField(GuiGraphics graphics, EditBox box, int mouseX, int mouseY) {
        boolean hovered = box.active && mouseX >= box.getX() && mouseX < box.getX() + box.getWidth()
                && mouseY >= box.getY() && mouseY < box.getY() + box.getHeight();
        int border = hovered || (box.active && box.isFocused()) ? 0xFF3A4655 : 0xFF252E3A;
        graphics.fill(box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight(), border);
        graphics.fill(box.getX() + 1, box.getY() + 1, box.getX() + box.getWidth() - 1,
                box.getY() + box.getHeight() - 1, box.active ? 0xFF0F141A : 0xFF0C1015);
    }

    private int getTargetColor() {
        return switch (paletteTarget) {
            case "BORDER" -> draftBorder;
            case "ACCENT" -> draftAccent;
            case "TITLE" -> draftTitleColor;
            case "MESSAGE" -> draftMessageColor;
            case "ICON" -> draftIconColor;
            default -> draftBackground;
        };
    }

    private void selectPaletteTargetFromFieldClick(double mouseX, double mouseY) {
        EditBox[] boxes = {backgroundBox, borderBox, accentBox, titleColorBox, messageColorBox, iconColorBox};
        String[] targets = {"BACKGROUND", "BORDER", "ACCENT", "TITLE", "MESSAGE", "ICON"};
        for (int i = 0; i < boxes.length; i++) {
            EditBox box = boxes[i];
            if (box == null || !box.visible || !box.active) continue;
            if (mouseX >= box.getX() && mouseX < box.getX() + box.getWidth()
                    && mouseY >= box.getY() && mouseY < box.getY() + box.getHeight()) {
                syncValidDraftColorsFromFields();
                paletteTarget = targets[i];
                updatePickerFromTarget();
                if (paletteTargetButton != null) {
                    paletteTargetButton.setMessage(Component.literal(paletteTargetLabel()));
                }
                return;
            }
        }
    }

    private void updatePickerFromTarget() {
        int color = getTargetColor();
        float[] hsv = rgbToHsv((color >> 16) & 255, (color >> 8) & 255, color & 255);
        pickerHue = hsv[0];
        pickerSaturation = hsv[1];
        pickerValue = hsv[2];
    }

    private void updateTargetFromPicker() {
        int source = getTargetColor();
        int alpha = (source >>> 24) & 255;
        int rgb = colorFromHsv(pickerHue, pickerSaturation, pickerValue) & 0x00FFFFFF;
        applyColorToTarget((alpha << 24) | rgb);
    }

    private int colorFromHsv(float hue, float saturation, float value) {
        float h = (hue - (float)Math.floor(hue)) * 6.0f;
        int sector = (int)Math.floor(h);
        float f = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * f);
        float t = value * (1.0f - saturation * (1.0f - f));
        float r, g, b;
        switch (sector) {
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            case 5 -> { r = value; g = p; b = q; }
            default -> { r = value; g = t; b = p; }
        }
        return ((Math.round(r * 255.0f) & 255) << 16)
                | ((Math.round(g * 255.0f) & 255) << 8)
                | (Math.round(b * 255.0f) & 255);
    }

    private float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h = pickerHue;
        if (d > 0.00001f) {
            if (max == rf) h = ((gf - bf) / d) % 6.0f;
            else if (max == gf) h = ((bf - rf) / d) + 2.0f;
            else h = ((rf - gf) / d) + 4.0f;
            h /= 6.0f;
            if (h < 0.0f) h += 1.0f;
        }
        float saturation = max <= 0.00001f ? 0.0f : d / max;
        return new float[]{h, saturation, max};
    }

    private void drawColorPicker(GuiGraphics graphics, int x, int y, int width, int height) {
        int squareSize = Math.max(80, height - 28);
        int squareW = Math.min(width, squareSize + 18);
        int hueY = y + squareSize + 10;

        graphics.fill(x - 1, y - 1, x + squareW + 1, y + squareSize + 1, 0xFF394866);

        int selectedRgb = colorFromHsv(pickerHue, pickerSaturation, pickerValue);
        int selectedArgb = (getTargetColor() & 0xFF000000) | selectedRgb;
        for (int col = 0; col < squareW; col++) {
            float sat = col / (float)Math.max(1, squareW - 1);
            int rgb = colorFromHsv(pickerHue, sat, 1.0f);
            graphics.fill(x + col, y, x + col + 1, y + squareSize, 0xFF000000 | rgb);
        }
        for (int row = 0; row < squareSize; row++) {
            int alpha = Math.round(255.0f * row / (float)Math.max(1, squareSize - 1));
            graphics.fill(x, y + row, x + squareW, y + row + 1, (alpha << 24));
        }

        graphics.fill(x - 1, hueY - 1, x + width + 1, hueY + 11, 0xFF394866);
        for (int i = 0; i < width; i++) {
            float hue = i / (float)Math.max(1, width - 1);
            int rgb = colorFromHsv(hue, 1.0f, 1.0f);
            graphics.fill(x + i, hueY, x + i + 1, hueY + 10, 0xFF000000 | rgb);
        }

        int svX = x + Math.round(pickerSaturation * Math.max(1, squareW - 1));
        int svY = y + Math.round((1.0f - pickerValue) * Math.max(1, squareSize - 1));
        drawCrossHandle(graphics, svX, svY, x, y, squareW, squareSize);

        int hueX = width >= 5
                ? x + 2 + Math.round(pickerHue * (width - 5))
                : x + Math.max(0, width / 2);
        drawHueHandle(graphics, hueX, hueY + 5);

        int swatchW = Math.max(1, Math.min(48, width));
        int swatchX = x + Math.max(0, width - swatchW);
        graphics.fill(swatchX, hueY + 15, swatchX + swatchW, hueY + 34, 0xFF394866);
        if (swatchW > 4) {
            int innerLeft = swatchX + 2;
            int innerTop = hueY + 17;
            int innerRight = swatchX + swatchW - 2;
            int innerBottom = hueY + 32;
            for (int checkerY = innerTop; checkerY < innerBottom; checkerY += 4) {
                for (int checkerX = innerLeft; checkerX < innerRight; checkerX += 4) {
                    boolean light = (((checkerX - innerLeft) / 4) + ((checkerY - innerTop) / 4)) % 2 == 0;
                    graphics.fill(checkerX, checkerY, Math.min(innerRight, checkerX + 4),
                            Math.min(innerBottom, checkerY + 4), light ? 0xFFB8BEC7 : 0xFF68717D);
                }
            }
            graphics.fill(innerLeft, innerTop, innerRight, innerBottom, selectedArgb);
        }
    }

    private void drawCrossHandle(GuiGraphics graphics, int cx, int cy, int originX, int originY, int width, int height) {
        int maxArm = Math.max(0, (Math.min(width, height) - 1) / 2);
        if (maxArm < 2) {
            int x = Math.max(originX, Math.min(originX + Math.max(0, width - 1), cx));
            int y = Math.max(originY, Math.min(originY + Math.max(0, height - 1), cy));
            graphics.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
            return;
        }
        int arm = Math.min(7, maxArm);
        int x = Math.max(originX + arm, Math.min(originX + width - arm - 1, cx));
        int y = Math.max(originY + arm, Math.min(originY + height - arm - 1, cy));
        graphics.fill(x - arm, y - 1, x + arm + 1, y + 2, 0xCC000000);
        graphics.fill(x - 1, y - arm, x + 2, y + arm + 1, 0xCC000000);
        graphics.fill(x - arm + 1, y, x + arm, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y - arm + 1, x + 1, y + arm, 0xFFFFFFFF);
    }

    private void drawHueHandle(GuiGraphics graphics, int cx, int cy) {
        graphics.fill(cx - 2, cy - 7, cx + 3, cy + 8, 0xCC000000);
        graphics.fill(cx - 1, cy - 6, cx + 2, cy + 7, 0xFFFFFFFF);
    }

    private int pickerHitMode(double mouseX, double mouseY) {
        if (!advancedColorsVisible || "VANILLA".equals(draftFrameStyle)) return 0;
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        boolean compact = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        int clipTop = top + UiMetrics.HEADER_HEIGHT;
        int footerHeight = footerAreaHeight(panelW, compact);
        int clipBottom = Math.max(clipTop + 1,
                Math.min(this.height, this.height - footerHeight - reservedErrorStripHeight()));
        if (mouseX < left || mouseX >= left + panelW || mouseY < clipTop || mouseY >= clipBottom) {
            return 0;
        }
        int previewColumnW = preferredPreviewColumnWidth(panelW);
        int sideInset = UiMetrics.contentInset(panelW);
        int controlsLeft = left + sideInset;
        int usablePanelW = Math.max(1, panelW - sideInset * 2);
        int controlW = controlAreaWidth(compact, usablePanelW, previewColumnW);
        int rightColumnW = compact ? controlW
                : Math.max(1, usablePanelW - controlW - UiMetrics.GAP_LG);
        int rightColumnX = controlsLeft + controlW + UiMetrics.GAP_LG;
        int pickerW = compact
                ? compactPickerWidth(controlW)
                : Math.max(1, Math.min(350, rightColumnW));
        int pickerH = compact ? 150 : 165;
        int contentShift = compact
                ? UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM + compactPreviewFirstShift(this.height)
                + appearanceContentShift(true, controlW)
                : appearanceContentShift(false, controlW);
        int pickerX = compact
                ? controlsLeft
                : rightColumnX + Math.max(0, (rightColumnW - pickerW) / 2);
        int pickerY = compact
                ? compactPickerLogicalY(top, contentShift, controlW) - scrollOffset
                : desktopPickerLogicalY(top, this.height);
        int squareSize = Math.max(80, pickerH - 28);
        int squareW = Math.min(pickerW, squareSize + 18);
        int hueY = pickerY + squareSize + 10;
        if (mouseX >= pickerX && mouseX < pickerX + squareW
                && mouseY >= pickerY && mouseY < pickerY + squareSize) {
            return 1;
        }
        if (mouseX >= pickerX && mouseX < pickerX + pickerW
                && mouseY >= hueY - 6 && mouseY <= hueY + 16) {
            return 2;
        }
        return 0;
    }

    private boolean updatePickerFromMouse(double mouseX, double mouseY, boolean allowOutside) {
        if (!advancedColorsVisible || "VANILLA".equals(draftFrameStyle)) return false;
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        boolean compact = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        int previewColumnW = preferredPreviewColumnWidth(panelW);
        int sideInset = UiMetrics.contentInset(panelW);
        int controlsLeft = left + sideInset;
        int usablePanelW = Math.max(1, panelW - sideInset * 2);
        int controlW = controlAreaWidth(compact, usablePanelW, previewColumnW);
        int rightColumnW = compact ? controlW
                : Math.max(1, usablePanelW - controlW - UiMetrics.GAP_LG);
        int rightColumnX = controlsLeft + controlW + UiMetrics.GAP_LG;
        int pickerW = compact
                ? compactPickerWidth(controlW)
                : Math.max(1, Math.min(350, rightColumnW));
        int pickerH = compact ? 150 : 165;
        int contentShift = compact
                ? UiMetrics.CONTROL_HEIGHT + UiMetrics.GAP_SM + compactPreviewFirstShift(this.height)
                + appearanceContentShift(true, controlW)
                : appearanceContentShift(false, controlW);
        int scroll = scrollOffset;
        int pickerX = compact
                ? controlsLeft
                : rightColumnX + Math.max(0, (rightColumnW - pickerW) / 2);
        int pickerY = compact
                ? compactPickerLogicalY(top, contentShift, controlW) - scroll
                : desktopPickerLogicalY(top, this.height);
        int squareSize = Math.max(80, pickerH - 28);
        int squareW = Math.min(pickerW, squareSize + 18);
        int hueY = pickerY + squareSize + 10;

        boolean inSquare = mouseX >= pickerX && mouseX < pickerX + squareW
                && mouseY >= pickerY && mouseY < pickerY + squareSize;
        boolean inHue = mouseX >= pickerX && mouseX < pickerX + pickerW
                && mouseY >= hueY - 6 && mouseY <= hueY + 16;

        if (!allowOutside && !inSquare && !inHue) {
            return false;
        }

        if (allowOutside && draggingColorPicker) {
            float sat = (float)((mouseX - pickerX) / Math.max(1, squareW - 1));
            float val = 1.0f - (float)((mouseY - pickerY) / Math.max(1, squareSize - 1));
            pickerSaturation = Math.max(0.0f, Math.min(1.0f, sat));
            pickerValue = Math.max(0.0f, Math.min(1.0f, val));
            updateTargetFromPicker();
            return true;
        }
        if (allowOutside && draggingHue) {
            pickerHue = Math.max(0.0f, Math.min(1.0f,
                    (float)((mouseX - pickerX) / Math.max(1, pickerW - 1))));
            updateTargetFromPicker();
            return true;
        }
        if (inSquare && !allowOutside) {
            float sat = (float)((mouseX - pickerX) / Math.max(1, squareW - 1));
            float val = 1.0f - (float)((mouseY - pickerY) / Math.max(1, squareSize - 1));
            pickerSaturation = Math.max(0.0f, Math.min(1.0f, sat));
            pickerValue = Math.max(0.0f, Math.min(1.0f, val));
            updateTargetFromPicker();
            return true;
        }
        if (inHue) {
            pickerHue = Math.max(0.0f, Math.min(1.0f,
                    (float)((mouseX - pickerX) / Math.max(1, pickerW - 1))));
            updateTargetFromPicker();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (clickButton(applyButton, mouseX, mouseY, button)
                || clickButton(testButton, mouseX, mouseY, button)
                || clickButton(cancelButton, mouseX, mouseY, button)) {
            return true;
        }
        int mode = pickerHitMode(mouseX, mouseY);
        if (mode == 1) {
            clearFocus();
            draggingColorPicker = true;
            draggingHue = false;
            updatePickerFromMouse(mouseX, mouseY, true);
            return true;
        }
        if (mode == 2) {
            clearFocus();
            draggingColorPicker = false;
            draggingHue = true;
            updatePickerFromMouse(mouseX, mouseY, true);
            return true;
        }
        if (!isInsideContentViewport(mouseX, mouseY)) {
            return false;
        }
        selectPaletteTargetFromFieldClick(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInsideContentViewport(double mouseX, double mouseY) {
        int panelW = UiMetrics.panelWidth(this.width);
        int left = UiMetrics.panelLeft(this.width, panelW);
        int top = UiMetrics.panelTop(this.height);
        boolean compact = panelW < 760 || this.height < DESKTOP_LAYOUT_MIN_HEIGHT;
        int footerHeight = footerAreaHeight(panelW, compact);
        int clipTop = top + UiMetrics.HEADER_HEIGHT;
        int clipBottom = Math.max(clipTop + 1,
                Math.min(this.height, this.height - footerHeight - reservedErrorStripHeight()));
        return mouseX >= left && mouseX < left + panelW
                && mouseY >= clipTop && mouseY < clipBottom;
    }

    private static boolean clickButton(Button button, double mouseX, double mouseY, int mouseButton) {
        return button != null && button.visible
                && mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight()
                && button.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && (draggingColorPicker || draggingHue)) {
            updatePickerFromMouse(mouseX, mouseY, true);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }


    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (draggingColorPicker || draggingHue) {
            updatePickerFromMouse(mouseX, mouseY, true);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && (draggingColorPicker || draggingHue)) {
            draggingColorPicker = false;
            draggingHue = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0) return "";
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

    private void drawPaletteCardButton(GuiGraphics graphics, Button button, String label, String paletteName,
                                       int mouseX, int mouseY) {
        int x = button.getX();
        int y = button.getY();
        int w = button.getWidth();
        int h = button.getHeight();
        boolean hovered = button.active
                && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        boolean focused = button.active && button.isFocused() && !hovered
                && this.minecraft != null && this.minecraft.getLastInputType().isKeyboard();
        int color = switch (paletteName) {
            case "ocean" -> 0xFF0B2034;
            case "midnight" -> 0xFF090D18;
            case "slate" -> 0xFF171B2A;
            case "forest" -> 0xFF07130D;
            case "teal" -> 0xFF102D3A;
            case "plum" -> 0xFF24142A;
            case "amber" -> 0xFF2A2010;
            default -> 0xFF2B0D18;
        };
        boolean selected = button.active && getTargetColor() == color;
        int border = hovered || selected ? ACCENT : 0xFF252E3A;
        graphics.fill(x, y, x + w, y + h, border);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1,
                selected ? UiFrame.ACCENT_SOFT : 0xFF10151C);
        if (focused && w > 6 && h > 6) {
            graphics.fill(x + 2, y + 2, x + w - 2, y + 3, ACCENT);
            graphics.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, ACCENT);
        }
        if (w < 38) {
            if (w > 4 && h > 4) {
                graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, color);
            }
            return;
        }
        int swatchRight = Math.min(x + 27, x + w - 5);
        graphics.fill(x + 7, y + 6, swatchRight, y + h - 6, color);
        int labelX = Math.min(x + 36, x + w - 2);
        String shownLabel = trimToWidth(label, Math.max(1, x + w - labelX - 6));
        graphics.drawString(this.font, Component.literal(shownLabel), labelX,
                UiMetrics.centeredTextY(y, h, this.font.lineHeight),
                hovered || selected ? 0xFFE7ECF2 : 0xFF8995A4, false);
    }

    private void clearFocus() {
        setFocused(null);
    }

    @Override
    public void onClose() {
        if (!settingsApplied && ChronicleClient.CONFIG != null) {
            String savedPath = ChronicleClient.CONFIG.toastBackgroundImagePath;
            if (savedPath != null && !savedPath.isBlank()) {
                CustomToastBackground.prepare(this.minecraft, savedPath);
            }
            CustomToastBackground.retain(this.minecraft, savedPath);
        }
        transition.start(this.minecraft, parent);
    }
}
