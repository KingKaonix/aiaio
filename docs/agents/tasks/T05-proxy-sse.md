# T05 — Proxy SSE proof

**Owner:** Core pod · **Depends on:** none (desktop core) · **Status:** Not started

## Goal

Prove the upstream proxy engine runs in the extracted core and streams a real SSE response
through a protocol transform, bound to loopback with token + peer-UID checks.

## Steps

1. Move `proxy/` into `core/crates/core` behind `PathProvider`/`EventSink` ports (minimal slice:
   `server`, `handlers`, `forwarder`, `providers/claude`, `streaming`).
2. Start on `127.0.0.1:0`; write the chosen port to a runtime file.
3. Require `Authorization: Bearer <token>`; reject bad tokens with 401.
4. Add a mock upstream that emits a Claude-format streaming response; route it through the
   Codex→Claude transform (or Claude passthrough) and assert chunks arrive incrementally.
5. Verify backpressure: slow client must not buffer unboundedly.
6. Verify no non-loopback bind is possible via config.

## Acceptance criteria

- [ ] `curl -N` through the proxy receives the first chunk < 500 ms and the stream completes.
- [ ] Invalid/missing token → 401; token is never logged.
- [ ] Peer UID check rejects a connection from a different app UID (test with `run-as`/helper).
- [ ] Slow-client test: memory stays bounded (record RSS).
- [ ] 100 concurrent streams for 10 minutes with no errors and no leaks.
- [ ] Same tests pass on the desktop build and in Android CI.

## Evidence

curl transcript with timings, memory samples, concurrency report, and the test names in CI.
