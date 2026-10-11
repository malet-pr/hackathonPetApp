package com.example.petcare.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val birthDate: String? = null,
    val owner: String? = null,
    val breed: String? = null,
    val photoPath: String? = null,
)

@Entity(tableName = "administrations", foreignKeys = [
    ForeignKey(entity = Pet::class, parentColumns = ["id"], childColumns = ["petId"]),
    ForeignKey(entity = TreatmentPlan::class, parentColumns = ["id"], childColumns = ["completedPlanId"]),
], indices = [Index("petId"), Index(value = ["completedPlanId"], unique = true)])
data class Administration(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val actualDate: String,
    val medicationName: String,
    val doseText: String,
    val purchaseSource: String? = null,
    val photoPath: String? = null,
    val recordedAt: Long,
    val completedPlanId: String? = null,
)

@Entity(tableName = "treatment_plans", foreignKeys = [
    ForeignKey(entity = Pet::class, parentColumns = ["id"], childColumns = ["petId"]),
    ForeignKey(entity = Administration::class, parentColumns = ["id"], childColumns = ["sourceAdministrationId"]),
], indices = [Index("petId"), Index("sourceAdministrationId")])
data class TreatmentPlan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val petId: String,
    val sourceAdministrationId: String,
    val dueDate: String,
    val status: String = "PENDING",
    val revision: Int = 0,
    val postponedOnce: Boolean = false,
    val handledBits: Int = 0,
    val needsReconciliation: Boolean = true,
)

data class PetSummary(val pet: Pet, val last: Administration?, val pending: TreatmentPlan?)

@Dao
interface PetCareDao {
    @Query("SELECT * FROM pets ORDER BY name COLLATE NOCASE, id") fun observePets(): Flow<List<Pet>>
    @Query("SELECT * FROM administrations ORDER BY actualDate DESC, recordedAt DESC, id DESC")
    fun observeAdministrations(): Flow<List<Administration>>
    @Query("SELECT * FROM treatment_plans") fun observePlans(): Flow<List<TreatmentPlan>>
    @Query("SELECT * FROM pets WHERE id = :id") suspend fun pet(id: String): Pet?
    @Query("SELECT * FROM administrations WHERE petId = :petId ORDER BY actualDate DESC, recordedAt DESC, id DESC")
    suspend fun history(petId: String): List<Administration>
    @Query("SELECT * FROM treatment_plans WHERE id = :id") suspend fun plan(id: String): TreatmentPlan?
    @Query("SELECT * FROM treatment_plans WHERE petId = :petId AND status = 'PENDING'")
    suspend fun pending(petId: String): TreatmentPlan?
    @Query("SELECT * FROM treatment_plans") suspend fun allPlans(): List<TreatmentPlan>
    @Insert suspend fun insertPet(pet: Pet)
    @Insert suspend fun insertAdministration(administration: Administration)
    @Insert suspend fun insertPlan(plan: TreatmentPlan)
    @Update suspend fun updatePlan(plan: TreatmentPlan)
}

@Database(entities = [Pet::class, Administration::class, TreatmentPlan::class], version = 1, exportSchema = true)
abstract class PetCareDatabase : RoomDatabase() {
    abstract fun careDao(): PetCareDao
    companion object {
        fun open(context: Context, name: String = "pet-care.db"): PetCareDatabase =
            Room.databaseBuilder(context.applicationContext, PetCareDatabase::class.java, name).build()
    }
}
