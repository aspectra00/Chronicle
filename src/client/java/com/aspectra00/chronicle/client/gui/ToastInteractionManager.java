package com.aspectra00.chronicle.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class ToastInteractionManager {
    private static final ArrayList<WeakReference<CustomReminderToast>> TOASTS = new ArrayList<>();
    private static final Set<Screen> REGISTERED_SCREENS =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean initialized;

    private ToastInteractionManager() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> registerScreen(screen));
    }

    private static void registerScreen(Screen screen) {
        if (screen == null || !REGISTERED_SCREENS.add(screen)) return;
        ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, event) ->
                !handleMouseClick(Minecraft.getInstance(), event));
    }

    static void register(CustomReminderToast toast) {
        if (toast == null) return;
        prune();
        TOASTS.add(new WeakReference<>(toast));
    }

    static void unregister(CustomReminderToast toast) {
        TOASTS.removeIf(reference -> {
            CustomReminderToast candidate = reference.get();
            return candidate == null || candidate == toast;
        });
    }

    private static boolean handleMouseClick(Minecraft client, MouseButtonEvent event) {
        if (client == null || event == null || event.button() != 0) return false;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            CustomReminderToast toast = TOASTS.get(i).get();
            if (toast == null) {
                TOASTS.remove(i);
            } else if (toast.handleMouseClick(client, event.x(), event.y())) {
                return true;
            }
        }
        return false;
    }

    private static void prune() {
        TOASTS.removeIf(reference -> reference.get() == null);
    }
}
