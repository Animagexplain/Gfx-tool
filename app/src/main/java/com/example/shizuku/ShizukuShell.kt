package com.example.shizuku

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Utility for constructing and executing elevated shell commands via Shizuku.
 * Supports builder pattern via newRequest().
 */
object ShizukuShell {
    private const val TAG = "ShizukuShell"

    /**
     * Creates a new shell command request.
     * Example: ShizukuShell.newRequest("am kill-all").exec()
     */
    fun newRequest(command: String): Request {
        return Request(command)
    }

    class Request(val command: String) {
        private var environment: Array<String>? = null
        private var workingDir: String? = null

        fun environment(env: Array<String>) = apply { this.environment = env }
        fun directory(dir: String) = apply { this.workingDir = dir }

        suspend fun exec(): ShellResult = withContext(Dispatchers.IO) {
            // When in test mode or Shizuku service is not responding, fallback to local simulation
            if (ShizukuManager.isTestMode() || !ShizukuManager.isShizukuRunning()) {
                return@withContext ShizukuManager.executeLocalFallback(command)
            }

            try {
                val process = invokeShizukuProcess(
                    arrayOf("sh", "-c", command),
                    environment,
                    workingDir
                )
                val stdout = process.inputStream.bufferedReader().use { it.readText() }
                val stderr = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                ShellResult(exitCode, stdout.trim(), stderr.trim())
            } catch (e: Throwable) {
                Log.e(TAG, "Failed executing '$command' via Shizuku: ${e.message}", e)
                ShellResult(-1, "", e.localizedMessage ?: "Process execution failed")
            }
        }

        private fun invokeShizukuProcess(
            cmd: Array<String>,
            env: Array<String>?,
            dir: String?
        ): Process {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            return method.invoke(null, cmd, env, dir) as Process
        }
    }
}
