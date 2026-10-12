package com.example.petcare.demo

import android.content.Context
import androidx.room.Room
import com.example.petcare.data.PetCareDatabase
import com.example.petcare.data.TreatmentRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class DemoSession private constructor(
    val id: String,
    val clock: AcceleratedClock,
    val database: PetCareDatabase,
) {
    val repository = TreatmentRepository(database, clock)
    val events = DemoEventPump(this)
    var lastChecked = clock.instant()

    companion object {
        suspend fun create(context: Context): DemoSession {
            val zone = ZoneId.systemDefault()
            val purchaseDay = LocalDate.now(zone)
            val clock = AcceleratedClock(zone, purchaseDay.atTime(11, 0).atZone(zone).toInstant())
            val database = Room.inMemoryDatabaseBuilder(context, PetCareDatabase::class.java).build()
            try {
                val session = DemoSession(UUID.randomUUID().toString(), clock, database)
                val pet = session.repository.addPet("Demo Fido")
                session.repository.saveFirstTreatment(pet.id, purchaseDay.minusDays(30),
                    "Medication A", "One sample dose", "Sample pet shop", purchaseDay.plusDays(4))
                session.lastChecked = clock.instant()
                return session
            } catch (error: Exception) {
                database.close()
                throw error
            }
        }
    }
}
