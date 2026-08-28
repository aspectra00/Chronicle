package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.ChronicleI18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ToastInteractionScreen extends Screen {
    public ToastInteractionScreen() {
        super(ChronicleI18n.component("toast.interaction.title"));
    }

    @Override
    public void tick() {
        super.tick();
        if (!ToastInteractionManager.hasVisibleActions(this.minecraft)) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta) {
        String hint = UiFrame.trimToWidth(this.font,
                ChronicleI18n.tr("toast.interaction.hint"), Math.max(1, this.width - 24));
        int textWidth = this.font.width(hint);
        int x = Math.max(6, (this.width - textWidth) / 2);
        int y = Math.max(6, this.height - this.font.lineHeight - 9);
        graphics.fill(x - 6, y - 4, Math.min(this.width - 2, x + textWidth + 6),
                y + this.font.lineHeight + 4, 0xB010151C);
        graphics.drawString(this.font, Component.literal(hint), x, y, UiFrame.MUTED, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
