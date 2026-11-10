package io.github.kingkaonix.aiaio.core;

/**
 * JSON-RPC-ish façade over the Rust core. One process, one binder, no per-command AIDL
 * surface: adding a core command must not require an Android API change.
 */
interface ICoreService {
    boolean ping();

    /** Protocol version implemented by libaiaio_core.so (0 when the library failed to load). */
    int protocolVersion();

    /**
     * Dispatch a JSON request (see core/crates/runtime-proto) and return a JSON response.
     * Never throws across Binder except for RemoteException.
     */
    String dispatch(String request);
}
