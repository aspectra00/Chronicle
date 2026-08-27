# Changelog

## 1.2.9

### Added

- Condition-based reminders for health, hunger, air, inventory space, held-item durability, dimensions, and X/Z areas
- Watch This alerts for crops, honey, cauldrons, composters, cave vines, furnaces, copper, and baby mobs
- Watches screen with per-world target management
- Notification history with missed, completed, and snoozed states
- Optional Snooze and Dismiss actions for Modern notifications
- Configurable snooze delay
- Custom PNG and JPG backgrounds for Modern notifications

### Changed

- Reworked reminder and trigger editing for simpler daily, weekly, interval, and condition setup
- Weekly reminders now run on every selected day
- After-trigger actions are limited to schedule types where they apply
- Rebuilt the Modern notification layout and aligned its text, icon, controls, and spacing
- Standardized spacing and separators across Chronicle screens
- Updated the project icon and moved the project to the MIT License

### Fixed

- Dismiss and Snooze now close the active notification immediately after a successful action
- Vanilla notifications never show Chronicle action buttons
- Reminder actions are saved before the notification is removed
- Notification history and watch changes survive restarts
- Scheduler handling for brief stalls, clock changes, disabled reminders, and burst notifications
