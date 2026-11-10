package com.tecsup.autosmart

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class NotificacionesFcmInstrumentedTest {
    @Test
    fun declaraElPermisoParaMostrarNotificaciones() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val flags = PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        val info = context.packageManager.getPackageInfo(context.packageName, flags)

        assertTrue(info.requestedPermissions.orEmpty().contains(Manifest.permission.POST_NOTIFICATIONS))
    }

    @Test
    fun obtieneUnTokenRealConFirebaseConfigurado() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verificarFcmReal") == "true")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.tecsup.autosmart", context.packageName)
        assertTrue("Firebase debe inicializarse con google-services.json", FirebaseApp.getApps(context).isNotEmpty())

        val token = Tasks.await(FirebaseMessaging.getInstance().token, 30, TimeUnit.SECONDS)
        assertTrue("Firebase debe devolver un token", token.isNotBlank())
    }
}
