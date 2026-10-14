package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File

enum class ShizukuStatus {
    CHECKING,
    RUNNING_AND_GRANTED,
    RUNNING_NEEDS_PERMISSION,
    SERVICE_STOPPED,
    NOT_INSTALLED,
    TEST_MODE
}

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean
        get() = exitCode == 0
}

/**
 * Singleton manager for Shizuku connection state, permission lifecycle,
 * elevated execution, and UE4 configuration file operations.
 */
object ShizukuManager {

    private const val TAG = "ShizukuManager"
    const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    const val REQUEST_CODE_PERMISSION = 7001

    private var appContext: Context? = null

    private val _status = MutableStateFlow(ShizukuStatus.CHECKING)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var isSimulated: Boolean = false

    private val _communicationErrorEvents = MutableSharedFlow<ShizukuCommunicationException>(extraBufferCapacity = 8)
    val communicationErrorEvents: SharedFlow<ShizukuCommunicationException> = _communicationErrorEvents.asSharedFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder dead")
        refreshStatus()
        _communicationErrorEvents.tryEmit(
            ShizukuNotRunningException("Shizuku binder died unexpectedly. Service connection lost.")
        )
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_PERMISSION) {
                Log.d(TAG, "Shizuku permission result: $grantResult")
                refreshStatus()
            }
        }

    init {
        registerShizukuListeners()
        refreshStatus()
    }

    /**
     * Operator invoke allows calling ShizukuManager(context) like a constructor
     * while retaining the singleton pattern across the entire application.
     */
    operator fun invoke(context: Context? = null): ShizukuManager {
        if (context != null && appContext == null) {
            initialize(context)
        }
        return this
    }

    /**
     * Explicit singleton accessor.
     */
    fun getInstance(context: Context? = null): ShizukuManager = invoke(context)

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            refreshStatus()
        }
    }

    private fun registerShizukuListeners() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register Shizuku listeners", e)
        }
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to remove Shizuku listeners", e)
        }
    }

    // ==========================================
    // Helper Methods to Verify Shizuku State
    // ==========================================

    /**
     * Checks if the Shizuku server service is actively running and binder is responding.
     */
    fun isShizukuRunning(): Boolean {
        if (isSimulated || _status.value == ShizukuStatus.TEST_MODE) return true
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Checks if Shizuku authorization has been granted to this application.
     */
    fun isAuthorized(): Boolean {
        if (isSimulated || _status.value == ShizukuStatus.TEST_MODE) return true
        return try {
            if (isShizukuRunning()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Alias for isAuthorized()
     */
    fun isPermissionGranted(): Boolean = isAuthorized()

    /**
     * Returns raw permission check code (PERMISSION_GRANTED or PERMISSION_DENIED)
     */
    fun checkPermission(): Int {
        if (isSimulated || _status.value == ShizukuStatus.TEST_MODE) return PackageManager.PERMISSION_GRANTED
        return try {
            if (isShizukuRunning()) {
                Shizuku.checkSelfPermission()
            } else {
                PackageManager.PERMISSION_DENIED
            }
        } catch (e: Throwable) {
            PackageManager.PERMISSION_DENIED
        }
    }

    /**
     * Checks if the Shizuku Manager application is installed on device.
     */
    fun isShizukuInstalled(): Boolean {
        val ctx = appContext ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.packageManager.getPackageInfo(
                    SHIZUKU_PACKAGE_NAME,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Gets running Shizuku server version or -1 if not running.
     */
    fun getShizukuVersion(): Int {
        return try {
            if (isShizukuRunning()) Shizuku.getVersion() else -1
        } catch (e: Throwable) {
            -1
        }
    }

    fun isTestMode(): Boolean = isSimulated || _status.value == ShizukuStatus.TEST_MODE

    /**
     * Refreshes the connection state flow based on active binder and permissions.
     */
    fun refreshStatus() {
        if (isSimulated) {
            _status.value = ShizukuStatus.TEST_MODE
            return
        }

        try {
            val running = isShizukuRunning()
            if (!running) {
                _status.value = if (isShizukuInstalled()) {
                    ShizukuStatus.SERVICE_STOPPED
                } else {
                    ShizukuStatus.NOT_INSTALLED
                }
                return
            }

            if (Shizuku.getVersion() < 11) {
                // Pre-v11 Shizuku not supported
                _status.value = ShizukuStatus.SERVICE_STOPPED
                return
            }

            val permission = Shizuku.checkSelfPermission()
            _status.value = if (permission == PackageManager.PERMISSION_GRANTED) {
                ShizukuStatus.RUNNING_AND_GRANTED
            } else {
                ShizukuStatus.RUNNING_NEEDS_PERMISSION
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error checking Shizuku status", e)
            _status.value = if (isShizukuInstalled()) {
                ShizukuStatus.SERVICE_STOPPED
            } else {
                ShizukuStatus.NOT_INSTALLED
            }
        }
    }

    /**
     * Requests Shizuku authorization dialog.
     */
    fun requestPermission(requestCode: Int = REQUEST_CODE_PERMISSION) {
        if (isSimulated) {
            _status.value = ShizukuStatus.TEST_MODE
            return
        }
        try {
            if (isShizukuRunning()) {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    Log.i(TAG, "Showing Shizuku request rationale")
                }
                Shizuku.requestPermission(requestCode)
            } else {
                Log.w(TAG, "Cannot request permission: Shizuku service is not running")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to request Shizuku permission", e)
        }
    }

    fun enableTestMode(enabled: Boolean) {
        isSimulated = enabled
        if (enabled) {
            _status.value = ShizukuStatus.TEST_MODE
        } else {
            refreshStatus()
        }
    }

    // ==========================================
    // Elevated Shell Execution via ShizukuShell
    // ==========================================

    /**
     * Executes an elevated shell command (e.g. 'am kill-all') using ShizukuShell.newRequest().
     */
    suspend fun executeShellCommand(command: String = "am kill-all"): ShellResult {
        return ShizukuShell.newRequest(command).exec()
    }

    /**
     * Executes shell command using ShizukuShell.newRequest().
     */
    suspend fun executeShell(command: String): ShellResult {
        return ShizukuShell.newRequest(command).exec()
    }

    suspend fun killBackgroundProcesses(): Result<String> = withContext(Dispatchers.IO) {
        if (isTestMode()) {
            return@withContext Result.success("Simulated RAM Clean: Background processes trimmed. Freed ~350 MB RAM.")
        }

        val result = executeShellCommand("am kill-all")
        if (result.isSuccess) {
            Result.success("RAM optimized via elevated shell (am kill-all). Background tasks terminated.")
        } else {
            Result.failure(Exception(result.stderr.ifBlank { "Command returned code ${result.exitCode}" }))
        }
    }

    // ==========================================
    // File IO Helpers for UE4 Config Files
    // ==========================================

    suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) {
        if (isTestMode()) {
            val simFile = getSimulatedFile(path)
            return@withContext if (simFile.exists()) {
                Result.success(simFile.readText())
            } else {
                Result.failure(Exception("File not found in test mode: $path"))
            }
        }

        if (!isShizukuRunning()) {
            val ex = ShizukuNotRunningException("Cannot read configuration: Shizuku service is not running.")
            _communicationErrorEvents.tryEmit(ex)
            return@withContext Result.failure(ex)
        }
        if (!isAuthorized()) {
            val ex = ShizukuPermissionDeniedException("Cannot read configuration: Shizuku authorization is missing.")
            _communicationErrorEvents.tryEmit(ex)
            return@withContext Result.failure(ex)
        }

        val result = executeShell("cat \"$path\"")
        if (result.isSuccess) {
            Result.success(result.stdout)
        } else {
            val ex = ShizukuFileAccessException(path, result.stderr.ifBlank { "Exit code ${result.exitCode}" })
            Result.failure(ex)
        }
    }

    suspend fun writeFile(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (isTestMode()) {
            val simFile = getSimulatedFile(path)
            simFile.parentFile?.mkdirs()
            simFile.writeText(content)
            return@withContext Result.success(Unit)
        }

        if (!isShizukuRunning()) {
            val ex = ShizukuNotRunningException("Cannot write settings: Shizuku server service is stopped or unreachable.")
            _communicationErrorEvents.tryEmit(ex)
            return@withContext Result.failure(ex)
        }
        if (!isAuthorized()) {
            val ex = ShizukuPermissionDeniedException("Cannot write settings: Shizuku permission has not been granted.")
            _communicationErrorEvents.tryEmit(ex)
            return@withContext Result.failure(ex)
        }

        try {
            val dir = File(path).parent ?: ""
            if (dir.isNotBlank()) {
                executeShell("mkdir -p \"$dir\"")
            }

            val process = invokeShizukuNewProcess(arrayOf("sh", "-c", "cat > \"$path\""), null, null)
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(content)
                writer.flush()
            }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Result.success(Unit)
            } else {
                val err = process.errorStream.bufferedReader().use { it.readText() }
                val ex = ShizukuFileAccessException(path, "Failed to write config via Shizuku: $err (exit $exitCode)")
                _communicationErrorEvents.tryEmit(ex)
                Result.failure(ex)
            }
        } catch (e: Throwable) {
            val ex = ShizukuFileAccessException(path, e.localizedMessage ?: "Unknown Shizuku IPC error", e)
            _communicationErrorEvents.tryEmit(ex)
            Result.failure(ex)
        }
    }

    suspend fun copyFile(sourcePath: String, destPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (isTestMode()) {
            val src = getSimulatedFile(sourcePath)
            val dst = getSimulatedFile(destPath)
            dst.parentFile?.mkdirs()
            if (src.exists()) {
                src.copyTo(dst, overwrite = true)
                return@withContext Result.success(Unit)
            } else {
                return@withContext Result.failure(Exception("Source file does not exist"))
            }
        }

        if (!isShizukuRunning()) {
            val ex = ShizukuNotRunningException("Cannot backup configuration: Shizuku service is not running.")
            _communicationErrorEvents.tryEmit(ex)
            return@withContext Result.failure(ex)
        }
        if (!isAuthorized()) {
            val ex = ShizukuPermissionDeniedException("Cannot backup configuration: Shizuku permission not granted.")
            _communicationErrorEvents.tryEmit(ex)
            return@withContext Result.failure(ex)
        }

        val destDir = File(destPath).parent ?: ""
        if (destDir.isNotBlank()) {
            executeShell("mkdir -p \"$destDir\"")
        }
        val result = executeShell("cp \"$sourcePath\" \"$destPath\"")
        if (result.isSuccess) {
            Result.success(Unit)
        } else {
            val ex = ShizukuFileAccessException(destPath, "Shizuku cp failed: ${result.stderr}")
            _communicationErrorEvents.tryEmit(ex)
            Result.failure(ex)
        }
    }

    suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        if (isTestMode()) {
            return@withContext getSimulatedFile(path).exists()
        }

        val result = executeShell("[ -f \"$path\" ] && echo '1' || echo '0'")
        result.stdout.trim() == "1"
    }

    suspend fun listBackupsInDirectory(dirPath: String): List<String> = withContext(Dispatchers.IO) {
        if (isTestMode()) {
            val simDir = getSimulatedFile(dirPath)
            return@withContext simDir.listFiles { _, name -> name.endsWith(".bak") || name.contains("backup") }
                ?.map { it.name } ?: emptyList()
        }

        val result = executeShell("ls -1 \"$dirPath\" 2>/dev/null")
        if (result.isSuccess) {
            result.stdout.lines().map { it.trim() }.filter { it.endsWith(".bak") || it.contains("backup") }
        } else {
            emptyList()
        }
    }

    private fun invokeShizukuNewProcess(cmd: Array<String>, env: Array<String>?, dir: String?): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(null, cmd, env, dir) as Process
    }

    fun getSimulatedFile(targetPath: String): File {
        val sanitized = targetPath.replace("/storage/emulated/0/", "").replace("/", "_")
        val ctx = appContext
        val baseDir = if (ctx != null) {
            File(ctx.filesDir, "simulated_storage")
        } else {
            File("/tmp/simulated_storage")
        }
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return File(baseDir, sanitized)
    }

    fun executeLocalFallback(command: String): ShellResult {
        return ShellResult(0, "Command executed in preview/test mode", "")
    }
}
