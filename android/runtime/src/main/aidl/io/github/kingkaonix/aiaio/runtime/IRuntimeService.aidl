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

    /**
     * Read [path] and return {"code":0,"path":"...","data":"<base64>","len":N}
     * or {"code":-1,"error":"..."}. [data] is standard Base64 (no line breaks).
     */
    String readFile(in String path);

    /**
     * Return {"code":0,"path":"...","exists":bool,"isFile":bool,"isDir":bool,
     * "len":N,"mode":octal-int} or {"code":-1,"error":"..."}.
     */
    String stat(in String path);

    /**
     * List directory entries at [path], sorted by name.
     * Returns {"code":0,"path":"...","entries":[{"name","isDir","len"},...]}
     * or {"code":-1,"error":"..."}.
     */
    String listDir(in String path);

    /**
     * Rename [from] to [to] within the runtime root.
     * Returns {"code":0,"from":"...","to":"..."} or {"code":-1,"error":"..."}.
     */
    String rename(in String from, in String to);

    /**
     * Remove a file or empty directory at [path].
     * Returns {"code":0,"path":"..."} or {"code":-1,"error":"..."}.
     */
    String remove(in String path);

    /**
     * Restore [path] from its .aiaio-bak sidecar (kept by atomic writes).
     * Returns {"code":0,"path":"...","restored":true} or {"code":-1,"error":"..."}.
     */
    String restoreBackup(in String path);

    /**
     * Atomic write: temp file in target dir -> fsync -> chmod([mode]) -> backup previous
     * as <name>.aiaio-bak -> rename -> dir sync -> read-back verify.
     * Returns {"code":0,"path":"...","bytes":N,"mode":M} or {"code":-1,"error":"..."}.
     */
    String writeFileAtomic(in String path, in byte[] data, int mode);

    /**
     * Install a bottle described by manifest JSON [manifestJson] for bottle [bottleId] and [version].
     * [archiveUrlOverride] when non-empty replaces the manifest archive URL (used for local/proof fixtures).
     * Performs manifest/signature/checksum verification, staged download+unpack, health check,
     * and atomic activation only on success.
     * Returns JSON result or {"code":-1,"error":"..."}.
     */
    String bottleInstall(in String manifestJson, in String bottleId, in String version, in String archiveUrlOverride);

    /**
     * List installed bottle versions and current pointer under runtime root.
     * Returns JSON result or {"code":-1,"error":"..."}.
     */
    String bottleList(in String bottleId);

    /**
     * Switch current symlink for [bottleId] to [version] after health check.
     * Returns JSON result or {"code":-1,"error":"..."}.
     */
    String bottleActivate(in String bottleId, in String version);

    /**
     * Remove an installed non-current version (GC helper).
     * Returns JSON result or {"code":-1,"error":"..."}.
     */
    String bottleRemove(in String bottleId, in String version);

    /**
     * Run bottle health check (or arbitrary argv relative to bottle root when healthCheck not used by caller flow).
     * Returns exec-style JSON result.
     */
    String bottleExec(in String bottleId, in String version, in String[] argv, in String cwd);
}
