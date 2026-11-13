# T07 — Device baseline bench

**Owner:** Human + Android pod · **Depends on:** T01, T02 · **Status:** In progress (partial, environment-limited) — device identified and app observed; T01 dependency satisfied on this device, T02 still blocked; many metrics require adb/dumpsys/bench tooling not available via vscreen.

## Goal

Record the baseline numbers that later phases must not regress.

## Metrics

| Metric | How | Budget (initial) |
|---|---|---|
| Cold start (UI first frame) | `am start -W` + Macrobenchmark | < 1.5 s |
| Core ready (dispatch answers) | timestamp in app log | < 3.0 s |
| UI RSS (idle) | `dumpsys meminfo` | < 180 MB |
| Core RSS (idle) | `dumpsys meminfo :core` | < 120 MB |
| Proxy throughput | 100 streams × 10 min | no errors, < 250 MB RSS |
| Battery, proxy idle | 8 h screen-off, `dumpsys batterystats` | < 3%/h |
| Runtime disk | `du -sh /data/local/tmp/aiaio` | recorded |
| Terminal frame time | Macrobenchmark/JankStats | 95% < 16 ms at 80×24 |

## Acceptance criteria

- [ ] Numbers recorded with device model, Android version, ABI, and thermal state.
- [ ] At least two devices (one mid-range, one flagship) if available.
- [ ] Budgets written into `docs/agents/STATE.md` and updated on every phase exit.
