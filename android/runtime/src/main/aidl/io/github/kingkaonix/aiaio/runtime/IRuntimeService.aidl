package io.github.kingkaonix.aiaio.runtime;

/**
 * Privileged surface of the Shizuku user service (shell UID). Every method here runs as
 * uid 2000; the app-side client must treat it as a boundary and validate inputs.
 *
 * Phase 0 exposes the minimum needed for T01 (exec proof). File I/O, PTY allocation and
 * package install methods are added in T02/T04.
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
}
