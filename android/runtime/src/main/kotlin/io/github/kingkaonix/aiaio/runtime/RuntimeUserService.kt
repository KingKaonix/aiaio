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

    override fun exec(argv: Array<out String>?, cwd: String?): String =
        ShellRunner.run(argv?.map { it.orEmpty() }?.toTypedArray() ?: emptyArray(), cwd.orEmpty())
}
