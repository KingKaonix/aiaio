package io.github.kingkaonix.aiaio

import io.github.kingkaonix.aiaio.runtime.RuntimePaths

/**
 * Shared constants for the T01 screen-off soak (see docs/agents/tasks/T01-shizuku-exec.md).
 *
 * The soak used to be a detached shell script. That proved nothing about the app staying
 * alive: when the app process died, the Shizuku user service died with it and the ticks
 * stopped. T01 step 5 asks for the app to keep the service alive, so the app now drives the
 * heartbeat itself from a foreground service and only the shell commands run in the service.
 */
object Soak {
    const val MINUTES = 30
    const val CHANNEL_ID = "soak"
    const val NOTIFICATION_ID = 1001

    val DIR: String = "${RuntimePaths.ROOT}/probe"
    val SCRIPT: String = "$DIR/soak.sh"
    val LOG: String = "$DIR/soak.log"
    val PROBE: String = "$DIR/probe.sh"

    val PROBE_BODY = """
        #!/system/bin/sh
        echo "probe-uid=${'$'}(id -u)"
        echo "probe-user=${'$'}(id -un 2>/dev/null || echo unknown)"
        echo "probe-dir=${'$'}(pwd)"
    """.trimIndent()

    /** `soak.sh start` truncates the log, `tick N` appends one heartbeat, `final` re-execs the probe. */
    val SCRIPT_BODY = """
        #!/system/bin/sh
        log=/data/local/tmp/aiaio/probe/soak.log
        wake() { dumpsys power | sed -n 's/.*mWakefulness=//p' | head -n 1; }
        case "${'$'}1" in
          start)
            : > "${'$'}log"
            echo "start uid=${'$'}(id -u) at ${'$'}(date +%H:%M:%S) wake=${'$'}(wake)" > "${'$'}log"
            ;;
          tick)
            echo "tick=${'$'}2 uid=${'$'}(id -u) at ${'$'}(date +%H:%M:%S) wake=${'$'}(wake)" >> "${'$'}log"
            ;;
          final)
            echo "after uid=${'$'}(id -u) at ${'$'}(date +%H:%M:%S) wake=${'$'}(wake)" >> "${'$'}log"
            /data/local/tmp/aiaio/probe/probe.sh >> "${'$'}log" 2>&1
            echo "done uid=${'$'}(id -u) at ${'$'}(date +%H:%M:%S)" >> "${'$'}log"
            ;;
        esac
    """.trimIndent()
}
