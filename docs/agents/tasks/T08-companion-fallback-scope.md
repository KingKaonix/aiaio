# T08 — Companion-mode fallback scope (Story 2)

**Owner:** Core + Android UI + Product · **Depends on:** none (parallel to P0) · **Status:** In progress — planning scope/feature matrix documented; formal review/approval and pivot-time TBD resolution pending
**Spec:** `_bmad-output/specs/spec-aiaio-completion/` (CAP-1, CAP-6) · **Story:** 2

## Goal

Define an actionable Companion-mode fallback so a P0/kill-criterion pivot is explicit (no ad-hoc re-plan), while self-contained work continues.

## Deliverables

1. **Scope/contract doc:** local vs delegated responsibilities, UX/degradation, security/licensing boundaries, data/sync behavior, activation & re-entry path. *(documented in spec companion; review pending)*
2. **Feature matrix:** per in-scope area (providers/presets, MCP/skills/prompts, sync, proxy, usage/quota, runtime/bottles, terminal/PTY, import-export) marked local / delegated / unavailable / TBD-with-rationale. *(documented; TBDs called out for pivot resolution)*
3. **ADR/STATE updates:** record fallback decision-readiness, triggers, and invariants (no root/Termux, no GPLv3, no Play-only deps, no secret leakage). *(STATE linked; no new ADR yet — planning-only, no new cross-cutting decision beyond existing ADRs)*
4. **UX notes:** mode indicator, disabled-action rationale, onboarding adjustments, path back to self-contained. *(included in spec companion)*

## Acceptance criteria

- [x] Scope is documented; open items are explicitly listed (remote host integration in v1, remote auth/transport contract + threat model, per-feature TBD support, runtime-repo/signing/rights dependency, ADR linkage at pivot). *Formal review/approval pending.*
- [x] Triggers map to PLAN/SPEC kill criteria and activation is defined as a recorded, evidence-backed decision in `docs/agents/STATE.md` (not yet activated).
- [x] Invariants preserved in scope: no alternate privileged path; no stubbed/local execution presented as working; no root/Termux/GPLv3/Play-only deps/secret leakage.
- [x] Spec companion `companion-mode.md` and this task are consistent as of 2026-11-13.

## Evidence

- `_bmad-output/specs/spec-aiaio-completion/companion-mode.md` (updated 2026-11-13: detailed trigger mapping, per-area matrix with rationale, UX/security/data/migration boundaries, open items).
- `docs/agents/STATE.md` updated to reference fallback readiness and T08 status.
- Review/approval note and any linked ADR updates still pending.

## Progress

- 2026-11-13: Expanded scope/contract in spec companion with explicit kill-criterion trigger mapping, planned feature matrix (Local/Delegated/Unavailable/TBD + rationale), UX/security/licensing/data/migration sections, and open items requiring pivot-time resolution. STATE/task linked for decision-readiness; formal review and activation evidence remain open. No ADR added (planning-only; open item recorded).
