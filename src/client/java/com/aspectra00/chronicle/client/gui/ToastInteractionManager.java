package com.aspectra00.chronicle.client.gui;

import net.minecraft.client.Minecraft;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public final class ToastInteractionManager {
    private static final ArrayList<WeakReference<CustomReminderToast>> TOASTS = new ArrayList<>();
    private ToastInteractionManager() {
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

    public static void open(Minecraft client) {
        if (client == null || client.screen != null || !hasVisibleActions(client)) return;
        client.setScreen(new ToastInteractionScreen());
    }

    public static boolean hasVisibleActions(Minecraft client) {
        if (client == null) return false;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            CustomReminderToast toast = TOASTS.get(i).get();
            if (toast == null) {
                TOASTS.remove(i);
            } else if (toast.hasVisibleActions(client)) {
                return true;
            }
        }
        return false;
    }

    public static boolean handleMouseClick(Minecraft client, double mouseX, double mouseY, int button) {
        if (client == null || button != 0) return false;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            CustomReminderToast toast = TOASTS.get(i).get();
            if (toast == null) {
                TOASTS.remove(i);
            } else if (toast.handleMouseClick(client, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private static void prune() {
        TOASTS.removeIf(reference -> reference.get() == null);
    }
}
