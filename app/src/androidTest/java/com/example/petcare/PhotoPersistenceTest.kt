package com.example.petcare

import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.petcare.data.*
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-10-12T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    @Test fun privatePhotosAndOptionalDetailsSurviveReopenAndFailedReplacementKeepsOriginal() = runBlocking {
        val photos = PhotoStore(context)
        val source = File(context.cacheDir, "picker-${UUID.randomUUID()}.png")
        Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.GREEN)
            source.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val dbName = "photo-${UUID.randomUUID()}.db"
        var db = PetCareDatabase.open(context, dbName)
        val preferenceFile = File(context.cacheDir, "photo-${UUID.randomUUID()}.preferences_pb")
        val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsStore(PreferenceDataStoreFactory.create(scope = settingsScope) { preferenceFile })
        var imported: String? = null
        try {
            imported = photos.import(Uri.fromFile(source))
            assertTrue(File(imported).isFile)
            assertTrue(File(imported).canonicalPath.startsWith(context.filesDir.canonicalPath))
            val thumbnail = photos.thumbnail(imported, 480)
            assertNotNull(thumbnail)
            assertTrue(maxOf(thumbnail!!.width, thumbnail.height) <= 480)
            thumbnail.recycle()

            val repo = TreatmentRepository(db, clock)
            val pet = repo.addPet("Fido", LocalDate.of(2020, 5, 1), "Nuria", "Mixed breed", imported)
            repo.saveFirstTreatment(pet.id, LocalDate.of(2026, 10, 11), "A", "One dose", "Clinic", null, imported)
            settings.saveAppearance("Fido's care", imported)
            db.close()
            db = PetCareDatabase.open(context, dbName)
            assertEquals("2020-05-01", db.careDao().pet(pet.id)?.birthDate)
            assertEquals("Nuria", db.careDao().pet(pet.id)?.owner)
            assertEquals("Mixed breed", db.careDao().pet(pet.id)?.breed)
            assertEquals(imported, db.careDao().pet(pet.id)?.photoPath)
            assertEquals(imported, db.careDao().history(pet.id).single().photoPath)
            assertEquals(imported, settings.welcomeImagePath.first())

            // The attempted replacement cannot be committed: the original remains readable and referenced.
            val bad = File(context.cacheDir, "bad-${UUID.randomUUID()}.image")
            bad.writeText("not an image")
            try { photos.import(Uri.fromFile(bad)); fail("Invalid image accepted") }
            catch (_: java.io.IOException) { }
            finally { bad.delete() }
            assertNotNull(photos.thumbnail(imported))
            assertEquals(imported, db.careDao().pet(pet.id)?.photoPath)
            assertEquals(imported, settings.welcomeImagePath.first())

            val demoCopy = photos.import(Uri.fromFile(source), demo = true)
            assertTrue(photos.isDemoPath(demoCopy))
            assertNotNull(photos.thumbnail(demoCopy))
            photos.clearDemo()
            assertFalse(File(demoCopy).exists())
            assertTrue(File(imported).exists())

            val reopenedRepository = TreatmentRepository(db, clock)
            val optionalPet = reopenedRepository.addPet("Luna")
            assertNull(db.careDao().pet(optionalPet.id)?.birthDate)
            try { reopenedRepository.updatePet(optionalPet.id, "Luna", LocalDate.of(2026, 10, 13), "", "", null)
                fail("Future birth date accepted") }
            catch (_: IllegalArgumentException) { }
            assertNull(db.careDao().pet(optionalPet.id)?.birthDate)
        } finally {
            db.close(); context.deleteDatabase(dbName)
            settingsScope.coroutineContext[Job]!!.cancelAndJoin()
            preferenceFile.delete(); source.delete(); photos.discard(imported)
        }
    }
}
