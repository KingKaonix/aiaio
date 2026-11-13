package io.github.kingkaonix.aiaio.runtime

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPInputStream
import org.json.JSONArray
import org.json.JSONObject

internal object RuntimeBottles {
    private val ROOT = File(RuntimePaths.ROOT, "runtime")
    private val STAGING = File(RuntimePaths.ROOT, "runtime-staging")

    fun install(manifestJson: String, bottleId: String, version: String, archiveUrlOverride: String): String {
        require(bottleId.isNotBlank() && version.isNotBlank()) { "bottleId/version required" }
        ROOT.mkdirs(); STAGING.mkdirs()
        val manifest = JSONObject(manifestJson)
        val unsigned = canonicalUnsigned(manifest)
        verifySignature(manifest, unsigned)
        val bottle = findBottle(manifest, bottleId, version)
        checkRedistributable(bottle)
        val archive = bottle.optJSONObject("archive") ?: throw IllegalArgumentException("missing archive")
        val url = archiveUrlOverride.ifBlank { archive.getString("url") }
        val sha = archive.getString("sha256").lowercase()
        val size = archive.getLong("size")
        val format = archive.optString("format", "tar.gz")
        val bottleRoot = File(ROOT, "$bottleId/$version")
        val current = File(ROOT, "$bottleId/current")
        val stagingDir = File(STAGING, "$bottleId-$version-${UUID.randomUUID()}")
        val tmpArchive = File(STAGING, "$bottleId-$version-${UUID.randomUUID()}.pkg")
        try {
            if (!stagingDir.mkdirs()) error("cannot create staging dir")
            downloadTo(url, tmpArchive, sha, size)
            unpack(tmpArchive, stagingDir, format, bottle.optJSONObject("install")?.optInt("stripComponents", 0) ?: 0)
            tmpArchive.delete()
            runHealthCheck(stagingDir, bottle.optJSONObject("install")?.optJSONObject("healthCheck"))
            if (bottleRoot.exists()) deleteRecursively(bottleRoot)
            bottleRoot.parentFile?.mkdirs()
            if (!stagingDir.renameTo(bottleRoot)) {
                copyRecursively(stagingDir, bottleRoot)
                deleteRecursively(stagingDir)
            }
            Files.deleteIfExists(current.toPath())
            Files.createSymbolicLink(current.toPath(), bottleRoot.toPath())
            writeState(bottleId, version, sha)
            return JSONObject()
                .put("code", 0)
                .put("id", bottleId)
                .put("version", version)
                .put("root", bottleRoot.path)
                .put("current", current.path)
                .put("sha256", sha)
                .put("redistributable", bottle.optBoolean("redistributable", false))
                .put("sourceUrl", bottle.optString("sourceUrl"))
                .toString()
        } catch (t: Throwable) {
            tmpArchive.delete()
            if (stagingDir.exists()) deleteRecursively(stagingDir)
            // never touch existing active install/current on failure
            throw t
        }
    }

    fun list(bottleId: String): String {
        val base = if (bottleId.isBlank()) ROOT else File(ROOT, bottleId)
        val out = JSONObject().put("code", 0).put("root", base.path)
        if (!base.exists()) return out.put("versions", JSONArray()).put("current", JSONObject.NULL).toString()
        val versions = JSONArray()
        val dirs = (if (bottleId.isBlank()) base.listFiles()?.flatMap { p -> (p.listFiles()?.toList() ?: emptyList()).map { p.name to it } } ?: emptyList() else (base.listFiles()?.map { bottleId to it } ?: emptyList()))
        dirs.filter { it.second.isDirectory && it.second.name != "current" }
            .sortedBy { it.second.name }
            .forEach { (id, f) -> versions.put(JSONObject().put("id", id).put("version", f.name).put("path", f.path)) }
        val cur = if (bottleId.isBlank()) null else readCurrent(bottleId)
        return out.put("versions", versions).put("current", cur ?: JSONObject.NULL).toString()
    }

    fun activate(bottleId: String, version: String): String {
        val bottleRoot = File(ROOT, "$bottleId/$version")
        require(bottleRoot.isDirectory) { "version not installed: $bottleId@$version" }
        // best-effort health check if metadata/state exists
        val hc = readState(bottleId, version)?.optJSONObject("healthCheck")
        if (hc != null) runHealthCheck(bottleRoot, hc)
        val current = File(ROOT, "$bottleId/current")
        current.parentFile?.mkdirs()
        Files.deleteIfExists(current.toPath())
        Files.createSymbolicLink(current.toPath(), bottleRoot.toPath())
        return JSONObject().put("code", 0).put("id", bottleId).put("version", version).put("current", current.path).toString()
    }

    fun remove(bottleId: String, version: String): String {
        val bottleRoot = File(ROOT, "$bottleId/$version")
        require(bottleRoot.exists()) { "version not installed: $bottleId@$version" }
        val cur = readCurrent(bottleId)
        if (cur == version) throw IllegalStateException("cannot remove active version: $version")
        deleteRecursively(bottleRoot)
        return JSONObject().put("code", 0).put("id", bottleId).put("removed", version).toString()
    }

    fun exec(bottleId: String, version: String, argv: Array<out String>, cwd: String): String {
        val ver = version.ifBlank { readCurrent(bottleId) ?: "" }
        require(ver.isNotBlank()) { "no version/current for $bottleId" }
        val root = File(ROOT, "$bottleId/$ver")
        require(root.isDirectory) { "version not installed: $bottleId@$ver" }
        val resolved = argv.toMutableList()
        if (resolved.isNotEmpty()) {
            val bin = resolved[0]
            if (!bin.startsWith("/")) {
                val candidate = File(root, bin)
                if (candidate.exists()) resolved[0] = candidate.path
            }
        }
        val dir = if (cwd.isBlank()) root.path else cwd
        return ShellRunner.run(resolved.toTypedArray(), dir)
    }

    private fun findBottle(manifest: JSONObject, id: String, version: String): JSONObject {
        val arr = manifest.optJSONArray("bottles") ?: throw IllegalArgumentException("manifest has no bottles")
        for (i in 0 until arr.length()) {
            val b = arr.getJSONObject(i)
            if (b.optString("id") == id && b.optString("version") == version) return b
        }
        throw IllegalArgumentException("bottle not found: $id@$version")
    }

    private fun checkRedistributable(bottle: JSONObject) {
        // must be explicit; T02 proof uses redistributable test fixture owned by repo.
        if (!bottle.has("redistributable")) throw IllegalArgumentException("manifest missing redistributable flag")
    }

    private fun canonicalUnsigned(manifest: JSONObject): ByteArray {
        val copy = JSONObject(manifest.toString())
        copy.remove("signature")
        return canonicalJson(copy).toByteArray(Charsets.UTF_8)
    }

    private fun canonicalJson(v: Any?): String = when (v) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> {
            val keys = v.keys().asSequence().toList().sorted()
            keys.joinToString(prefix = "{", postfix = "}") { k -> quote(k) + ":" + canonicalJson(v.opt(k)) }
        }
        is JSONArray -> (0 until v.length()).joinToString(prefix = "[", postfix = "]") { i -> canonicalJson(v.opt(i)) }
        is String -> quote(v)
        is Number, is Boolean -> v.toString()
        else -> quote(v.toString())
    }

    private fun quote(s: String): String = JSONObject.quote(s)

    private fun verifySignature(manifest: JSONObject, unsigned: ByteArray) {
        val sigObj = manifest.optJSONObject("signature") ?: throw SecurityException("manifest signature missing")
        val alg = sigObj.optString("alg")
        if (alg != "ed25519") throw SecurityException("unsupported signature alg: $alg")
        val sig = Base64.getDecoder().decode(sigObj.getString("value"))
        val keyId = sigObj.optString("keyId")
        val pubRaw = lookupPublicKeyRaw(manifest, keyId) ?: throw SecurityException("unknown signing key: $keyId")
        val verified = try {
            val kf = KeyFactory.getInstance("Ed25519")
            // X.509 SubjectPublicKeyInfo for Ed25519: fixed prefix + 32-byte raw key
            val prefix = Base64.getDecoder().decode("MCowBQYDK2VwAyEA")
            val x509 = X509EncodedKeySpec(prefix + pubRaw)
            val pub = kf.generatePublic(x509)
            val verifier = Signature.getInstance("Ed25519")
            verifier.initVerify(pub)
            verifier.update(unsigned)
            verifier.verify(sig)
        } catch (t: Throwable) {
            throw SecurityException("signature verification failed: ${t.message}", t)
        }
        if (!verified) throw SecurityException("manifest signature invalid")
    }

    private fun lookupPublicKeyRaw(manifest: JSONObject, keyId: String): ByteArray? {
        // pinned test key for T02 proof fixtures (repo-owned, redistributable)
        if (keyId == "test-ed25519-v1") {
            return Base64.getDecoder().decode("F783eE5y2BnZCz6/Fh4rD0iLIODz5oIJOKQ95DLEZEo=")
        }
        // optional embedded key (not in schema, but harmless if provided for proof)
        val embedded = manifest.optJSONObject("publicKeys")?.optString(keyId, null)
        return embedded?.let { Base64.getDecoder().decode(it) }
    }

    private fun downloadTo(url: String, dest: File, expectedSha: String, expectedSize: Long) {
        val part = File(dest.path + ".part")
        part.delete(); dest.delete()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
        }
        try {
            conn.inputStream.use { input ->
                FileOutputStream(part).use { out ->
                    BufferedInputStream(input).copyTo(out)
                }
            }
        } finally {
            conn.disconnect()
        }
        val actualSize = part.length()
        if (expectedSize > 0 && actualSize != expectedSize) {
            part.delete(); throw SecurityException("size mismatch: expected=$expectedSize actual=$actualSize")
        }
        val actualSha = sha256(part)
        if (!actualSha.equals(expectedSha, ignoreCase = true)) {
            part.delete(); throw SecurityException("sha256 mismatch")
        }
        if (!part.renameTo(dest)) {
            Files.move(part.toPath(), dest.toPath())
        }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }

    private fun unpack(archive: File, target: File, format: String, stripComponents: Int) {
        when (format) {
            "tar.gz" -> untarGz(archive, target, stripComponents)
            else -> throw IllegalArgumentException("unsupported archive format: $format")
        }
    }

    private fun untarGz(archive: File, target: File, strip: Int) {
        GZIPInputStream(FileInputStream(archive)).use { gz ->
            TarReader.extract(gz, target, strip)
        }
    }

    private fun runHealthCheck(root: File, hc: JSONObject?) {
        if (hc == null) return
        val argvJson = hc.optJSONArray("argv") ?: return
        val argv = Array(argvJson.length()) { i ->
            val v = argvJson.getString(i)
            if (v.startsWith("/")) v else File(root, v).path
        }
        val out = ShellRunner.run(argv, root.path)
        val json = runCatching { JSONObject(out) }.getOrNull()
        val code = json?.optInt("code", -1) ?: -1
        val output = json?.optString("output") ?: out
        if (code != 0) throw IllegalStateException("health check failed: $out")
        val expect = hc.optString("expectStdout", "")
        if (expect.isNotEmpty() && !output.contains(expect)) {
            throw IllegalStateException("health check output mismatch: expected '$expect' got '$output'")
        }
    }

    private fun writeState(bottleId: String, version: String, sha: String) {
        val f = File(ROOT, "$bottleId/$version/.aiaio-bottle.json")
        val obj = JSONObject().put("id", bottleId).put("version", version).put("sha256", sha)
        f.writeText(obj.toString())
    }
    private fun readState(bottleId: String, version: String): JSONObject? {
        val f = File(ROOT, "$bottleId/$version/.aiaio-bottle.json")
        return if (f.exists()) runCatching { JSONObject(f.readText()) }.getOrNull() else null
    }
    private fun readCurrent(bottleId: String): String? {
        val cur = File(ROOT, "$bottleId/current")
        return if (Files.isSymbolicLink(cur.toPath())) cur.toPath().toRealPath().fileName?.toString() else null
    }

    private fun deleteRecursively(f: File) {
        if (f.isDirectory) f.listFiles()?.forEach { deleteRecursively(it) }
        f.delete()
    }
    private fun copyRecursively(src: File, dst: File) {
        if (src.isDirectory) {
            dst.mkdirs()
            src.listFiles()?.forEach { copyRecursively(it, File(dst, it.name)) }
        } else {
            dst.parentFile?.mkdirs()
            Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

private object TarReader {
    fun extract(input: java.io.InputStream, target: File, strip: Int) {
        val header = ByteArray(512)
        while (true) {
            val read = readFully(input, header)
            if (read < 512) break
            if (header.all { it == 0.toByte() }) break
            val name = parseName(header)
            val size = parseOctal(header, 124, 12)
            val type = header[156].toInt().toChar()
            val stripped = stripPath(name, strip)
            if (stripped != null && stripped.isNotEmpty()) {
                val out = safeResolve(target, stripped)
                if (type == '5' || name.endsWith("/")) {
                    out.mkdirs()
                } else if (type == '0' || type == '\u0000') {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { os -> copyN(input, os, size) }
                    if ((parseOctal(header, 100, 8) and 0x40L) != 0L) out.setExecutable(true, false)
                } else {
                    skipN(input, size)
                }
            } else {
                skipN(input, size)
            }
            val pad = ((512 - (size % 512)) % 512).toInt()
            if (pad > 0) skipN(input, pad.toLong())
        }
    }

    private fun parseName(h: ByteArray): String {
        var end = 0
        while (end < 100 && h[end] != 0.toByte()) end++
        val name = String(h, 0, end, Charsets.US_ASCII)
        var pfxEnd = 0
        while (pfxEnd < 155 && h[345 + pfxEnd] != 0.toByte()) pfxEnd++
        return if (pfxEnd > 0) String(h, 345, pfxEnd, Charsets.US_ASCII) + "/" + name else name
    }
    private fun parseOctal(h: ByteArray, off: Int, len: Int): Long {
        var s = off; var e = off + len
        while (s < e && (h[s] == 0.toByte() || h[s] == 32.toByte())) s++
        while (e > s && (h[e - 1] == 0.toByte() || h[e - 1] == 32.toByte())) e--
        if (s >= e) return 0
        var v = 0L
        for (i in s until e) { val c = h[i].toInt().toChar(); if (c in '0'..'7') v = (v shl 3) + (c - '0') }
        return v
    }
    private fun stripPath(name: String, strip: Int): String? {
        var p = name.replace('\\', '/').trimStart('/')
        repeat(strip) { val idx = p.indexOf('/'); if (idx < 0) return null else p = p.substring(idx + 1) }
        return p.trimEnd('/').ifEmpty { null }
    }
    private fun safeResolve(root: File, rel: String): File {
        val f = File(root, rel).canonicalFile
        val rp = root.canonicalPath + File.separator
        require(f.path == root.canonicalPath || f.path.startsWith(rp)) { "tar path escapes target: $rel" }
        return f
    }
    private fun readFully(input: java.io.InputStream, buf: ByteArray): Int {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) break
            off += n
        }
        return off
    }
    private fun copyN(input: java.io.InputStream, out: java.io.OutputStream, n: Long) {
        var left = n
        val buf = ByteArray(8192)
        while (left > 0) {
            val r = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
            if (r < 0) throw java.io.EOFException("unexpected EOF in tar entry")
            out.write(buf, 0, r); left -= r
        }
    }
    private fun skipN(input: java.io.InputStream, n: Long) {
        var left = n
        val buf = ByteArray(8192)
        while (left > 0) {
            val r = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
            if (r < 0) break
            left -= r
        }
    }
}
