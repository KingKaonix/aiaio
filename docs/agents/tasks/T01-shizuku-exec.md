# T01 — Shizuku exec proof

**Owner:** Runtime pod · **Depends on:** T06 (installable APK) · **Status:** Not started

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

- `adb shell` transcript, logcat lines tagged `aiaio-runtime`, device model + Android version,
  Shizuku version (13.x), and a screen recording of the status screen.

## Risks

- Shizuku user service lifetime tied to Shizuku, not the app → plan a reconnect path.
- OEM battery killers (Xiaomi/Huawei/Samsung) may kill the service → document per-OEM steps.

## Out of scope

Package installs (T02), PTY (T04), proxy (T05).
