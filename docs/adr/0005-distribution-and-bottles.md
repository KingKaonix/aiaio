# ADR-0005 — Distribution and runtime bottles

**Status:** accepted · 2026-11-10

## Context

The app is self-contained: it installs and runs CLI tools itself. Those tools are separate
programs (Node.js, git, ripgrep, Claude Code, Codex CLI, Gemini CLI, OpenCode, …), not linked
into the app. They cannot live in app-private storage (W^X), so they live in
`/data/local/tmp/aiaio/runtime/` under the Shizuku service's control.

Two constraints shape distribution:

1. **Licensing.** Some tools are FOSS and redistributable. Others (notably Claude Code) are
   proprietary and may forbid redistribution. Hosting a binary we have no right to host is a
   takedown risk and an F-Droid rejection.
2. **Store policy.** F-Droid ships only FOSS and forbids downloading proprietary add-ons at
   runtime. GitHub Releases has no such restriction.

## Decision

### Flavors

| Flavor | Channel | Runtime catalog |
|---|---|---|
| `github` | GitHub Releases / Obtainium | Full catalog. Bottles we may redistribute, plus proprietary tools installed from the official source with explicit user consent. |
| `libre` | F-Droid (planned) | FOSS tools only. Proprietary tools are hidden with a short explainer, never prompted for download. |

The app code is identical; only the catalog and `BuildConfig.TOOL_CATALOG` differ.

### Bottles

- A **bottle** is a versioned, self-contained tool payload plus metadata.
- Bottles are hosted in a separate repository (`aiaio-runtime`) as release assets; the app
  repository only holds recipes and the manifest schema.
- Archive format: `tar.zst` (deterministic layout, sorted entries, fixed mtime/uid/gid).
- Install layout: `/data/local/tmp/aiaio/runtime/<tool>/<version>/` with a `current` symlink
  switched atomically only after a health check succeeds.
- Updates install side-by-side; rollback flips the symlink back; stale versions are GC-able.
- Interrupted downloads can never activate a partial runtime (staging dir + verify + rename).

### Manifest and trust

- Signed manifest (Ed25519) with a key id, plus per-file SHA-256 and size. The app verifies
  signature, checksums and size **before** unpacking.
- Manifest fields per bottle: `id`, `name`, `version`, `license`, `sourceUrl`,
  `redistributable`, `archive { url, sha256, size, format }`, `install { stripComponents, bin }`,
  `deps`, `capabilities`, `platforms`.
- `redistributable: false` means the manifest points at the official download URL; the app
  fetches it at install time after explicit consent and still verifies checksums. We are then
  not the redistributor.
- Every recipe and release asset records its license and redistribution status. Unknown status
  defaults to `false`.
- Revocation: the manifest has a monotonic version; a pinned key can publish an empty/blocked
  entry for a compromised version.

### Integrity and provenance

- Release assets are built from pinned source tags with recorded build recipes; CI attaches
  SHA-256 sums and the signed manifest.
- The app never executes a bottle whose signature or checksums fail, even if the user insists.
- No bottle is ever extracted into app-private storage or linked into the APK.

## Consequences

- The `github` flavor can ship Claude Code; the `libre` flavor cannot. Users who want Claude
  Code install the GitHub build.
- Legal review is per tool before first publication; the manifest must not lie about
  `redistributable`.
- F-Droid metadata must declare the network/runtime-download anti-feature for `libre` only if
  it still downloads anything (it downloads FOSS bottles). This is disclosed in the listing.
- Bottle size (50–300 MB per tool) means per-tool downloads, no "download everything" default.

## Rejected

- Bundling runtimes in the APK: APK size limits, ABI matrix, update coupling, license
  contamination.
- Termux as the runtime provider: violates self-contained requirement.
- Redistributing proprietary CLIs without permission: legal and store risk.
