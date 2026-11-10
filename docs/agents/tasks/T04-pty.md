# T04 — PTY proof

**Owner:** Terminal pod · **Depends on:** T01 · **Status:** Not started

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

- [ ] Interactive `bash` works end-to-end; `stty size` matches the Compose viewport.
- [ ] `Ctrl-C`, `Ctrl-D`, `Ctrl-Z`, arrows, Home/End, Tab completion behave correctly.
- [ ] UTF-8 and emoji pass through unchanged; no mojibake on resize.
- [ ] 30-minute session with no fd leak and stable RSS (±10%).
- [ ] Terminal survives app backgrounding (FGS) and reconnects to the same session.
- [ ] Session close kills the child process group (no orphans in `/data/local/tmp`).

## Evidence

Asciinema-style transcript or screen recording, `ls -l /proc/<pid>/fd` before/after,
`stty size` output, and RSS samples.
