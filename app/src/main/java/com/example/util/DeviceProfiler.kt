package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.example.data.model.DeviceInfo
import com.example.data.model.PresetType

object DeviceProfiler {

    fun getDeviceInfo(context: Context): DeviceInfo {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
        } else null

        val cpu = socModel
            ?: Build.HARDWARE.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
            ?: Build.BOARD

        return DeviceInfo(
            totalRamMb = totalMb,
            availRamMb = availMb,
            cpuModel = cpu,
            androidRelease = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        )
    }

    fun recommendPreset(info: DeviceInfo): PresetType {
        return when {
            info.totalRamMb < 4000 -> PresetType.POTATO
            info.totalRamMb < 6000 -> PresetType.SMOOTH
            info.totalRamMb < 8000 -> PresetType.BALANCED
            else -> PresetType.HD
        }
    }

    fun getRecommendationExplanation(info: DeviceInfo, preset: PresetType): String {
        return when (preset) {
            PresetType.POTATO ->
                "Detected ${String.format("%.1f", info.totalRamGb)} GB RAM on ${info.cpuModel}. Potato Mode is recommended to minimize memory pressure, reduce thermal throttling, and prevent game crashes on entry-level hardware."
            PresetType.SMOOTH ->
                "Detected ${String.format("%.1f", info.totalRamGb)} GB RAM on ${info.cpuModel}. Smooth Mode is ideal for high competitive framerates (up to 90 FPS) with low input latency and clean visibility."
            PresetType.BALANCED ->
                "Detected ${String.format("%.1f", info.totalRamGb)} GB RAM. Balanced Mode delivers stable 60 FPS with optimal anti-aliasing and HD textures for mid-tier processors."
            PresetType.HD ->
                "Detected ${String.format("%.1f", info.totalRamGb)} GB RAM on high-performance ${info.cpuModel}. HD Mode provides sharp 1080p graphics, rich shadows, and flagship visual quality."
        }
    }
}
