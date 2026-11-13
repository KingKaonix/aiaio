# T04 — PTY proof

**Owner:** Terminal pod · **Depends on:** T01 · **Status:** Not started / unverified — T01 done on device, but no PTY (`forkpty`), no master-fd-over-Binder AIDL, and no terminal UI/session flow present in current debug build.

## Goal

Allocate a PTY as shell UID in the runtime, pass the master fd to the app, and drive an
interactive shell from Compose with correct echo, signals, resize and Unicode.

## Steps

1. Add a small native helper (`libaiaio_pty.so`, `forkpty`) invoked by the Shizuku service.
2. Return the master fd to the app as `ParcelFileDescriptor` over Binder (no byte copying).
3. App-side reader/writer coroutines with backpressure and a bounded buffer.
4. Resize: `TIOCSWINSZ` + `SIGWINCH` on layout changes.
5. Keyboard: send Ctrl/Alt/Esc sequences; verify `Ctrl-C` interrupts a `sleep 100`, `Ctrl-D`
   closes, arrows work in `bash`/`vim`-style apps.
6. Run for 30 minutes with output churn; check fd leaks, RSS and dropped bytes.

## Acceptance criteria

- [ ] Interactive `bash` works end-to-end; `stty size` matches the Compose viewport. *(unverified)*
- [ ] `Ctrl-C`, `Ctrl-D`, `Ctrl-Z`, arrows, Home/End, Tab completion behave correctly. *(unverified)*
- [ ] UTF-8 and emoji pass through unchanged; no mojibake on resize. *(unverified)*
- [ ] 30-minute session with no fd leak and stable RSS (±10%). *(unverified)*
- [ ] Terminal survives app backgrounding (FGS) and reconnects to the same session. *(unverified)*
- [ ] Session close kills the child process group (no orphans in `/data/local/tmp`). *(unverified)*

## Evidence

- 2026-11-13 (device inspection via Agent-VScreen): Motorola moto g play 2024 (Android 14/SDK 34) with `io.github.kingkaonix.aiaio.debug` shows only Phase 0 scaffold screen/tabs (Providers/MCP/Skills/Sessions/More) and T01 probe/soak controls; no interactive terminal surface observed. Runtime AIDL (`IRuntimeService`) exposes `uid/exec/mkdirs/writeFile` only (no PTY allocation/fd passing). Transcript, fd/RSS samples, `stty`/resize/Unicode/signal/session-cleanup proof not possible on current build. Asciinema/recording, `ls -l /proc/<pid>/fd`, and RSS samples to be added when PTY is implemented.
