package com.aspectra00.chronicle.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class VerticallyCenteredEditBox extends EditBox {
    private final Font font;
    private int horizontalPadding;

    VerticallyCenteredEditBox(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x, y, width, height, narration);
        this.font = font;
    }

    void setHorizontalPadding(int padding) {
        this.horizontalPadding = Math.max(0, padding);
        setCursorPosition(getCursorPosition());
    }

    @Override
    public int getInnerWidth() {
        int baseWidth = Math.max(1, super.getInnerWidth());
        int inset = Math.min(horizontalPadding, Math.max(0, (baseWidth - 1) / 2));
        return Math.max(1, baseWidth - inset * 2);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        super.onClick(contentEvent(event), doubleClick);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        super.onDrag(contentEvent(event), dragX, dragY);
    }

    @Override
    public int getScreenX(int index) {
        return super.getScreenX(index) + effectivePadding();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int logicalY = getY();
        int logicalX = getX();
        int inset = effectivePadding();
        int textOffset = Math.max(0, (getHeight() - font.lineHeight + 1) / 2);
        if (textOffset == 0 && inset == 0) {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
            return;
        }

        setX(logicalX + inset);
        setY(logicalY + textOffset);
        try {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
        } finally {
            setX(logicalX);
            setY(logicalY);
        }
    }

    private int effectivePadding() {
        return Math.min(horizontalPadding, Math.max(0, (getWidth() - 1) / 2));
    }

    private MouseButtonEvent contentEvent(MouseButtonEvent event) {
        int inset = effectivePadding();
        if (inset == 0) return event;

        double adjustedX = event.x() - inset;
        adjustedX = Math.max(getX(), Math.min(getX() + getInnerWidth(), adjustedX));
        return new MouseButtonEvent(adjustedX, event.y(), event.buttonInfo());
    }
}
