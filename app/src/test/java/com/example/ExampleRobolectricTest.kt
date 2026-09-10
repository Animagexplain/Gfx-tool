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
  }

  @Test
  fun `verify preset configurations`() {
    val potato = PresetType.POTATO.settings
    assertEquals(Resolution.RES_360P, potato.resolution)
    assertEquals(GraphicsQuality.SMOOTH, potato.graphicsQuality)

    val smooth = PresetType.SMOOTH.settings
    assertEquals(FpsCap.FPS_90, smooth.fpsCap)

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
}

