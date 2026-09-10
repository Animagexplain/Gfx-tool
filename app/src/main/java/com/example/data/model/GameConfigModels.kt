package com.example.data.model

enum class GamePackage(
    val packageName: String,
    val displayName: String,
    val badge: String
) {
    PUBG_GLOBAL(
        packageName = "com.tencent.ig",
        displayName = "PUBG Mobile (Global)",
        badge = "Global"
    ),
    BGMI(
        packageName = "com.pubg.imobile",
        displayName = "BGMI (India)",
        badge = "BGMI"
    ),
    PUBG_VN(
        packageName = "com.vng.pubgmobile",
        displayName = "PUBG Mobile (Vietnam)",
        badge = "VN"
    ),
    PUBG_KR(
        packageName = "com.rekoo.pubgm",
        displayName = "PUBG Mobile (Korea)",
        badge = "KR"
    );

    val configDirectory: String
        get() = "/storage/emulated/0/Android/data/$packageName/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android"

    val configFilePath: String
        get() = "$configDirectory/GameUserSettings.ini"
}

enum class Resolution(
    val label: String,
    val width: Int,
    val height: Int,
    val screenScale: Int
) {
    RES_360P("360p", 960, 540, 60),
    RES_480P("480p", 1152, 648, 75),
    RES_720P("720p", 1280, 720, 100),
    RES_900P("900p", 1600, 900, 115),
    RES_1080P("1080p", 1920, 1080, 125);

    companion object {
        fun fromDimensions(w: Int, h: Int): Resolution {
            return entries.minByOrNull { kotlin.math.abs(it.height - h) } ?: RES_720P
        }
    }
}

enum class GraphicsQuality(val label: String, val iniValue: Int) {
    SMOOTH("Smooth", 0),
    BALANCED("Balanced", 1),
    HD("HD", 2),
    HDR("HDR", 3),
    ULTRA_HD("Ultra HD", 4);

    companion object {
        fun fromValue(value: Int): GraphicsQuality {
            return entries.firstOrNull { it.iniValue == value } ?: SMOOTH
        }
    }
}

enum class FpsCap(val fps: Int, val label: String, val frameRateLimit: Float) {
    FPS_20(20, "20 FPS", 20f),
    FPS_30(30, "30 FPS", 30f),
    FPS_40(40, "40 FPS", 40f),
    FPS_60(60, "60 FPS (Ultra)", 60f),
    FPS_90(90, "90 FPS (Extreme)", 90f);

    companion object {
        fun fromLimit(limit: Float): FpsCap {
            val rounded = limit.toInt()
            return entries.minByOrNull { kotlin.math.abs(it.fps - rounded) } ?: FPS_60
        }
    }
}

enum class ShadowQuality(val label: String, val iniValue: Int) {
    DISABLED("Disabled", 0),
    LOW("Low", 1),
    HIGH("High", 2);

    companion object {
        fun fromValue(value: Int): ShadowQuality {
            return entries.firstOrNull { it.iniValue == value } ?: DISABLED
        }
    }
}

enum class TextureQuality(val label: String, val iniValue: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2);

    companion object {
        fun fromValue(value: Int): TextureQuality {
            return entries.firstOrNull { it.iniValue == value } ?: LOW
        }
    }
}

enum class EffectsQuality(val label: String, val iniValue: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2);

    companion object {
        fun fromValue(value: Int): EffectsQuality {
            return entries.firstOrNull { it.iniValue == value } ?: LOW
        }
    }
}

enum class GraphicsApi(val label: String, val iniValue: String) {
    VULKAN("Vulkan", "Vulkan"),
    OPENGL_ES("OpenGL ES", "OpenGLES");

    companion object {
        fun fromValue(value: String): GraphicsApi {
            return if (value.equals("OpenGLES", ignoreCase = true) || value.equals("OpenGL", ignoreCase = true)) {
                OPENGL_ES
            } else {
                VULKAN
            }
        }
    }
}

data class GameSettings(
    val resolution: Resolution = Resolution.RES_720P,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.SMOOTH,
    val fpsCap: FpsCap = FpsCap.FPS_60,
    val antiAliasing: Boolean = false,
    val shadowQuality: ShadowQuality = ShadowQuality.DISABLED,
    val textureQuality: TextureQuality = TextureQuality.LOW,
    val effectsQuality: EffectsQuality = EffectsQuality.LOW,
    val graphicsApi: GraphicsApi = GraphicsApi.VULKAN,
    val gpuOptimization: Boolean = true
)

enum class PresetType(
    val title: String,
    val tag: String,
    val description: String,
    val idealFor: String,
    val settings: GameSettings
) {
    POTATO(
        title = "Potato Mode",
        tag = "Max FPS",
        description = "Stripped-down graphics & 360p rendering to squeeze maximum playable frames from older/weaker hardware.",
        idealFor = "Weak SoC / 2GB - 4GB RAM",
        settings = GameSettings(
            resolution = Resolution.RES_360P,
            graphicsQuality = GraphicsQuality.SMOOTH,
            fpsCap = FpsCap.FPS_60,
            antiAliasing = false,
            shadowQuality = ShadowQuality.DISABLED,
            textureQuality = TextureQuality.LOW,
            effectsQuality = EffectsQuality.LOW,
            graphicsApi = GraphicsApi.OPENGL_ES,
            gpuOptimization = true
        )
    ),
    SMOOTH(
        title = "Smooth Mode",
        tag = "Pro Gaming",
        description = "Minimal rendering latency, 90 FPS cap, shadows disabled for peak competitive situational awareness.",
        idealFor = "Mid-range gaming / 90Hz+ screens",
        settings = GameSettings(
            resolution = Resolution.RES_720P,
            graphicsQuality = GraphicsQuality.SMOOTH,
            fpsCap = FpsCap.FPS_90,
            antiAliasing = false,
            shadowQuality = ShadowQuality.DISABLED,
            textureQuality = TextureQuality.LOW,
            effectsQuality = EffectsQuality.LOW,
            graphicsApi = GraphicsApi.VULKAN,
            gpuOptimization = true
        )
    ),
    BALANCED(
        title = "Balanced Mode",
        tag = "Sweet Spot",
        description = "Balanced visual fidelity and solid 60 FPS with moderate anti-aliasing and ambient effects enabled.",
        idealFor = "Stable gameplay / 4GB - 6GB RAM",
        settings = GameSettings(
            resolution = Resolution.RES_720P,
            graphicsQuality = GraphicsQuality.BALANCED,
            fpsCap = FpsCap.FPS_60,
            antiAliasing = true,
            shadowQuality = ShadowQuality.LOW,
            textureQuality = TextureQuality.MEDIUM,
            effectsQuality = EffectsQuality.MEDIUM,
            graphicsApi = GraphicsApi.VULKAN,
            gpuOptimization = true
        )
    ),
    HD(
        title = "HD Mode",
        tag = "High Fidelity",
        description = "Crisp 1080p rendering, HD textures, dynamic shadows, and anti-aliasing for flagship devices.",
        idealFor = "Flagship Snapdragon / 8GB+ RAM",
        settings = GameSettings(
            resolution = Resolution.RES_1080P,
            graphicsQuality = GraphicsQuality.HD,
            fpsCap = FpsCap.FPS_60,
            antiAliasing = true,
            shadowQuality = ShadowQuality.HIGH,
            textureQuality = TextureQuality.HIGH,
            effectsQuality = EffectsQuality.HIGH,
            graphicsApi = GraphicsApi.VULKAN,
            gpuOptimization = true
        )
    )
}

data class BackupItem(
    val id: String,
    val fileName: String,
    val formattedDate: String,
    val timestampMillis: Long,
    val sizeBytes: Long,
    val fullPath: String,
    val isGameDir: Boolean
)

data class DeviceInfo(
    val totalRamMb: Long,
    val availRamMb: Long,
    val cpuModel: String,
    val androidRelease: String,
    val apiLevel: Int,
    val deviceModel: String,
    val manufacturer: String
) {
    val totalRamGb: Float
        get() = totalRamMb / 1024f

    val availRamGb: Float
        get() = availRamMb / 1024f

    val usedRamPercent: Int
        get() = if (totalRamMb > 0) (((totalRamMb - availRamMb) * 100) / totalRamMb).toInt() else 0
}
