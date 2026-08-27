package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.gui.ReminderConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ReminderConfigScreen(parent);
    }
}
