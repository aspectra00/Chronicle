package com.aspectra00.chronicle.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

final class VerticallyCenteredEditBox extends EditBox {
    private final Font font;
    private int horizontalPadding;
    private boolean centered;

    VerticallyCenteredEditBox(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x, y, width, height, narration);
        this.font = font;
    }

    void setHorizontalPadding(int padding) {
        this.horizontalPadding = Math.max(0, padding);
        setCursorPosition(getCursorPosition());
    }

    void setCentered(boolean centered) {
        this.centered = centered;
        setCursorPosition(getCursorPosition());
    }

    @Override
    public int getInnerWidth() {
        int baseWidth = Math.max(1, super.getInnerWidth());
        int inset = Math.min(horizontalPadding, Math.max(0, (baseWidth - 1) / 2));
        return Math.max(1, baseWidth - inset * 2);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(contentX(mouseX), mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        super.onDrag(contentX(mouseX), mouseY, dragX, dragY);
    }

    @Override
    public int getScreenX(int index) {
        return super.getScreenX(index) + effectivePadding();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int logicalY = getY();
        int logicalX = getX();
        int inset = effectivePadding();
        int textOffset = Math.max(0, (getHeight() - font.lineHeight + 1) / 2);
        if (textOffset == 0 && inset == 0) {
            super.renderWidget(graphics, mouseX, mouseY, delta);
            return;
        }

        setX(logicalX + inset);
        setY(logicalY + textOffset);
        try {
            super.renderWidget(graphics, mouseX, mouseY, delta);
        } finally {
            setX(logicalX);
            setY(logicalY);
        }
    }

    private int effectivePadding() {
        int centeredPadding = centered ? Math.max(0, (getWidth() - font.width(getValue())) / 2) : 0;
        return Math.min(Math.max(horizontalPadding, centeredPadding), Math.max(0, (getWidth() - 1) / 2));
    }

    private double contentX(double mouseX) {
        int inset = effectivePadding();
        if (inset == 0) return mouseX;

        double adjustedX = mouseX - inset;
        return Math.max(getX(), Math.min(getX() + getInnerWidth(), adjustedX));
    }
}
