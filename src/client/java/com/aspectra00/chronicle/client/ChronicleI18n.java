package com.aspectra00.chronicle.client;

import net.minecraft.network.chat.Component;

/** Small translation facade used by Chronicle's custom-drawn screens. */
public final class ChronicleI18n {
    private ChronicleI18n() {}

    public static Component component(String key, Object... args) {
        return Component.translatable("chronicle." + key, args);
    }

    public static String tr(String key, Object... args) {
        return component(key, args).getString();
    }
}
