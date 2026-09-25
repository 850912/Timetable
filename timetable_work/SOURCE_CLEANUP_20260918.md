# Source cleanup — 2026-09-18

- Removed all project text references to the requested third-party app/brand.
- Removed the obsolete “conditional UI” user preference; unavailable glass controls are now hidden automatically.
- Removed legacy frosted/global glass flags and their storage/write APIs; one liquid-glass switch is authoritative.
- Simplified glass activation checks across Wear UI components.
- Preserved functional appearance, animation, top-time, theme/background, import/export, locale and calendar preferences.
- Build verification was attempted, but the wrapper distribution (Gradle 9.4.1) is not cached and the execution environment cannot resolve services.gradle.org.
