# T02 — Bottle install proof

**Owner:** Runtime pod · **Depends on:** T01 · **Status:** Not started

## Goal

Download, verify, unpack and run a runtime bottle (Node.js + a tiny npm package) inside
`/data/local/tmp/aiaio/` using the Shizuku service, then update it and roll back.

## Steps

1. Define a minimal bottle archive layout (tar.zst or tar.gz) and manifest entry
   (see `tools/bottles/manifest.schema.json`).
2. Ship a signed manifest; verify Ed25519 signature + per-file SHA-256 + size before unpack.
3. Unpack into `/data/local/tmp/aiaio/runtime/<version>` and atomically switch the `current`
   symlink only after a successful health check (`node -v`).
4. Install a toy npm package into a tool prefix and execute it via the service.
5. Publish v2 of the bottle, update, then roll back to v1.
6. Interrupt a download midway and verify no partial install is activated.

## Acceptance criteria

- [ ] Signature and checksum failures abort the install with a clear error.
- [ ] `node -v` returns the manifest's pinned version via the service.
- [ ] The toy package runs and its output is returned to the app.
- [ ] Update + rollback leave exactly one active version; stale versions are GC-able.
- [ ] Interrupted download never activates a partial runtime.
- [ ] Manifest records license, source URL and `redistributable` flag per bottle.

## Evidence

Manifest hash, install log timeline, `ls -la /data/local/tmp/aiaio/runtime`, output of the toy
package, and the rollback transcript.

## Risks

- GitHub asset size/bandwidth; runtimes are 50–300 MB per tool.
- Proprietary tool redistribution rights (see ADR-0005) — manifest must not lie about this.
