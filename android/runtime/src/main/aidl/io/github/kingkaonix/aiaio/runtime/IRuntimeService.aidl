package io.github.kingkaonix.aiaio.runtime;

/**
 * Privileged surface of the Shizuku user service (shell UID). Every method here runs as
 * uid 2000; the app-side client must treat it as a boundary and validate inputs.
 *
 * Phase 0 exposes the minimum needed for T01: write a program under
 * /data/local/tmp/aiaio and execute it. PTY allocation and package install methods are
 * added in T02/T04.
 *
 * Method order is part of the wire contract (AIDL assigns transaction ids in declaration
 * order). Append new methods; never insert them in the middle.
 */
interface IRuntimeService {
    /** Effective uid of the service process. Must be 2000 (shell). */
    int uid();

    /**
     * Run a program and wait for it. Returns a JSON object:
     * {"code": 0, "output": "..."} or {"code": -1, "error": "..."}.
     * argv[0] must be an absolute path or resolvable on PATH.
     */
    String exec(in String[] argv, in String cwd);

    /**
     * Create [path] and any missing parents. The path must stay under
     * /data/local/tmp/aiaio (checked again on the service side).
     * Returns {"code": 0, "output": "<resolved path>"} or {"code": -1, "error": "..."}.
     */
    String mkdirs(in String path);

    /**
     * Write [data] to [path] and apply the Unix [mode] (for example 0700 = 448).
     * Returns {"code": 0, "output": "..."} or {"code": -1, "error": "..."}.
     */
    String writeFile(in String path, in byte[] data, int mode);
}
