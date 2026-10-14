package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.ConfigManager
import com.example.data.model.BackupItem
import com.example.data.model.DeviceInfo
import com.example.data.model.EffectsQuality
import com.example.data.model.FpsCap
import com.example.data.model.GamePackage
import com.example.data.model.GameSettings
import com.example.data.model.GraphicsApi
import com.example.data.model.GraphicsQuality
import com.example.data.model.PresetType
import com.example.data.model.Resolution
import com.example.data.model.ShadowQuality
import com.example.data.model.TextureQuality
import com.example.shizuku.ShizukuCommunicationException
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuNotRunningException
import com.example.shizuku.ShizukuPermissionDeniedException
import com.example.shizuku.ShizukuStatus
import com.example.ui.feedback.AppSnackbarVisuals
import com.example.ui.feedback.FeedbackType
import com.example.util.DeviceProfiler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GfxViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("gfx_pro_booster_prefs", Context.MODE_PRIVATE)

    val shizukuManager = ShizukuManager(application)
    val configManager = ConfigManager(application, shizukuManager)

    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuManager.status

    private val _isDisclaimerAccepted = MutableStateFlow(
        prefs.getBoolean("disclaimer_accepted", false)
    )
    val isDisclaimerAccepted: StateFlow<Boolean> = _isDisclaimerAccepted.asStateFlow()

    private val _installedGames = MutableStateFlow<List<GamePackage>>(emptyList())
    val installedGames: StateFlow<List<GamePackage>> = _installedGames.asStateFlow()

    private val _selectedGame = MutableStateFlow(GamePackage.PUBG_GLOBAL)
    val selectedGame: StateFlow<GamePackage> = _selectedGame.asStateFlow()

    private val _currentSettings = MutableStateFlow(GameSettings())
    val currentSettings: StateFlow<GameSettings> = _currentSettings.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupItem>>(emptyList())
    val backups: StateFlow<List<BackupItem>> = _backups.asStateFlow()

    private val _deviceInfo = MutableStateFlow(DeviceProfiler.getDeviceInfo(application))
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _recommendedPreset = MutableStateFlow(PresetType.SMOOTH)
    val recommendedPreset: StateFlow<PresetType> = _recommendedPreset.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _snackbarVisuals = MutableStateFlow<AppSnackbarVisuals?>(null)
    val snackbarVisuals: StateFlow<AppSnackbarVisuals?> = _snackbarVisuals.asStateFlow()

    private val _ramCleanResult = MutableStateFlow<String?>(null)
    val ramCleanResult: StateFlow<String?> = _ramCleanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        detectInstalledGames()
        refreshDeviceInfo()
        loadConfigForGame(_selectedGame.value)

        // Listen for asynchronous Shizuku IPC communication errors
        viewModelScope.launch {
            shizukuManager.communicationErrorEvents.collect { error ->
                handleShizukuCommunicationError(error)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        shizukuManager.cleanup()
    }

    fun acceptDisclaimer() {
        prefs.edit().putBoolean("disclaimer_accepted", true).apply()
        _isDisclaimerAccepted.value = true
    }

    fun detectInstalledGames() {
        val detected = mutableListOf<GamePackage>()
        val pm = getApplication<Application>().packageManager
        GamePackage.entries.forEach { game ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(game.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(game.packageName, 0)
                }
                detected.add(game)
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed
            } catch (_: Throwable) {
                // Ignore package visibility security exceptions
            }
        }
        _installedGames.value = detected
        if (detected.isNotEmpty() && !detected.contains(_selectedGame.value)) {
            selectGame(detected.first())
        }
    }

    fun selectGame(game: GamePackage) {
        _selectedGame.value = game
        loadConfigForGame(game)
        refreshBackups(game)
    }

    fun loadConfigForGame(game: GamePackage) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.loadSettings(game)
            if (result.isSuccess) {
                _currentSettings.value = result.getOrNull() ?: GameSettings()
            }
            refreshBackups(game)
            _isLoading.value = false
        }
    }

    fun refreshBackups(game: GamePackage = _selectedGame.value) {
        viewModelScope.launch {
            _backups.value = configManager.getAllBackups(game)
        }
    }

    fun refreshDeviceInfo() {
        val info = DeviceProfiler.getDeviceInfo(getApplication())
        _deviceInfo.value = info
        _recommendedPreset.value = DeviceProfiler.recommendPreset(info)
    }

    // Settings update functions
    fun updateResolution(resolution: Resolution) {
        _currentSettings.value = _currentSettings.value.copy(resolution = resolution)
    }

    fun updateGraphicsQuality(quality: GraphicsQuality) {
        _currentSettings.value = _currentSettings.value.copy(graphicsQuality = quality)
    }

    fun updateFpsCap(fpsCap: FpsCap) {
        _currentSettings.value = _currentSettings.value.copy(fpsCap = fpsCap)
    }

    fun updateAntiAliasing(enabled: Boolean) {
        _currentSettings.value = _currentSettings.value.copy(antiAliasing = enabled)
    }

    fun updateShadowQuality(quality: ShadowQuality) {
        _currentSettings.value = _currentSettings.value.copy(shadowQuality = quality)
    }

    fun updateTextureQuality(quality: TextureQuality) {
        _currentSettings.value = _currentSettings.value.copy(textureQuality = quality)
    }

    fun updateEffectsQuality(quality: EffectsQuality) {
        _currentSettings.value = _currentSettings.value.copy(effectsQuality = quality)
    }

    fun updateGraphicsApi(api: GraphicsApi) {
        _currentSettings.value = _currentSettings.value.copy(graphicsApi = api)
    }

    fun updateGpuOptimization(enabled: Boolean) {
        _currentSettings.value = _currentSettings.value.copy(gpuOptimization = enabled)
    }

    fun applyPreset(preset: PresetType) {
        _currentSettings.value = preset.settings
        applySettings()
    }

    fun applySettings(andLaunch: Boolean = false) {
        viewModelScope.launch {
            val targetGame = _selectedGame.value
            val targetSettings = _currentSettings.value

            // 1. Proactively verify Shizuku communication status if not in simulated/test mode
            if (!shizukuManager.isTestMode()) {
                if (!shizukuManager.isShizukuRunning()) {
                    showShizukuErrorSnackbar(
                        message = "Shizuku service is not running or responding.",
                        detailSubtitle = "Service status: ${shizukuStatus.value.name}. Start Shizuku app or enable Test Mode.",
                        actionLabel = "TEST MODE"
                    ) {
                        enableTestMode(true)
                        applySettings(andLaunch)
                    }
                    return@launch
                }

                if (!shizukuManager.isAuthorized()) {
                    showShizukuErrorSnackbar(
                        message = "Shizuku permission not granted for Android/data access.",
                        detailSubtitle = "GFX Pro Booster requires elevated authorization from Shizuku.",
                        actionLabel = "GRANT"
                    ) {
                        shizukuManager.requestPermission()
                    }
                    return@launch
                }
            }

            _isLoading.value = true
            val result = configManager.applySettings(targetGame, targetSettings)
            _isLoading.value = false

            if (result.isSuccess) {
                val detail = "${targetSettings.resolution.label} • ${targetSettings.graphicsQuality.label} • ${targetSettings.fpsCap.label}"
                refreshBackups(targetGame)
                showSuccessSnackbar(
                    message = "Graphics settings applied successfully for ${targetGame.displayName}!",
                    customTitle = "SETTINGS APPLIED",
                    detailSubtitle = "$detail | Auto-backup created",
                    actionLabel = "RUN ${targetGame.badge}"
                ) {
                    launchGame(targetGame)
                }
                if (andLaunch) {
                    launchGame(targetGame)
                }
            } else {
                val ex = result.exceptionOrNull()
                if (ex is ShizukuCommunicationException ||
                    ex?.message?.contains("Shizuku", ignoreCase = true) == true ||
                    ex?.message?.contains("Permission", ignoreCase = true) == true ||
                    ex?.message?.contains("Binder", ignoreCase = true) == true
                ) {
                    showShizukuErrorSnackbar(
                        message = "Shizuku communication error: ${ex?.localizedMessage ?: "IPC write failed"}",
                        detailSubtitle = "Check Shizuku status or switch to Test Mode",
                        actionLabel = if (!shizukuManager.isAuthorized() && !shizukuManager.isTestMode()) "GRANT" else "RETRY"
                    ) {
                        if (!shizukuManager.isAuthorized() && !shizukuManager.isTestMode()) {
                            shizukuManager.requestPermission()
                        } else {
                            applySettings(andLaunch)
                        }
                    }
                } else {
                    showErrorSnackbar(
                        message = "Failed to apply settings: ${ex?.localizedMessage ?: "Unknown error"}",
                        actionLabel = "RETRY"
                    ) {
                        applySettings(andLaunch)
                    }
                }
            }
        }
    }

    private fun handleShizukuCommunicationError(error: ShizukuCommunicationException) {
        when (error) {
            is ShizukuNotRunningException -> {
                showShizukuErrorSnackbar(
                    message = error.localizedMessage ?: "Shizuku service is not running or responding.",
                    detailSubtitle = "Start Shizuku via wireless debugging/root or use Test Mode.",
                    actionLabel = "TEST MODE"
                ) {
                    enableTestMode(true)
                }
            }
            is ShizukuPermissionDeniedException -> {
                showShizukuErrorSnackbar(
                    message = error.localizedMessage ?: "Shizuku authorization has not been granted.",
                    detailSubtitle = "Authorize GFX Pro Booster in the Shizuku app to write game files.",
                    actionLabel = "GRANT"
                ) {
                    shizukuManager.requestPermission()
                }
            }
            else -> {
                showShizukuErrorSnackbar(
                    message = error.localizedMessage ?: "Error communicating with Shizuku.",
                    detailSubtitle = "Elevated operation failed.",
                    actionLabel = "RETRY"
                ) {
                    refreshShizukuStatus()
                }
            }
        }
    }

    fun showSuccessSnackbar(
        message: String,
        customTitle: String? = "SETTINGS APPLIED",
        detailSubtitle: String? = null,
        actionLabel: String? = "RUN GAME",
        onAction: (() -> Unit)? = null
    ) {
        _statusMessage.value = message
        _snackbarVisuals.value = AppSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            type = FeedbackType.SUCCESS,
            customTitle = customTitle,
            detailSubtitle = detailSubtitle,
            onAction = onAction
        )
    }

    fun showShizukuErrorSnackbar(
        message: String,
        detailSubtitle: String? = null,
        actionLabel: String? = "RETRY",
        onAction: (() -> Unit)? = null
    ) {
        _statusMessage.value = "Shizuku error: $message"
        _snackbarVisuals.value = AppSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            type = FeedbackType.SHIZUKU_ERROR,
            customTitle = "SHIZUKU IPC ERROR",
            detailSubtitle = detailSubtitle,
            onAction = onAction
        )
    }

    fun showErrorSnackbar(
        message: String,
        actionLabel: String? = "DISMISS",
        onAction: (() -> Unit)? = null
    ) {
        _statusMessage.value = message
        _snackbarVisuals.value = AppSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            type = FeedbackType.GENERIC_ERROR,
            onAction = onAction
        )
    }

    fun showInfoSnackbar(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _statusMessage.value = message
        _snackbarVisuals.value = AppSnackbarVisuals(
            message = message,
            actionLabel = actionLabel,
            type = FeedbackType.INFO,
            onAction = onAction
        )
    }

    fun clearSnackbarVisuals() {
        _snackbarVisuals.value = null
    }

    fun restoreLatestBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.restoreLatestBackup(_selectedGame.value)
            _isLoading.value = false
            if (result.isSuccess) {
                loadConfigForGame(_selectedGame.value)
                showSuccessSnackbar(
                    message = "Configuration restored from backup for ${_selectedGame.value.displayName}!",
                    customTitle = "BACKUP RESTORED",
                    actionLabel = "RUN ${_selectedGame.value.badge}"
                ) {
                    launchGame(_selectedGame.value)
                }
            } else {
                val ex = result.exceptionOrNull()
                if (ex is ShizukuCommunicationException) {
                    showShizukuErrorSnackbar("Shizuku error restoring backup: ${ex.localizedMessage}")
                } else {
                    showErrorSnackbar("Restore failed: ${ex?.localizedMessage}")
                }
            }
        }
    }

    fun restoreBackup(backup: BackupItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.restoreSpecificBackup(_selectedGame.value, backup)
            _isLoading.value = false
            if (result.isSuccess) {
                loadConfigForGame(_selectedGame.value)
                showSuccessSnackbar(
                    message = "Successfully restored backup: ${backup.fileName}",
                    customTitle = "RESTORE COMPLETE",
                    actionLabel = "RUN ${_selectedGame.value.badge}"
                ) {
                    launchGame(_selectedGame.value)
                }
            } else {
                val ex = result.exceptionOrNull()
                if (ex is ShizukuCommunicationException) {
                    showShizukuErrorSnackbar("Shizuku error restoring backup: ${ex.localizedMessage}")
                } else {
                    showErrorSnackbar("Failed to restore ${backup.fileName}: ${ex?.localizedMessage}")
                }
            }
        }
    }

    fun deleteBackup(backup: BackupItem) {
        viewModelScope.launch {
            val deleted = configManager.deleteBackup(backup)
            if (deleted) {
                showInfoSnackbar("Backup deleted: ${backup.fileName}")
                refreshBackups(_selectedGame.value)
            } else {
                showErrorSnackbar("Failed to delete backup ${backup.fileName}")
            }
        }
    }

    fun restoreDefaultConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.restoreDefaultConfig(_selectedGame.value)
            _isLoading.value = false
            if (result.isSuccess) {
                loadConfigForGame(_selectedGame.value)
                showSuccessSnackbar(
                    message = "Reset to clean default settings for ${_selectedGame.value.displayName}!",
                    customTitle = "DEFAULTS RESTORED",
                    actionLabel = "RUN ${_selectedGame.value.badge}"
                ) {
                    launchGame(_selectedGame.value)
                }
            } else {
                val ex = result.exceptionOrNull()
                if (ex is ShizukuCommunicationException) {
                    showShizukuErrorSnackbar("Shizuku communication error: ${ex.localizedMessage}")
                } else {
                    showErrorSnackbar("Failed to restore defaults: ${ex?.localizedMessage}")
                }
            }
        }
    }

    fun cleanRam() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = shizukuManager.killBackgroundProcesses()
            _isLoading.value = false
            refreshDeviceInfo()
            if (result.isSuccess) {
                val msg = result.getOrNull() ?: "RAM optimized via elevated shell"
                _ramCleanResult.value = msg
                showSuccessSnackbar(
                    message = msg,
                    customTitle = "RAM OPTIMIZED",
                    actionLabel = "DISMISS"
                )
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "RAM clean failed"
                _ramCleanResult.value = err
                showShizukuErrorSnackbar(
                    message = "Shizuku command execution failed: $err",
                    actionLabel = "RETRY"
                ) {
                    cleanRam()
                }
            }
        }
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun refreshShizukuStatus() {
        shizukuManager.refreshStatus()
    }

    fun enableTestMode(enabled: Boolean) {
        shizukuManager.enableTestMode(enabled)
    }

    fun launchGame(game: GamePackage = _selectedGame.value): Boolean {
        val app = getApplication<Application>()
        val pm = app.packageManager
        val intent = pm.getLaunchIntentForPackage(game.packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
            showInfoSnackbar("Launching ${game.displayName}...")
            true
        } else {
            showErrorSnackbar("${game.displayName} (${game.badge}) is not installed on this device")
            false
        }
    }

    fun cleanGameShaderCache(game: GamePackage = _selectedGame.value) {
        viewModelScope.launch {
            _isLoading.value = true
            val cacheDir = "/storage/emulated/0/Android/data/${game.packageName}/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Logs"
            val shaderDir = "/storage/emulated/0/Android/data/${game.packageName}/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/cache"
            val cmd = "rm -rf '$cacheDir'/* '$shaderDir'/* 2>/dev/null; echo 'OK'"
            val res = shizukuManager.executeShell(cmd)
            _isLoading.value = false
            if (res.isSuccess) {
                showSuccessSnackbar(
                    message = "Shader cache and log files cleaned for ${game.displayName}!",
                    customTitle = "CACHE PURGED",
                    actionLabel = "RUN ${game.badge}"
                ) {
                    launchGame(game)
                }
            } else {
                showShizukuErrorSnackbar(
                    message = "Shizuku failed to clear shader cache: ${res.stderr.ifBlank { "Exit code ${res.exitCode}" }}",
                    actionLabel = "RETRY"
                ) {
                    cleanGameShaderCache(game)
                }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearRamCleanResult() {
        _ramCleanResult.value = null
    }
}
