# Runtime bottles

Bottles are the self-contained tool payloads aiaio installs into
`/data/local/tmp/aiaio/runtime/<tool>/<version>/`.

This directory contains only **recipes and the manifest schema**. The signed manifest and the
archives themselves live in the separate [`aiaio-runtime`](https://github.com/kingkaonix/aiaio-runtime)
repository as release assets.

## Files

| File | Purpose |
|---|---|
| `manifest.schema.json` | JSON Schema for the signed catalog served to the app |
| `recipes/*.json` | Build/install recipe per tool (source, version, build steps, files) |
| `keys/*.pub` | Pinned Ed25519 public keys (added before first release) |

## Rules

1. **Pinned versions only.** No `latest`, no ranges.
2. **Redistribution is opt-in per tool.** `redistributable: true` requires recorded permission
   (license allows it, or we have written clearance). Otherwise the manifest points at the
   official download URL and the app asks the user for consent at install time.
3. **Never trust an unsigned archive.** The app verifies the manifest signature, then the
   archive SHA-256 and size, before unpacking.
4. **Deterministic archives.** Sorted entries, fixed mtime/uid/gid, `tar.zst`.
5. **Health check required.** A bottle is not activated until its health check passes.
6. **No app-private extraction.** Bottles are unpacked by the Shizuku service into
   `/data/local/tmp/aiaio/`.

## Recipe shape (informative)

```json
{
  "id": "node",
  "version": "24.11.0",
  "license": "MIT",
  "sourceUrl": "https://nodejs.org/dist/v24.11.0/",
  "redistributable": true,
  "platforms": ["arm64-v8a", "x86_64"],
  "build": {
    "type": "prebuilt",
    "urlTemplate": "https://nodejs.org/dist/v{version}/node-v{version}-linux-arm64.tar.xz",
    "stripComponents": 1,
    "bin": ["bin/node", "bin/npm", "bin/npx"]
  },
  "healthCheck": { "argv": ["bin/node", "-v"], "expectStdout": "v24.11.0" }
}
```

## Publishing checklist

- [ ] License and redistribution status recorded.
- [ ] Archive built from a pinned source tag; build recipe committed.
- [ ] SHA-256 + size in the manifest; Ed25519 signature attached.
- [ ] Health check verified on arm64-v8a and x86_64.
- [ ] Rollback tested against the previous version.
- [ ] `blocked` entry added for any revoked version.
