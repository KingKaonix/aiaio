package io.github.kingkaonix.aiaio.runtime

import android.os.Process

/**
 * Privileged surface executed in a shell-UID process that Shizuku owns.
 *
 * Shizuku instantiates this class by name (`UserServiceArgs`) and calls the AIDL Stub
 * methods. It must:
 *  - extend [IRuntimeService.Stub] (not `android.app.Service`);
 *  - have a public no-arg constructor;
 *  - stay in the APK (see proguard-rules.pro and the stable `tag` in RuntimeManager).
 *
 * Do not add Android UI or app-lifecycle assumptions: this is not a normal app process.
 */
class RuntimeUserService : IRuntimeService.Stub() {
    override fun uid(): Int = Process.myUid()

    override fun mkdirs(path: String?): String = runCatching {
        RuntimeFiles.mkdirs(path.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun writeFile(path: String?, data: ByteArray?, mode: Int): String = runCatching {
        RuntimeFiles.writeFile(path.orEmpty(), data ?: ByteArray(0), mode)
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun readFile(path: String?): String = runCatching {
        RuntimeFiles.readFile(path.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun stat(path: String?): String = runCatching {
        RuntimeFiles.stat(path.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun listDir(path: String?): String = runCatching {
        RuntimeFiles.listDir(path.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun rename(from: String?, to: String?): String = runCatching {
        RuntimeFiles.rename(from.orEmpty(), to.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun remove(path: String?): String = runCatching {
        RuntimeFiles.remove(path.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun restoreBackup(path: String?): String = runCatching {
        RuntimeFiles.restoreBackup(path.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun writeFileAtomic(path: String?, data: ByteArray?, mode: Int): String = runCatching {
        RuntimeFiles.writeFileAtomic(path.orEmpty(), data ?: ByteArray(0), mode)
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun bottleInstall(manifestJson: String?, bottleId: String?, version: String?, archiveUrlOverride: String?): String = runCatching {
        RuntimeBottles.install(manifestJson.orEmpty(), bottleId.orEmpty(), version.orEmpty(), archiveUrlOverride.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun bottleList(bottleId: String?): String = runCatching {
        RuntimeBottles.list(bottleId.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun bottleActivate(bottleId: String?, version: String?): String = runCatching {
        RuntimeBottles.activate(bottleId.orEmpty(), version.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun bottleRemove(bottleId: String?, version: String?): String = runCatching {
        RuntimeBottles.remove(bottleId.orEmpty(), version.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun bottleExec(bottleId: String?, version: String?, argv: Array<out String>?, cwd: String?): String = runCatching {
        RuntimeBottles.exec(bottleId.orEmpty(), version.orEmpty(), argv?.map { it.orEmpty() }?.toTypedArray() ?: emptyArray(), cwd.orEmpty())
    }.getOrElse { RuntimeResult.error(it.message ?: it.javaClass.name) }

    override fun exec(argv: Array<out String>?, cwd: String?): String =
        ShellRunner.run(argv?.map { it.orEmpty() }?.toTypedArray() ?: emptyArray(), cwd.orEmpty())
}
