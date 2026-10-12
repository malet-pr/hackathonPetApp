package com.example.petcare

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.petcare.data.SettingsStore
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {
    @Test fun titleReloadsFromDiskAfterStoreCloses() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "title-test.preferences_pb")
        file.delete()
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val first = SettingsStore(PreferenceDataStoreFactory.create(scope = firstScope) { file })
        assertEquals("Pet Care", first.title.first())
        first.saveTitle("  Fido and Friends  ")
        first.saveAppearance("Fido and Friends", "/private/welcome.image")
        firstScope.coroutineContext[Job]!!.cancelAndJoin()
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened = SettingsStore(PreferenceDataStoreFactory.create(scope = secondScope) { file })
            assertEquals("Fido and Friends", reopened.title.first())
            assertEquals("/private/welcome.image", reopened.welcomeImagePath.first())
            reopened.saveTitle("New title")
            assertEquals("/private/welcome.image", reopened.welcomeImagePath.first())
        } finally {
            secondScope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
