# Phase 0 — proofs and go/no-go

**Goal:** prove the three load-bearing assumptions before investing in feature work.

1. A shell-UID Shizuku user service can own and execute a runtime in `/data/local/tmp/aiaio/`
   without root (T01, T02).
2. The Rust core can read/write runtime configs atomically and host the proxy (T03, T05).
3. The app builds with `targetSdk 36` and 16 KB page alignment on CI (T06).

## Task order

```
T06 (build)  ──►  T01 (exec)  ──►  T02 (bottle)  ──►  T03 (config)
                        │
                        └────►  T04 (pty)
T05 (proxy) runs in parallel on the desktop build.
T07 (bench) runs after T01/T02, before Phase 3.
```

## Gate

Proceed to Phase 1 only when:

- [ ] T01 and T02 pass on a real device (Android 13+ and 14+ ideally).
- [ ] T03 passes with byte-identical round trip and symlink-escape rejection.
- [ ] T05 streams a real SSE response through the transform pipeline.
- [ ] T06 is green in CI (both ABIs, 16 KB aligned).
- [ ] T07 numbers are recorded and within budget (cold start, RSS, proxy throughput).

If T01 fails and cannot be fixed without root, **stop and pivot to Companion mode**
(plan §3.6). Do not start the UI or the terminal on a broken foundation.

## Evidence format

Every task must attach: command, device/OS/ABI, Shizuku version, logcat excerpt, and where
relevant a screen recording or APK hash. "Works" without evidence is `Unverified`.
