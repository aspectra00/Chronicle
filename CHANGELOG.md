# Changelog

## 1.3.1+26.2

### Changed

- Lowered the minimum Fabric Loader version to 0.19.0
- Kept Fabric Loader 0.19.3 as the recommended stable version

## 1.3.0

### Added

- Keyboard-accessible Snooze and Dismiss controls for active Modern notifications
- Configurable notification interaction key, bound to U by default
- About Chronicle screen with a supporter guide and optional public supporter credits
- Discreet Ko-fi support link that appears after Chronicle has delivered a real reminder and can be hidden permanently

### Changed

- Consolidated notification design, sound, and project information into a single Customize screen
- Simplified the main navigation to keep everyday reminder controls easy to reach
- Aligned list surfaces and scrollbars across reminders, history, and watches
- Refined support controls to remain secondary to Chronicle's reminder workflow

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
