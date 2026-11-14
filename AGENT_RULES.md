# NezhaEDR Mandatory AI Agent Coding Standards & Rules

To maintain the highest level of code quality, performance, and security across the **NezhaEDR (NezhaCyberEDR Android)** project, all AI Coding Agents (including Cursor, Claude, Gemini, ChatGPT, and Android Studio Agent) must strictly adhere to the following mandatory rules and guidelines when generating or modifying code for this repository.

---

## 1. Zero Code Truncation Policy
- **No Shortcuts or Placeholders**: Never output truncated code, partial snippets with `// TODO: add rest here`, or incomplete function bodies.
- **Production-Ready Implementation**: Every Composable, data model, parser method, and service routine must be written completely from top to bottom, fully compiling and ready for production deployment.

---

## 2. Local Testing & Verification Requirement
- **Mandatory Local Unit Testing**: Before submitting any code changes or Pull Requests, you **MUST** execute the unit test suite (`./gradlew app:testDebugUnitTest`) and verify that 100% of unit tests pass with zero failures.
- **Build Compilation Check**: Ensure `./gradlew app:assembleDebug` builds cleanly with zero compilation errors.

---

## 3. Strict UI Design System & High-Contrast Guidelines
- **System Theme Respect**: Never force dark mode surfaces (`#000000`) over light mode when the system or user selection is in Light Mode (`isSystemInDarkTheme()`).
- **WCAG AAA Text Contrast**: All status labels, body texts, and subtitles must use high-contrast dark slate (`#111827` primary, `#374151` secondary, `#4B5563` muted) on light backgrounds. Never use faint light-gray text on white surfaces.
- **Unified Theme Color Palette**: Selected chips (`FilterChip`), badges, indicators, and buttons must strictly consume `MaterialTheme.colorScheme.primary` and `primaryContainer`. Never allow default Material 3 purple (`#E8DEF8`) fallbacks.
- **No Duplicate UI Labels**: Avoid repeating status badges or text elements inside the same card component.

---

## 4. Internationalization & 100% Localization
- **Zero Language Mixing**: Do not hardcode raw English or Chinese text strings directly inside Composable functions or View code.
- **String Resource Binding**: Every user-facing text element must use `stringResource(R.string...)` across all 5 supported locale resource files (`values/strings.xml`, `values-zh-rCN/`, `values-ja/`, `values-ko/`, `values-th/`).

---

## 5. Security & Thread-Safety Standards
- **Uncaught Exception Handling**: Network byte parsing (`NezhaVpnService.kt`), file stream reads, and SQLite database operations must be wrapped in `runCatching { ... }` blocks to prevent runtime crashes.
- **Immutable State Snapshots**: StateFlow mutations must use immutable snapshots (`.toList()`, `.toSet()`, `.update { ... }`) to prevent `ConcurrentModificationException` during UI list iterations.
- **Zero Remote Telemetry**: Never add analytics, tracking SDKs, or cloud transmission routines. All telemetry must remain 100% on-device inside local SQLite storage.
