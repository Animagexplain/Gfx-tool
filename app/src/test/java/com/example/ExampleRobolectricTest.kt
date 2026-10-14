package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FpsCap
import com.example.data.model.GamePackage
import com.example.data.model.GameSettings
import com.example.data.model.GraphicsQuality
import com.example.data.model.PresetType
import com.example.data.model.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GFX Pro Booster", appName)
  }

  @Test
  fun `verify game packages paths`() {
    val pubgGlobal = GamePackage.PUBG_GLOBAL
    assertEquals("com.tencent.ig", pubgGlobal.packageName)
    assertTrue(pubgGlobal.configFilePath.contains("com.tencent.ig"))
    assertTrue(pubgGlobal.configFilePath.endsWith("GameUserSettings.ini"))

    val bgmi = GamePackage.BGMI
    assertEquals("com.pubg.imobile", bgmi.packageName)
    assertTrue(bgmi.configFilePath.contains("com.pubg.imobile"))

    val pubgFlash = GamePackage.PUBG_FLASH
    assertEquals("com.tencent.igfit", pubgFlash.packageName)
    assertEquals("Flash", pubgFlash.badge)
    assertTrue(pubgFlash.configFilePath.contains("com.tencent.igfit"))
    assertTrue(pubgFlash.configFilePath.endsWith("GameUserSettings.ini"))
  }

  @Test
  fun `verify preset configurations`() {
    val potato = PresetType.POTATO.settings
    assertEquals(Resolution.RES_360P, potato.resolution)
    assertEquals(GraphicsQuality.SMOOTH, potato.graphicsQuality)

    val smooth = PresetType.SMOOTH.settings
    assertEquals(FpsCap.EXTREME_PLUS, smooth.fpsCap)

    val hd = PresetType.HD.settings
    assertEquals(Resolution.RES_1080P, hd.resolution)
  }

  @Test
  fun `verify ShizukuManager singleton and ShizukuShell newRequest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager1 = com.example.shizuku.ShizukuManager(context)
    val manager2 = com.example.shizuku.ShizukuManager.getInstance()
    
    // Check singleton identity
    assertEquals(manager1, manager2)
    assertEquals(com.example.shizuku.ShizukuManager, manager1)

    // Test mode and verification methods
    manager1.enableTestMode(true)
    assertTrue(manager1.isTestMode())
    assertTrue(manager1.isShizukuRunning())
    assertTrue(manager1.isAuthorized())

    // Verify ShizukuShell.newRequest() creation
    val request = com.example.shizuku.ShizukuShell.newRequest("am kill-all")
    assertNotNull(request)
    assertEquals("am kill-all", request.command)
  }

  @Test
  fun `verify AppSnackbarVisuals structure and feedback types`() {
    val successVisuals = com.example.ui.feedback.AppSnackbarVisuals(
      message = "Graphics settings applied successfully!",
      type = com.example.ui.feedback.FeedbackType.SUCCESS,
      customTitle = "SETTINGS APPLIED",
      actionLabel = "RUN GAME"
    )

    assertEquals("Graphics settings applied successfully!", successVisuals.message)
    assertEquals(com.example.ui.feedback.FeedbackType.SUCCESS, successVisuals.type)
    assertEquals("SETTINGS APPLIED", successVisuals.customTitle)
    assertEquals("RUN GAME", successVisuals.actionLabel)

    val shizukuErrorVisuals = com.example.ui.feedback.AppSnackbarVisuals(
      message = "Shizuku service is not running or responding.",
      type = com.example.ui.feedback.FeedbackType.SHIZUKU_ERROR,
      customTitle = "SHIZUKU IPC ERROR",
      actionLabel = "TEST MODE"
    )

    assertEquals(com.example.ui.feedback.FeedbackType.SHIZUKU_ERROR, shizukuErrorVisuals.type)
    assertEquals("SHIZUKU IPC ERROR", shizukuErrorVisuals.customTitle)
    assertEquals("TEST MODE", shizukuErrorVisuals.actionLabel)
    assertEquals(androidx.compose.material3.SnackbarDuration.Long, shizukuErrorVisuals.duration)
  }

  @Test
  fun `verify GfxViewModel snackbar feedback for applied settings and Shizuku errors`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.GfxViewModel(app)

    // Trigger Success snackbar as when settings are applied
    viewModel.showSuccessSnackbar(
      message = "Graphics settings applied successfully for PUBG Mobile (Global)!",
      actionLabel = "RUN Global"
    )

    val snackbar = viewModel.snackbarVisuals.value
    assertNotNull("Snackbar visuals should be posted upon applying settings", snackbar)
    assertEquals(com.example.ui.feedback.FeedbackType.SUCCESS, snackbar?.type)
    assertTrue(snackbar?.message?.contains("applied successfully") == true)
    assertEquals("RUN Global", snackbar?.actionLabel)

    // Trigger Shizuku error feedback
    viewModel.showShizukuErrorSnackbar(
      message = "Shizuku authorization missing",
      actionLabel = "GRANT"
    )

    val errorSnackbar = viewModel.snackbarVisuals.value
    assertNotNull(errorSnackbar)
    assertEquals(com.example.ui.feedback.FeedbackType.SHIZUKU_ERROR, errorSnackbar?.type)
    assertEquals("GRANT", errorSnackbar?.actionLabel)
    assertEquals("SHIZUKU IPC ERROR", errorSnackbar?.customTitle)

    viewModel.clearSnackbarVisuals()
    assertEquals(null, viewModel.snackbarVisuals.value)
  }
}

