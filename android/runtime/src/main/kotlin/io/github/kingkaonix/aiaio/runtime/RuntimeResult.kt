package io.github.kingkaonix.aiaio.runtime

import org.json.JSONObject

/** Uniform JSON envelope returned by every [IRuntimeService] method. */
internal object RuntimeResult {
    fun ok(output: String = ""): String = JSONObject()
        .put("code", 0)
        .put("output", output)
        .toString()

    fun error(message: String, code: Int = -1): String = JSONObject()
        .put("code", code)
        .put("error", message)
        .toString()
}
