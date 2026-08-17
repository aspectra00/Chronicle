package com.aspectra00.chronicle.client;

import com.aspectra00.chronicle.client.gui.ReminderConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * This is the bridge used by Mod Menu. It adds the Config button to the
 * Chronicle entry, opening the custom styled reminder screen.
 */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ReminderConfigScreen(parent);
    }
}
