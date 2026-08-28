# Changelog

## 1.3.1+1.21

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21
- Updated Fabric API compatibility for the 1.21 release line
- Kept the minimum Fabric Loader version at 0.15.11

## 1.3.1+1.21.1

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.1
- Updated Fabric API, Text Placeholder API, and Mod Menu compatibility for the 1.21.1 release line
- Set the minimum Fabric Loader version to 0.15.11

## 1.3.1+1.21.2

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.2
- Updated Fabric API compatibility for the 1.21.2 release line
- Set the minimum Fabric Loader version to 0.16.7

## 1.3.1+1.21.3

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.3
- Updated Fabric API and Mod Menu compatibility for the 1.21.3 release line
- Set the minimum Fabric Loader version to 0.16.8

## 1.3.1+1.21.4

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.4
- Updated Fabric API, Text Placeholder API, and Mod Menu compatibility for the 1.21.4 release line
- Set the minimum Fabric Loader version to 0.16.9

## 1.3.1+1.21.5

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.5
- Updated Fabric API, Text Placeholder API, and Mod Menu compatibility for the 1.21.5 release line
- Set the minimum Fabric Loader version to 0.16.10

## 1.3.1+1.21.6

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.6
- Updated Fabric API compatibility for the 1.21.6 release line
- Kept the minimum Fabric Loader version at 0.16.13

## 1.3.1+1.21.7

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.7
- Updated Fabric API compatibility for the 1.21.7 release line
- Kept the minimum Fabric Loader version at 0.16.13

## 1.3.1+1.21.8

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.8
- Updated Fabric API, Text Placeholder API, and Mod Menu compatibility for the 1.21.8 release line
- Set the minimum Fabric Loader version to 0.16.13

## 1.3.1+1.21.9

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.9
- Updated Fabric API and Text Placeholder API compatibility for the 1.21.9 release line
- Kept the minimum Fabric Loader version at 0.17.0

## 1.3.1+1.21.10

### Changed

- Ported Chronicle 1.3.1 to Minecraft 1.21.10
- Updated Fabric API, Text Placeholder API, and Mod Menu compatibility for the 1.21.10 release line
- Set the minimum Fabric Loader version to 0.17.0

## 1.3.1+26.1

### Changed

- Ported Chronicle 1.3.1 to Minecraft 26.1
- Updated Fabric API compatibility for the 26.1 release line
- Lowered the minimum Fabric Loader version to 0.18.6

## 1.3.1+26.1.2

### Changed

- Lowered the minimum Fabric Loader version to 0.19.0
- Kept Fabric Loader 0.19.3 as the recommended stable version

## 1.3.0+26.1.2

### Changed

- Ported Chronicle 1.3.0 to Minecraft 26.1.2
- Updated Fabric API, Text Placeholder API, and Mod Menu compatibility for the 26.1.2 release line

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
