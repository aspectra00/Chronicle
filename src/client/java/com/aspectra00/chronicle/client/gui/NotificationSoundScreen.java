package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleClient;
import com.aspectra00.chronicle.client.ChronicleI18n;
import com.aspectra00.chronicle.client.CustomSoundPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;

public final class NotificationSoundScreen extends Screen {
    private static final int TEXT = UiFrame.TEXT;
    private static final int MUTED = UiFrame.MUTED;
    private static final int ERROR = 0xFFD69A9A;

    private final Screen parent;
    private final ChronicleScreenTransition transition = new ChronicleScreenTransition();
    private String mode;
    private String soundPath;
    private int volumePercent;
    private String saveError;

    private VerticallyCenteredEditBox pathBox;
    private Button vanillaModeButton;
    private Button customModeButton;
    private Button offModeButton;
    private Button volumeValueButton;
    private Button testSoundButton;
    private Button applyButton;
    private long lastPressedAt = -1L;
    private int lastPressedX;
    private int lastPressedY;
    private int lastPressedW;
    private int lastPressedH;

    private record SoundLayout(
            int panelW, int left, int top, int bottom, int inset,
            int x, int contentW, int controlH,
            int titleY, int separatorY,
            int modeLabelY, int modeY,
            int fileLabelY, int fileY, int formatsY,
            int volumeLabelY, int volumeY, int footerY,
            boolean showSubtitle
    ) {}

    public NotificationSoundScreen(Screen parent) {
        super(ChronicleI18n.component("sound.title"));
        this.parent = parent;
        this.mode = switch (ChronicleClient.CONFIG.notificationSoundMode == null
                ? "VANILLA" : ChronicleClient.CONFIG.notificationSoundMode) {
            case "CUSTOM" -> "CUSTOM";
            case "OFF" -> "OFF";
            default -> "VANILLA";
        };
        this.soundPath = ChronicleClient.CONFIG.customSoundPath;
        this.volumePercent = Math.round(ChronicleClient.CONFIG.notificationSoundVolume * 100.0f);
        CustomSoundPlayer.clearLastError();
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
        String playbackError = CustomSoundPlayer.getLastError();
        if (playbackError != null) saveError = playbackError;
    }

    @Override
    protected void init() {
        clearFocus();
        setDragging(false);
        clearWidgets();
        SoundLayout layout = soundLayout();

        int modeGap = Math.min(UiMetrics.GAP_XS, Math.max(0, (layout.contentW() - 3) / 2));
        int modeW = Math.max(1, (layout.contentW() - modeGap * 2) / 3);
        vanillaModeButton = button(ChronicleI18n.tr("sound.mode.vanilla"),
                layout.x(), layout.modeY(), modeW, layout.controlH(),
                b -> selectMode("VANILLA"));
        customModeButton = button(ChronicleI18n.tr("sound.mode.custom"),
                layout.x() + modeW + modeGap, layout.modeY(), modeW, layout.controlH(),
                b -> selectMode("CUSTOM"));
        offModeButton = button(ChronicleI18n.tr("sound.mode.off"),
                layout.x() + (modeW + modeGap) * 2, layout.modeY(),
                Math.max(1, layout.contentW() - (modeW + modeGap) * 2),
                layout.controlH(), b -> selectMode("OFF"));
        addRenderableWidget(vanillaModeButton);
        addRenderableWidget(customModeButton);
        addRenderableWidget(offModeButton);

        int fileGap = layout.contentW() >= 96
                ? UiMetrics.GAP_SM
                : layout.contentW() >= 32 ? UiMetrics.GAP_XS : 0;
        fileGap = Math.min(fileGap, Math.max(0, layout.contentW() - 2));
        int minimumFieldW = Math.min(96, Math.max(1, layout.contentW() * 3 / 5));
        int browsePreferred = Math.min(112, Math.max(84, layout.contentW() / 4));
        int browseW = Math.max(1, Math.min(browsePreferred,
                Math.max(1, layout.contentW() - fileGap - minimumFieldW)));
        int fieldW = Math.max(1, layout.contentW() - browseW - fileGap);
        pathBox = new VerticallyCenteredEditBox(this.font, layout.x(), layout.fileY(), fieldW,
                layout.controlH(),
                ChronicleI18n.component("sound.file"));
        pathBox.setMaxLength(1024);
        pathBox.setValue(soundPath == null ? "" : soundPath);
        pathBox.setResponder(value -> soundPath = value);
        pathBox.setBordered(false);
        pathBox.setTextShadow(false);
        pathBox.setTextColorUneditable(MUTED);
        pathBox.setHorizontalPadding(4);
        pathBox.setHint(ChronicleI18n.component("sound.file.hint"));
        addRenderableWidget(pathBox);
        addRenderableWidget(button(ChronicleI18n.tr("action.browse"),
                layout.x() + fieldW + fileGap,
                layout.fileY(), browseW, layout.controlH(), b -> browseForSound()));

        int volumeGap = Math.min(UiMetrics.GAP_SM, Math.max(0, (layout.contentW() - 3) / 2));
        int maxSideW = Math.max(1, (layout.contentW() - volumeGap * 2 - 1) / 2);
        int smallW = Math.min(maxSideW, Math.min(54, Math.max(36, layout.contentW() / 7)));
        int valueW = Math.max(1, layout.contentW() - smallW * 2 - volumeGap * 2);
        addRenderableWidget(button("−", layout.x(), layout.volumeY(), smallW,
                layout.controlH(), b -> adjustVolume(-10)));
        volumeValueButton = button(volumeLabel(), layout.x() + smallW + volumeGap,
                layout.volumeY(), valueW, layout.controlH(), b -> {});
        volumeValueButton.active = false;
        addRenderableWidget(volumeValueButton);
        addRenderableWidget(button("+", layout.x() + smallW + volumeGap
                        + valueW + volumeGap,
                layout.volumeY(), smallW, layout.controlH(), b -> adjustVolume(10)));

        int gap = Math.min(UiMetrics.GAP_SM, Math.max(0, (layout.contentW() - 3) / 2));
        int actionW = Math.max(1, (layout.contentW() - gap * 2) / 3);
        testSoundButton = button(ChronicleI18n.tr("action.test_sound"), layout.x(), layout.footerY(),
                actionW, layout.controlH(), b -> testSound());
        addRenderableWidget(testSoundButton);
        applyButton = button(ChronicleI18n.tr("action.apply"), layout.x() + actionW + gap,
                layout.footerY(), actionW, layout.controlH(), b -> apply());
        addRenderableWidget(applyButton);
        addRenderableWidget(button(ChronicleI18n.tr("action.cancel"),
                layout.x() + (actionW + gap) * 2,
                layout.footerY(), actionW, layout.controlH(), b -> onClose()));

        updateModeState();
    }

    @Override
    protected void rebuildWidgets() {
        if (pathBox != null) soundPath = pathBox.getValue();
        boolean restorePathFocus = getFocused() == pathBox;
        clearFocus();
        setDragging(false);
        init();
        if (restorePathFocus && pathBox != null && "CUSTOM".equals(mode)) {
            setFocused(pathBox);
            pathBox.setFocused(true);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (pathBox != null) soundPath = pathBox.getValue();
        lastPressedAt = -1L;
        clearFocus();
        setDragging(false);
        super.resize(width, height);
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

    private void selectMode(String selectedMode) {
        mode = switch (selectedMode) {
            case "CUSTOM" -> "CUSTOM";
            case "OFF" -> "OFF";
            default -> "VANILLA";
        };
        updateModeState();
        CustomSoundPlayer.clearLastError();
        saveError = null;
    }

    private String volumeLabel() {
        return ChronicleI18n.tr("sound.volume.value", volumePercent);
    }

    private SoundLayout soundLayout() {
        int panelW = Math.max(1, Math.min(620, this.width - 24));
        int left = (this.width - panelW) / 2;
        int panelH = Math.min(330, Math.max(1, this.height - 24));
        int top = Math.max(4, (this.height - panelH) / 2);
        int bottom = Math.min(this.height, top + panelH);
        int inset = UiMetrics.contentInset(panelW);
        int x = left + inset;
        int contentW = Math.max(1, panelW - inset * 2);

        boolean standard = this.height >= 320;
        boolean compact = this.height >= 200 && !standard;
        int titleY = standard ? UiMetrics.headerTitleY(top, false) : top + 9;
        int separatorY = standard
                ? UiMetrics.headerDividerY(top, UiMetrics.HEADER_HEIGHT)
                : top + (compact ? 31 : 25);
        int controlH;
        if (standard) {
            controlH = 30;
        } else if (compact) {
            controlH = this.height < 260 ? 22 : 26;
        } else {
            int microAvailable = Math.max(1, bottom - (separatorY + 2) - 2);
            controlH = Math.max(8, Math.min(20, microAvailable / 4));
        }

        int modeLabelY;
        int modeY;
        int fileLabelY;
        int fileY;
        int formatsY = -1;
        int volumeLabelY = -1;
        int volumeY;

        if (standard) {
            modeLabelY = separatorY + UiMetrics.GAP_MD;
            modeY = modeLabelY + UiMetrics.LABEL_OFFSET;
            fileLabelY = modeY + controlH + UiMetrics.GAP_MD;
            fileY = fileLabelY + UiMetrics.LABEL_OFFSET;
            formatsY = fileY + controlH + UiMetrics.GAP_SM;
            volumeLabelY = formatsY + this.font.lineHeight + UiMetrics.GAP_MD;
            volumeY = volumeLabelY + UiMetrics.LABEL_OFFSET;
        } else if (compact) {
            modeLabelY = -1;
            modeY = separatorY + UiMetrics.GAP_SM;
            fileLabelY = modeY + controlH + UiMetrics.GAP_MD;
            fileY = fileLabelY + UiMetrics.LABEL_OFFSET;
            volumeY = fileY + controlH + UiMetrics.GAP_MD;
        } else {
            modeLabelY = -1;
            fileLabelY = -1;
            int microTop = separatorY + 2;
            int microBottom = Math.max(microTop, bottom - 2);
            int microGap = Math.max(0, (microBottom - microTop - controlH * 4) / 3);
            modeY = microTop;
            fileY = modeY + controlH + microGap;
            volumeY = fileY + controlH + microGap;
        }

        int footerInset = standard ? UiMetrics.GAP_MD : compact ? UiMetrics.GAP_SM : 6;
        int footerY;
        if (standard || compact) {
            footerY = Math.max(volumeY + controlH + 4, bottom - controlH - footerInset);
            footerY = Math.min(Math.max(0, bottom - controlH - 2), footerY);
        } else {
            int microTop = separatorY + 2;
            int microBottom = Math.max(microTop, bottom - 2);
            int microGap = Math.max(0, (microBottom - microTop - controlH * 4) / 3);
            footerY = volumeY + controlH + microGap;
        }
        return new SoundLayout(panelW, left, top, bottom, inset, x, contentW, controlH,
                titleY, separatorY, modeLabelY, modeY, fileLabelY, fileY, formatsY,
                volumeLabelY, volumeY, footerY, standard);
    }

    private int errorY(SoundLayout layout) {
        if (layout.formatsY() >= 0) {
            return layout.formatsY();
        }
        int aboveFooter = layout.footerY() - this.font.lineHeight - 3;
        if (aboveFooter >= layout.volumeY() + layout.controlH() + 2) {
            return aboveFooter;
        }
        if (layout.fileLabelY() >= 0) {
            return layout.fileLabelY();
        }
        return layout.titleY();
    }

    private void adjustVolume(int delta) {
        volumePercent = Math.max(0, Math.min(100, volumePercent + delta));
        volumeValueButton.setMessage(Component.literal(volumeLabel()));
    }

    private void updateModeState() {
        boolean custom = "CUSTOM".equals(mode);
        if (pathBox != null) {
            if (!custom) {
                if (getFocused() == pathBox) {
                    clearFocus();
                }
                pathBox.setFocused(false);
            }
            pathBox.setEditable(custom);
            pathBox.active = custom;
            pathBox.setTextColor(custom ? TEXT : MUTED);
        }
        if (testSoundButton != null) {
            testSoundButton.active = !"OFF".equals(mode);
        }
    }

    private void browseForSound() {
        String defaultPath = soundPath == null || soundPath.isBlank()
                ? Path.of(System.getProperty("user.home", ".")).toAbsolutePath().toString()
                : soundPath;
        String selected;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String[] patterns = CustomSoundPlayer.supportedFilePatterns();
            PointerBuffer nativePatterns = stack.mallocPointer(patterns.length);
            for (String pattern : patterns) nativePatterns.put(stack.UTF8(pattern));
            nativePatterns.flip();
            selected = TinyFileDialogs.tinyfd_openFileDialog(
                    ChronicleI18n.tr("sound.dialog.title"),
                    defaultPath,
                    nativePatterns,
                    CustomSoundPlayer.supportedFormats(),
                    false
            );
        }
        if (selected != null && !selected.isBlank()) {
            mode = "CUSTOM";
            soundPath = Path.of(selected).toAbsolutePath().normalize().toString();
            pathBox.setValue(soundPath);
            updateModeState();
            CustomSoundPlayer.clearLastError();
            saveError = null;
        }
    }

    private void testSound() {
        saveError = null;
        if ("OFF".equals(mode)) return;
        if ("CUSTOM".equals(mode)) {
            CustomSoundPlayer.playCustom(this.minecraft, soundPath, volumePercent / 100.0f);
        } else {
            CustomSoundPlayer.playVanilla(this.minecraft, volumePercent / 100.0f);
        }
    }

    private void apply() {
        if ("CUSTOM".equals(mode)) {
            String fileError = CustomSoundPlayer.validateCustomFile(soundPath);
            if (fileError != null) {
                saveError = fileError;
                return;
            }
        }
        String oldMode = ChronicleClient.CONFIG.notificationSoundMode;
        String oldPath = ChronicleClient.CONFIG.customSoundPath;
        float oldVolume = ChronicleClient.CONFIG.notificationSoundVolume;
        ChronicleClient.CONFIG.notificationSoundMode = mode;
        ChronicleClient.CONFIG.customSoundPath = soundPath == null ? "" : soundPath.trim();
        ChronicleClient.CONFIG.notificationSoundVolume = volumePercent / 100.0f;
        ChronicleClient.CONFIG.ensureValid();
        if (!ChronicleClient.CONFIG.save()) {
            ChronicleClient.CONFIG.notificationSoundMode = oldMode;
            ChronicleClient.CONFIG.customSoundPath = oldPath;
            ChronicleClient.CONFIG.notificationSoundVolume = oldVolume;
            saveError = ChronicleClient.CONFIG.getLastSaveError();
            return;
        }
        CustomSoundPlayer.stopCustom();
        transition.start(this.minecraft, parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        transition.begin(graphics, this.width, this.height);
        SoundLayout layout = soundLayout();
        boolean hasError = saveError != null && !saveError.isBlank();
        int currentErrorY = hasError ? errorY(layout) : -1;
        boolean errorReplacesTitle = hasError && currentErrorY == layout.titleY();
        boolean errorReplacesFileLabel = hasError && currentErrorY == layout.fileLabelY();

        graphics.fill(0, 0, this.width, this.height, UiFrame.BACKDROP);
        UiFrame.drawWindow(graphics, layout.left(), layout.top(), layout.panelW(), layout.bottom());
        int headerTextX = layout.x();
        int headerTextWidth = Math.max(1,
                layout.left() + layout.panelW() - layout.inset() - headerTextX);
        String headerTitle = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("sound.title"), headerTextWidth);
        if (!errorReplacesTitle) {
            graphics.text(this.font, Component.literal(headerTitle), headerTextX,
                    layout.titleY(), TEXT, true);
        }
        if (layout.showSubtitle()) {
            graphics.text(this.font, Component.literal(UiFrame.trimToWidth(this.font,
                            ChronicleI18n.tr("sound.subtitle"), headerTextWidth)),
                    headerTextX, UiMetrics.headerSubtitleY(layout.top(), false), MUTED, false);
        }
        UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                layout.separatorY());
        int footerGap = layout.showSubtitle() ? UiMetrics.GAP_MD : UiMetrics.GAP_SM;
        int footerDividerY = layout.footerY() - footerGap;
        if (footerDividerY > layout.volumeY() + layout.controlH()) {
            UiFrame.drawInsetDivider(graphics, layout.left(), layout.panelW(), layout.inset(),
                    footerDividerY);
        }

        if (layout.modeLabelY() >= 0) {
            graphics.text(this.font, ChronicleI18n.component("sound.mode"), layout.x(),
                    layout.modeLabelY(), MUTED, false);
        }
        if (layout.fileLabelY() >= 0 && !errorReplacesFileLabel) {
            String fileLabel = ChronicleI18n.tr("sound.file");
            graphics.text(this.font, Component.literal(UiFrame.trimToWidth(this.font, fileLabel,
                            layout.contentW())), layout.x(), layout.fileLabelY(), MUTED, false);
        }
        if (layout.formatsY() >= 0 && !hasError) {
            String formats = UiFrame.trimToWidth(this.font,
                    ChronicleI18n.tr("sound.formats", CustomSoundPlayer.supportedFormats()),
                    layout.contentW());
            graphics.text(this.font, Component.literal(formats), layout.x(),
                    layout.formatsY(), MUTED, false);
        }
        if (layout.volumeLabelY() >= 0) {
            graphics.text(this.font, ChronicleI18n.component("sound.volume"), layout.x(),
                    layout.volumeLabelY(), MUTED, false);
        }

        if (pathBox != null && pathBox.visible) {
            boolean pathHovered = pathBox.active
                    && mouseX >= pathBox.getX() && mouseX < pathBox.getX() + pathBox.getWidth()
                    && mouseY >= pathBox.getY() && mouseY < pathBox.getY() + pathBox.getHeight();
            int pathBorder = pathBox.active && (pathHovered || pathBox.isFocused())
                    ? UiFrame.CONTROL_HOVER : UiFrame.INNER_LINE;
            graphics.fill(pathBox.getX(), pathBox.getY(),
                    pathBox.getX() + pathBox.getWidth(), pathBox.getY() + pathBox.getHeight(), pathBorder);
            if (pathBox.getWidth() > 2 && pathBox.getHeight() > 2) {
                graphics.fill(pathBox.getX() + 1, pathBox.getY() + 1,
                        pathBox.getX() + pathBox.getWidth() - 1,
                        pathBox.getY() + pathBox.getHeight() - 1, UiFrame.PANEL);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        for (var child : this.children()) {
            if (child instanceof Button button) {
                boolean selectedMode = (button == vanillaModeButton && "VANILLA".equals(mode))
                        || (button == customModeButton && "CUSTOM".equals(mode))
                        || (button == offModeButton && "OFF".equals(mode));
                boolean emphasized = button == applyButton || selectedMode;
                UiFrame.drawButton(graphics, this.font, button, UiFrame.ACCENT, emphasized, mouseX, mouseY);
            }
        }
        if (lastPressedAt >= 0L) {
            float pulse = UiAnimation.pressProgress(lastPressedAt, Util.getMillis(), 170L);
            if (pulse > 0.0f) {
                int insetPulse = UiAnimation.pressInset(pulse);
                int alpha = Math.max(0, Math.min(72, Math.round(pulse * 72.0f)));
                graphics.fill(lastPressedX - insetPulse, lastPressedY - insetPulse,
                        lastPressedX + lastPressedW + insetPulse,
                        lastPressedY + lastPressedH + insetPulse,
                        (alpha << 24) | 0x008FB3E8);
            }
        }

        if (hasError) {
            String shown = UiFrame.trimToWidth(this.font, saveError, layout.contentW());
            graphics.text(this.font, Component.literal(shown), layout.x(), currentErrorY, ERROR, false);
        }
        transition.end(graphics, this.width, this.height);
    }

    @Override
    public void onClose() {
        CustomSoundPlayer.stopCustom();
        transition.start(this.minecraft, parent);
    }
}
