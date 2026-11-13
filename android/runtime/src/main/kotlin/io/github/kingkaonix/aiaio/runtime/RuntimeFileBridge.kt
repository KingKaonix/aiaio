package io.github.kingkaonix.aiaio.runtime

import java.util.Base64
import org.json.JSONObject

class RuntimeFileBridge(private val manager: RuntimeManager) {

    fun read(path: String): Result<ByteArray> = runCatching {
        val json = requireOk(manager.readFile(path))
        val data = json.optString("data", "")
        if (data.isEmpty() && json.optInt("len", 0) != 0) {
            error("missing data for $path")
        }
        Base64.getDecoder().decode(data)
    }

    fun writeAtomic(path: String, bytes: ByteArray, mode: Int = 0x180): Result<JSONObject> = runCatching {
        requireOk(manager.writeFileAtomic(path, bytes, mode))
    }

    fun stat(path: String): Result<JSONObject> = runCatching {
        requireOk(manager.stat(path))
    }

    fun listDir(path: String): Result<JSONObject> = runCatching {
        requireOk(manager.listDir(path))
    }

    fun rename(from: String, to: String): Result<JSONObject> = runCatching {
        requireOk(manager.rename(from, to))
    }

    fun remove(path: String): Result<JSONObject> = runCatching {
        requireOk(manager.remove(path))
    }

    fun restoreBackup(path: String): Result<JSONObject> = runCatching {
        requireOk(manager.restoreBackup(path))
    }

    fun mkdirs(path: String): Result<JSONObject> = runCatching {
        requireOk(manager.mkdirs(path))
    }

    private fun requireOk(raw: String): JSONObject {
        val json = JSONObject(raw)
        val code = json.optInt("code", -1)
        if (code != 0) {
            val err = json.optString("error", json.optString("output", "unknown error"))
            throw IllegalStateException(err)
        }
        return json
    }
}
