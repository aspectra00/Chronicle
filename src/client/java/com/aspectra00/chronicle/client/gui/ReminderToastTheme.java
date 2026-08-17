package com.aspectra00.chronicle.client.gui;

import com.aspectra00.chronicle.client.config.ReminderConfig;

record ReminderToastTheme(int background, int border, int accent, int title, int message, int icon) {
    static ReminderToastTheme defaultMinimal() {
        return new ReminderToastTheme(0xFF10151C, 0xFF252E3A, 0xFF8FB3E8,
                0xFFE7ECF2, 0xFFB3BFCC, 0xFFE7ECF2);
    }

    static ReminderToastTheme fromConfig(ReminderConfig config) {
        return new ReminderToastTheme(
                config.toastBackgroundColor,
                config.toastBorderColor,
                config.toastAccentColor,
                config.toastTitleColor,
                config.toastMessageColor,
                config.toastIconColor
        );
    }
}
