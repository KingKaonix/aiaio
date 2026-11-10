# ADR-0004 — Native VT terminal (no WebView, no GPLv3)

**Status:** accepted · 2026-11-10

## Context

aiaio must run interactive CLI tools (Claude Code, Codex CLI, Gemini CLI, OpenCode, …)
including their login flows, TUIs, alternate-screen apps, mouse reporting and paste. The
plan forbids a WebView + xterm.js stack (second rendering engine, crash coupling, memory)
and forbids GPLv3 terminal libraries (license contagion into an MIT app).

A terminal needs three things: a **PTY**, a **VT parser**, and a **renderer**.

- The PTY must be allocated by the Shizuku user service (shell UID). `forkpty` there yields a
  master fd which can be passed to the app over Binder as a `ParcelFileDescriptor`; bytes never
  cross Binder.
- The parser and renderer are Kotlin/Compose inside the app.

## Decision

- New `android/terminal` module (Kotlin + Compose, no native dependencies beyond the PTY helper).
- **Parser:** ECMA-48 / xterm subset written in-house:
  - CSI/OSC/DCS introducers, SGR (16/256/truecolor), cursor save/restore,
    erase-in-display/line, scroll regions, insert/delete lines/chars.
  - Primary + alternate screen buffers; ring-buffer scrollback; optional reflow on resize.
  - DEC private modes actually used by the target tools (cursor visibility, bracketed paste,
    mouse reporting with SGR 1006, app cursor keys, focus events).
  - OSC 8 hyperlinks, OSC 52 clipboard (opt-in), wide chars/emoji, combining marks.
- **Screen model:** cell grid with attributes, dirty regions, snapshot for tests.
- **Renderer:** Compose `Canvas` + `TextMeasurer`, monospace font bundled, glyph cache,
  dirty-region redraw. Target 60 fps at 80×24 on a mid-range device.
- **Input:** IME composing text, hardware keyboard, modifier toolbar (Ctrl/Alt/Esc/Tab/arrows),
  bracketed paste, paste confirmation, `TIOCSWINSZ` + `SIGWINCH` on layout change.
- **Conformance:** recorded ANSI streams from each target CLI replayed as golden screen
  snapshots in CI, plus a vttest subset. A parser change that alters a snapshot must update
  the golden files deliberately.
- **PTY:** small native helper (`forkpty`) launched by the Shizuku service; the master fd is
  returned to the app. No terminal code runs as shell UID beyond the PTY syscalls.

## Consequences

- 6–9 agent-weeks plus 2–3 human-weeks of keyboard/device testing. Top-2 project risk.
- The parser and renderer are covered by unit tests; the PTY path needs a real device.
- No third-party terminal code is linked: no license contamination, full control over
  rendering and accessibility.
- If TUI fidelity is not acceptable by week 12, ship non-interactive login flows first and the
  terminal as beta (plan §9 kill criteria).

## Rejected

- **WebView + xterm.js**: second rendering stack, JS bridge, memory, cannot pass an fd to JS
  cleanly, crash coupling.
- **Termux `terminal-emulator`**: GPLv3, and it drags in a large app-shaped model we do not
  need.
- **libvterm (MIT)**: viable, but it is a C library needing JNI and still requires a renderer;
  maintaining our own parser is comparable work and keeps the module pure Kotlin.
- **SSH-only / external terminal**: breaks the self-contained, native-terminal requirement.
