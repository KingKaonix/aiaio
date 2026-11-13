# T10 — Core extraction foundations & DB v18 sync schema (Story 3 prep)

**Owner:** Core pod · **Depends on:** P0 direction, contract agreement · **Status:** Not started
**Spec:** `_bmad-output/specs/spec-aiaio-completion/` (CAP-2, CAP-3) · **Story:** 3

## Goal

Advance contract-first extraction of Tauri-free core and introduce DB schema foundations required for merge-based sync.

## Scope (initial slice)

- Lock/extend `dispatch` + event + port contracts; document schema/IDL for cross-pod use.
- Incremental extraction plan from `src-tauri/` (services, database, proxy, deeplink, session_manager, live, mode) with upstream golden/fixture oracle.
- Design DB v18: per-entity `updated_at`, `origin_device`, `deleted_at` (tombstone), `rev`; `devices` table; oplog hooks/append-only model — aligned with ADR-0003 and `runtime-proto` sync meta types.
- Keep desktop Tauri shell building on shared core as canary (CI).

## Acceptance criteria

- [ ] Contract/schema documented and reviewed; extraction slices ordered with owners.
- [ ] DB v18 migration design reviewed; compatible with deterministic merge/tombstones.
- [ ] Initial golden/parity tests ported or harness wired for extracted slice.

## Evidence

Design doc/ADR addendum, migration draft, test run/CI links (to be added).

## Notes

- Coordinate tightly with Story 4 (merge engine + 3-device CI sim). Do not bolt sync metadata on late.
