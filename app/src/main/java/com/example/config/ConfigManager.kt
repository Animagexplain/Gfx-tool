package com.example.config

import android.content.Context
import android.util.Log
import com.example.data.model.BackupItem
import com.example.data.model.EffectsQuality
import com.example.data.model.FpsCap
import com.example.data.model.GamePackage
import com.example.data.model.GameSettings
import com.example.data.model.GraphicsApi
import com.example.data.model.GraphicsQuality
import com.example.data.model.Resolution
import com.example.data.model.ShadowQuality
import com.example.data.model.TextureQuality
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConfigManager(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {
    companion object {
        private const val TAG = "ConfigManager"
        private const val DEFAULT_SECTION = "[/Script/ShadowTrackerExtra.ShadowTrackerExtraGameUserSettings]"

        // Common Keys
        private const val KEY_RES_X = "UserSelectedResolutionSizeX"
        private const val KEY_RES_Y = "UserSelectedResolutionSizeY"
        private const val KEY_RESOLUTION_SIZE_X = "ResolutionSizeX"
        private const val KEY_RESOLUTION_SIZE_Y = "ResolutionSizeY"
        private const val KEY_DESIRED_SCREEN_WIDTH = "DesiredScreenWidth"
        private const val KEY_DESIRED_SCREEN_HEIGHT = "DesiredScreenHeight"
        private const val KEY_SCREEN_SCALE = "ScreenScale"
        private const val KEY_GRAPHICS_QUALITY = "GraphicsQuality"
        private const val KEY_FRAME_RATE_LIMIT = "FrameRateLimit"
        private const val KEY_ANTI_ALIASING = "AntiAliasing"
        private const val KEY_SHADOW_QUALITY = "ShadowQuality"
        private const val KEY_TEXTURE_QUALITY = "TextureQuality"
        private const val KEY_EFFECTS_QUALITY = "EffectsQuality"
        private const val KEY_GRAPHICS_API = "GraphicsAPI"
        private const val KEY_GPU_OPTIMIZATION = "bEnableGPUOptimization"
        private const val KEY_SHOW_FPS = "bShowFPS"
        private const val KEY_DETAIL_MODE = "DetailMode"
    }

    private val localBackupDir: File
        get() {
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun loadSettings(game: GamePackage): Result<GameSettings> = withContext(Dispatchers.IO) {
        val configPath = game.configFilePath
        val readResult = shizukuManager.readFile(configPath)

        if (readResult.isFailure) {
            Log.w(TAG, "Config not found at $configPath, using default settings")
            return@withContext Result.success(GameSettings())
        }

        val content = readResult.getOrNull().orEmpty()
        try {
            val settings = parseIniContent(content)
            Result.success(settings)
        } catch (e: Throwable) {
            Log.e(TAG, "Error parsing INI content", e)
            Result.failure(e)
        }
    }

    suspend fun applySettings(
        game: GamePackage,
        newSettings: GameSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        // Step 1: Input Validation
        val validationError = validateSettings(newSettings)
        if (validationError != null) {
            return@withContext Result.failure(IllegalArgumentException(validationError))
        }

        val configPath = game.configFilePath
        val configDir = game.configDirectory
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        // Step 2: Read existing config if available, and create backup
        val existingContent = shizukuManager.readFile(configPath).getOrNull()

        if (!existingContent.isNullOrBlank()) {
            // Automatic backup: 1) GameUserSettings.ini.bak in game dir
            val bakPath = "$configDir/GameUserSettings.ini.bak"
            shizukuManager.copyFile(configPath, bakPath)

            // 2) Timestamped backup in game dir
            val timestampedBak = "$configDir/GameUserSettings_$timestamp.ini.bak"
            shizukuManager.copyFile(configPath, timestampedBak)

            // 3) Internal persistent app backup
            saveLocalBackup(game, existingContent, timestamp)
        } else {
            // Seed a local initial backup template
            val defaultTemplate = generateDefaultIni(GameSettings())
            saveLocalBackup(game, defaultTemplate, "${timestamp}_initial")
        }

        // Step 3: Merge settings into existing INI lines or create fresh INI
        val updatedContent = mergeSettingsIntoIni(existingContent, newSettings)

        // Step 4: Write to file via elevated Shizuku
        val writeResult = shizukuManager.writeFile(configPath, updatedContent)
        if (writeResult.isFailure) {
            return@withContext Result.failure(writeResult.exceptionOrNull() ?: Exception("Write failed"))
        }

        Result.success("Settings applied successfully for ${game.displayName}! Backup created: GameUserSettings_$timestamp.ini.bak")
    }

    suspend fun restoreLatestBackup(game: GamePackage): Result<String> = withContext(Dispatchers.IO) {
        val configPath = game.configFilePath
        val bakPath = "${game.configDirectory}/GameUserSettings.ini.bak"

        // Check if game dir .bak exists
        if (shizukuManager.fileExists(bakPath)) {
            val restoreResult = shizukuManager.copyFile(bakPath, configPath)
            if (restoreResult.isSuccess) {
                return@withContext Result.success("Restored from GameUserSettings.ini.bak")
            }
        }

        // Fallback: check internal backups
        val internalBackups = getLocalBackups(game)
        if (internalBackups.isNotEmpty()) {
            val latest = internalBackups.first()
            val file = File(latest.fullPath)
            if (file.exists()) {
                val content = file.readText()
                val writeResult = shizukuManager.writeFile(configPath, content)
                if (writeResult.isSuccess) {
                    return@withContext Result.success("Restored from backup: ${latest.fileName}")
                }
            }
        }

        Result.failure(Exception("No backup found to restore for ${game.displayName}"))
    }

    suspend fun restoreSpecificBackup(game: GamePackage, backup: BackupItem): Result<String> = withContext(Dispatchers.IO) {
        val configPath = game.configFilePath
        if (backup.isGameDir) {
            val result = shizukuManager.copyFile(backup.fullPath, configPath)
            return@withContext if (result.isSuccess) {
                Result.success("Successfully restored ${backup.fileName}")
            } else {
                Result.failure(Exception("Failed to restore ${backup.fileName}"))
            }
        } else {
            val file = File(backup.fullPath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("Backup file no longer exists"))
            }
            val content = file.readText()
            val writeResult = shizukuManager.writeFile(configPath, content)
            return@withContext if (writeResult.isSuccess) {
                Result.success("Successfully restored ${backup.fileName}")
            } else {
                Result.failure(Exception("Failed to write restored content"))
            }
        }
    }

    suspend fun restoreDefaultConfig(game: GamePackage): Result<String> = withContext(Dispatchers.IO) {
        val defaultContent = generateDefaultIni(GameSettings())
        val writeResult = shizukuManager.writeFile(game.configFilePath, defaultContent)
        return@withContext if (writeResult.isSuccess) {
            Result.success("Default configuration restored.")
        } else {
            Result.failure(Exception("Failed to restore default configuration."))
        }
    }

    suspend fun getAllBackups(game: GamePackage): List<BackupItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<BackupItem>()

        // 1. App internal backups
        val internalList = getLocalBackups(game)
        list.addAll(internalList)

        // 2. Game directory backups via Shizuku
        try {
            val gameDirFiles = shizukuManager.listBackupsInDirectory(game.configDirectory)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            gameDirFiles.forEach { name ->
                val fullPath = "${game.configDirectory}/$name"
                list.add(
                    BackupItem(
                        id = "game_$name",
                        fileName = name,
                        formattedDate = dateFormat.format(Date()),
                        timestampMillis = System.currentTimeMillis(),
                        sizeBytes = 2048L,
                        fullPath = fullPath,
                        isGameDir = true
                    )
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not list game directory backups", e)
        }

        list.distinctBy { it.fileName }.sortedByDescending { it.timestampMillis }
    }

    suspend fun deleteBackup(backup: BackupItem): Boolean = withContext(Dispatchers.IO) {
        if (!backup.isGameDir) {
            val file = File(backup.fullPath)
            return@withContext file.delete()
        } else {
            val result = shizukuManager.executeShell("rm -f \"${backup.fullPath}\"")
            return@withContext result.isSuccess
        }
    }

    private fun validateSettings(settings: GameSettings): String? {
        if (settings.resolution.width !in 640..2560 || settings.resolution.height !in 360..1600) {
            return "Invalid resolution parameters: ${settings.resolution.width}x${settings.resolution.height}"
        }
        if (settings.graphicsQuality.iniValue !in 0..4) {
            return "Invalid graphics quality level: ${settings.graphicsQuality.iniValue}"
        }
        if (settings.fpsCap.fps !in 20..90) {
            return "Invalid FPS cap: ${settings.fpsCap.fps}"
        }
        if (settings.shadowQuality.iniValue !in 0..2) {
            return "Invalid shadow quality value"
        }
        if (settings.textureQuality.iniValue !in 0..2) {
            return "Invalid texture quality value"
        }
        if (settings.effectsQuality.iniValue !in 0..2) {
            return "Invalid effects quality value"
        }
        return null
    }

    private fun parseIniContent(content: String): GameSettings {
        val map = mutableMapOf<String, String>()
        content.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && !line.startsWith(";") && !line.startsWith("#") && !line.startsWith("[")) {
                val split = line.split("=", limit = 2)
                if (split.size == 2) {
                    map[split[0].trim()] = split[1].trim()
                }
            }
        }

        val resX = map[KEY_RES_X]?.toIntOrNull()
            ?: map[KEY_RESOLUTION_SIZE_X]?.toIntOrNull()
            ?: map[KEY_DESIRED_SCREEN_WIDTH]?.toIntOrNull() ?: 1280
        val resY = map[KEY_RES_Y]?.toIntOrNull()
            ?: map[KEY_RESOLUTION_SIZE_Y]?.toIntOrNull()
            ?: map[KEY_DESIRED_SCREEN_HEIGHT]?.toIntOrNull() ?: 720
        val resolution = Resolution.fromDimensions(resX, resY)

        val graphicsQuality = GraphicsQuality.fromValue(map[KEY_GRAPHICS_QUALITY]?.toIntOrNull() ?: 0)

        val fpsLimit = map[KEY_FRAME_RATE_LIMIT]?.toFloatOrNull() ?: 60f
        val fpsCap = FpsCap.fromLimit(fpsLimit)

        val antiAliasing = (map[KEY_ANTI_ALIASING]?.toIntOrNull() ?: 0) > 0
        val shadowQuality = ShadowQuality.fromValue(map[KEY_SHADOW_QUALITY]?.toIntOrNull() ?: 0)
        val textureQuality = TextureQuality.fromValue(map[KEY_TEXTURE_QUALITY]?.toIntOrNull() ?: 0)
        val effectsQuality = EffectsQuality.fromValue(map[KEY_EFFECTS_QUALITY]?.toIntOrNull() ?: 0)
        val graphicsApi = GraphicsApi.fromValue(map[KEY_GRAPHICS_API] ?: "Vulkan")
        val gpuOpt = map[KEY_GPU_OPTIMIZATION]?.equals("True", ignoreCase = true) ?: true

        return GameSettings(
            resolution = resolution,
            graphicsQuality = graphicsQuality,
            fpsCap = fpsCap,
            antiAliasing = antiAliasing,
            shadowQuality = shadowQuality,
            textureQuality = textureQuality,
            effectsQuality = effectsQuality,
            graphicsApi = graphicsApi,
            gpuOptimization = gpuOpt
        )
    }

    private fun mergeSettingsIntoIni(existingContent: String?, settings: GameSettings): String {
        val keyValueMap = mutableMapOf(
            KEY_RES_X to settings.resolution.width.toString(),
            KEY_RES_Y to settings.resolution.height.toString(),
            KEY_RESOLUTION_SIZE_X to settings.resolution.width.toString(),
            KEY_RESOLUTION_SIZE_Y to settings.resolution.height.toString(),
            KEY_DESIRED_SCREEN_WIDTH to settings.resolution.width.toString(),
            KEY_DESIRED_SCREEN_HEIGHT to settings.resolution.height.toString(),
            KEY_SCREEN_SCALE to settings.resolution.screenScale.toString(),
            KEY_GRAPHICS_QUALITY to settings.graphicsQuality.iniValue.toString(),
            KEY_FRAME_RATE_LIMIT to String.format(Locale.US, "%.6f", settings.fpsCap.frameRateLimit),
            KEY_ANTI_ALIASING to (if (settings.antiAliasing) "1" else "0"),
            KEY_SHADOW_QUALITY to settings.shadowQuality.iniValue.toString(),
            KEY_TEXTURE_QUALITY to settings.textureQuality.iniValue.toString(),
            KEY_EFFECTS_QUALITY to settings.effectsQuality.iniValue.toString(),
            KEY_GRAPHICS_API to settings.graphicsApi.iniValue,
            KEY_GPU_OPTIMIZATION to (if (settings.gpuOptimization) "True" else "False"),
            KEY_SHOW_FPS to "True",
            KEY_DETAIL_MODE to (if (settings.graphicsQuality == GraphicsQuality.SMOOTH) "0" else "1")
        )

        if (existingContent.isNullOrBlank()) {
            return generateDefaultIni(settings)
        }

        val updatedLines = mutableListOf<String>()
        val foundKeys = mutableSetOf<String>()
        var inTargetSection = false
        var hasAnySection = false

        existingContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                hasAnySection = true
                if (inTargetSection) {
                    // Append any remaining keys before moving to next section
                    keyValueMap.forEach { (k, v) ->
                        if (!foundKeys.contains(k)) {
                            updatedLines.add("$k=$v")
                            foundKeys.add(k)
                        }
                    }
                }
                inTargetSection = trimmed.contains("ShadowTrackerExtra", ignoreCase = true)
                updatedLines.add(line)
            } else if (trimmed.contains("=") && !trimmed.startsWith(";") && !trimmed.startsWith("#")) {
                val key = trimmed.substringBefore("=").trim()
                if (keyValueMap.containsKey(key)) {
                    val newValue = keyValueMap[key]
                    updatedLines.add("$key=$newValue")
                    foundKeys.add(key)
                } else {
                    updatedLines.add(line)
                }
            } else {
                updatedLines.add(line)
            }
        }

        // If target section was active or no sections existed, append remaining keys
        keyValueMap.forEach { (k, v) ->
            if (!foundKeys.contains(k)) {
                if (!hasAnySection) {
                    updatedLines.add(0, DEFAULT_SECTION)
                    hasAnySection = true
                }
                updatedLines.add("$k=$v")
                foundKeys.add(k)
            }
        }

        return updatedLines.joinToString("\n")
    }

    private fun generateDefaultIni(settings: GameSettings): String {
        val sb = StringBuilder()
        sb.appendLine(DEFAULT_SECTION)
        sb.appendLine("$KEY_RES_X=${settings.resolution.width}")
        sb.appendLine("$KEY_RES_Y=${settings.resolution.height}")
        sb.appendLine("$KEY_RESOLUTION_SIZE_X=${settings.resolution.width}")
        sb.appendLine("$KEY_RESOLUTION_SIZE_Y=${settings.resolution.height}")
        sb.appendLine("$KEY_DESIRED_SCREEN_WIDTH=${settings.resolution.width}")
        sb.appendLine("$KEY_DESIRED_SCREEN_HEIGHT=${settings.resolution.height}")
        sb.appendLine("$KEY_SCREEN_SCALE=${settings.resolution.screenScale}")
        sb.appendLine("$KEY_GRAPHICS_QUALITY=${settings.graphicsQuality.iniValue}")
        sb.appendLine("$KEY_FRAME_RATE_LIMIT=${String.format(Locale.US, "%.6f", settings.fpsCap.frameRateLimit)}")
        sb.appendLine("$KEY_ANTI_ALIASING=${if (settings.antiAliasing) "1" else "0"}")
        sb.appendLine("$KEY_SHADOW_QUALITY=${settings.shadowQuality.iniValue}")
        sb.appendLine("$KEY_TEXTURE_QUALITY=${settings.textureQuality.iniValue}")
        sb.appendLine("$KEY_EFFECTS_QUALITY=${settings.effectsQuality.iniValue}")
        sb.appendLine("$KEY_GRAPHICS_API=${settings.graphicsApi.iniValue}")
        sb.appendLine("$KEY_GPU_OPTIMIZATION=${if (settings.gpuOptimization) "True" else "False"}")
        sb.appendLine("$KEY_SHOW_FPS=True")
        sb.appendLine("$KEY_DETAIL_MODE=${if (settings.graphicsQuality == GraphicsQuality.SMOOTH) "0" else "1"}")
        return sb.toString()
    }

    private fun saveLocalBackup(game: GamePackage, content: String, timestamp: String) {
        try {
            val gameDir = File(localBackupDir, game.packageName)
            if (!gameDir.exists()) gameDir.mkdirs()
            val backupFile = File(gameDir, "GameUserSettings_$timestamp.ini.bak")
            backupFile.writeText(content)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to save local internal backup", e)
        }
    }

    private fun getLocalBackups(game: GamePackage): List<BackupItem> {
        val gameDir = File(localBackupDir, game.packageName)
        if (!gameDir.exists()) return emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        return gameDir.listFiles { _, name -> name.endsWith(".bak") }
            ?.map { file ->
                BackupItem(
                    id = "local_${file.name}",
                    fileName = file.name,
                    formattedDate = dateFormat.format(Date(file.lastModified())),
                    timestampMillis = file.lastModified(),
                    sizeBytes = file.length(),
                    fullPath = file.absolutePath,
                    isGameDir = false
                )
            } ?: emptyList()
    }
}
