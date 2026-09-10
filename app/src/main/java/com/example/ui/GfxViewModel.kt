package com.example.ui

import android.app.Application
import android.content.Context
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
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
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

    private val _ramCleanResult = MutableStateFlow<String?>(null)
    val ramCleanResult: StateFlow<String?> = _ramCleanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        detectInstalledGames()
        refreshDeviceInfo()
        loadConfigForGame(_selectedGame.value)
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

    fun applySettings() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.applySettings(_selectedGame.value, _currentSettings.value)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = result.getOrNull()
                refreshBackups(_selectedGame.value)
            } else {
                _statusMessage.value = "Failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun restoreLatestBackup() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.restoreLatestBackup(_selectedGame.value)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = result.getOrNull()
                loadConfigForGame(_selectedGame.value)
            } else {
                _statusMessage.value = "Restore failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun restoreBackup(backup: BackupItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.restoreSpecificBackup(_selectedGame.value, backup)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = result.getOrNull()
                loadConfigForGame(_selectedGame.value)
            } else {
                _statusMessage.value = "Failed to restore: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteBackup(backup: BackupItem) {
        viewModelScope.launch {
            val deleted = configManager.deleteBackup(backup)
            if (deleted) {
                _statusMessage.value = "Backup deleted: ${backup.fileName}"
                refreshBackups(_selectedGame.value)
            } else {
                _statusMessage.value = "Failed to delete backup"
            }
        }
    }

    fun restoreDefaultConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = configManager.restoreDefaultConfig(_selectedGame.value)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = result.getOrNull()
                loadConfigForGame(_selectedGame.value)
            } else {
                _statusMessage.value = "Failed: ${result.exceptionOrNull()?.localizedMessage}"
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
                _ramCleanResult.value = result.getOrNull()
                _statusMessage.value = result.getOrNull()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "RAM clean failed"
                _ramCleanResult.value = err
                _statusMessage.value = err
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

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearRamCleanResult() {
        _ramCleanResult.value = null
    }
}
