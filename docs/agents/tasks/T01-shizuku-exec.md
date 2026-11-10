# T01 — Shizuku exec proof

**Owner:** Runtime pod · **Depends on:** T06 (installable APK) · **Status:** In progress — probe implemented, device run pending

## Goal

Prove that a `targetSdk 36` app can, without root, use a Shizuku user service (shell UID) to
create and execute a program in `/data/local/tmp/aiaio/`.

## Why

Android denies `execve()` on files in app-private storage for `targetSdk >= 29`. If this task
fails, the self-contained runtime is impossible and the project pivots to Companion mode.

## Steps

1. Add a Shizuku `UserService` (AIDL) to `android/runtime`.
2. Request `Shizuku.requestPermission()` and show status in the app.
3. From the service: `id -u` must print `2000`; `mkdir -p /data/local/tmp/aiaio/probe`.
4. Write a tiny static aarch64 ELF (or a `#!/system/bin/sh` script) into that directory,
   `chmod 700`, execute it, return stdout over AIDL.
5. Start a foreground service in the app, bind the user service, keep the process alive for
   30 minutes with the screen off, then execute again.
6. Uninstall the app and verify what remains in `/data/local/tmp/aiaio` (document cleanup rules).

## Acceptance criteria

- [ ] `id -u` returns `2000` from the service.
- [ ] A program written by the service executes and returns expected output.
- [ ] Execution still works after 30 min screen-off (FGS alive, battery optimization disabled).
- [ ] Failure modes are surfaced with actionable UI (Shizuku not installed / not running /
      permission denied / service killed).
- [ ] No root, no Termux, no system modification.

## Evidence

Device: Motorola moto g play - 2024 (`fogona_g`, Android 14 / SDK 34, kernel 5.15.185, SELinux
enforcing), Shizuku-Next manager 14.0.27-next, app `io.github.kingkaonix.aiaio.debug`.

- `docs/agents/evidence/T01-device-shizuku-next.jpg` — device + Shizuku facts screen.
- `docs/agents/evidence/T01-probe-result.jpg` — probe card: `uid=2000`, `mkdirs` →
  `/data/local/tmp/aiaio/probe`, `write` mode 700, `exec` → `probe-uid=2000`,
  `probe-user=shell`.
- Second run after leaving the app (cold binder rebind, 11 s) reproduced the same output.
- No `adb`, no root, no Termux: everything went through `IRuntimeService` (uid 2000).

## Risks

- Shizuku user service lifetime tied to Shizuku, not the app → plan a reconnect path.
- OEM battery killers (Xiaomi/Huawei/Samsung) may kill the service → document per-OEM steps.

## Out of scope

Package installs (T02), PTY (T04), proxy (T05).

## Implementation notes (Phase 0 probe)

Steps 1–4 are implemented and compile in CI; steps 5–6 still need a device run.

- `IRuntimeService` (AIDL) exposes `uid()`, `exec(argv, cwd)`, `mkdirs(path)` and
  `writeFile(path, data, mode)`. New methods are appended, never inserted: AIDL transaction
  ids follow declaration order. `RuntimeManager`'s Shizuku user-service `version` must be
  bumped whenever this interface changes (currently `2`).
- Every path is confined to `/data/local/tmp/aiaio` by `RuntimePaths.resolve()` on the
  service side (canonical path, `..` and prefix tricks rejected). `RuntimePathsTest` covers
  the rejection cases.
- `writeFile` applies the mode with `Files.setPosixFilePermissions`; the probe writes
  `probe.sh` with `0700` and executes it with no shell interpolation.
- The **More** tab has a "Run T01 probe" button that binds the user service and reports
  `uid`, `mkdirs`, `write` and `exec` results as `code=… output=…` lines. Expected output
  starts with `probe-uid=2000`.

Evidence to attach when run on device: screenshot of the probe card, `id -u` output, device
model/Android version, Shizuku version, and the 30-minute screen-off repeat.
