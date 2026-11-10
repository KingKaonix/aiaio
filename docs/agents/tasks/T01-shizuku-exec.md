# T01 — Shizuku exec proof

**Owner:** Runtime pod · **Depends on:** T06 (installable APK) · **Status:** In progress — probe
passes on device; 30-minute screen-off soak running

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
- Re-run on the soak build (`98fd830`): `probe-dir=/data/local/tmp/aiaio/probe`, i.e. `exec`
  now runs with the cwd the AIDL call asked for. Soak started 15:37:59, `tick=1` written at
  15:39:00 with `uid=2000`.

### Shizuku permission on this device (important)

Two managers are installed, and only one of them is the running server:

| Package | Role |
|---|---|
| `af.shizuku.plus.api` | **the live server + permission dialog** (`…/authorization/RequestPermissionActivity`) |
| `moe.shizuku.privileged.api` | Shizuku-Next UI; shows "Stopped" and "Start Shizuku to see apps" even while the server is alive and serving this app |

The permission dialog is launched on the **main** display, so it is invisible to the
background screen. `vscreen handoff in` (no package) moves it onto the background screen;
tap **"Allow all the time"** there, then relaunch the app. The app's "Shizuku" status text is
a snapshot taken at composition, so it can still read `permission not granted` after a grant
that actually worked — the probe is the source of truth.

The grant is bound to the signing certificate (the dialog lists
`B6A7 737B 7AA5 … 11BD`), so a build signed with a new key needs a fresh grant, and removing
the app drops it.

## Risks

- Shizuku user service lifetime tied to Shizuku, not the app → plan a reconnect path.
- OEM battery killers (Xiaomi/Huawei/Samsung) may kill the service → document per-OEM steps.

## Out of scope

Package installs (T02), PTY (T04), proxy (T05).

## Implementation notes (Phase 0 probe)

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
- "Start 30-minute soak" writes `probe/soak.sh` (heartbeat every 60 s with uid and
  `dumpsys power` wakefulness) and starts it detached, so the binder call returns at once;
  "Read soak log" reports the tick count and the first/last line.

## Cleanup rule (step 6)

`/data/local/tmp/aiaio/` lives outside the app sandbox and **survives uninstall** (observed:
after uninstalling the probe build the directory was still there, and the next install reused
it). Nothing else is left behind by the probe: no services, no scheduled jobs, no system
modification. A release build must therefore ship an explicit "remove runtime" action, and
must not create anything outside this directory.

Evidence to attach when run on device: screenshot of the probe card, `id -u` output, device
model/Android version, Shizuku version, and the 30-minute screen-off repeat.
