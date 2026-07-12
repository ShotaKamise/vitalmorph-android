package app.vitalmorph.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.FoodEntry
import app.vitalmorph.domain.MealSlot
import app.vitalmorph.domain.WorkoutTag
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class HealthConnectRepository(private val context: Context) {
    companion object {
        val permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            HealthPermission.getWritePermission(NutritionRecord::class),
        )
    }

    val sdkStatus: Int
        get() = HealthConnectClient.getSdkStatus(context)

    private val client: HealthConnectClient?
        get() = if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(): Boolean {
        val granted = client?.permissionController?.getGrantedPermissions() ?: return false
        return granted.containsAll(permissions)
    }

    suspend fun readDays(
        start: LocalDate,
        endInclusive: LocalDate,
        workoutTags: Map<LocalDate, WorkoutTag>,
    ): List<DailyHealthData> {
        val healthClient = client ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val days = mutableListOf<DailyHealthData>()
        var date = start
        while (!date.isAfter(endInclusive)) {
            val startInstant = date.atStartOfDay(zone).toInstant()
            val endInstant = date.plusDays(1).atStartOfDay(zone).toInstant()
            val filter = TimeRangeFilter.between(startInstant, endInstant)
            val aggregate = healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    ),
                    timeRangeFilter = filter,
                ),
            )
            val exercise = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = filter,
                ),
            ).records
            // 栄養は集計APIではなくレコード単位で読み、VitaMorph自身が書き込んだ分を除外する。
            // こうしないと、書き込んだ記録を読み戻して二重計上してしまう(DATA_MODEL.mdの規約)。
            val externalNutrition = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = filter,
                ),
            ).records.filter { it.metadata.dataOrigin.packageName != context.packageName }
            val exerciseMinutes = exercise.sumOf { Duration.between(it.startTime, it.endTime).toMinutes().coerceAtLeast(0) }
            val steps = aggregate[StepsRecord.COUNT_TOTAL] ?: 0L
            val activeCalories = aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
            val calories = externalNutrition.sumOf { it.energy?.inKilocalories ?: 0.0 }
            val protein = externalNutrition.sumOf { it.protein?.inGrams ?: 0.0 }
            val carbs = externalNutrition.sumOf { it.totalCarbohydrate?.inGrams ?: 0.0 }
            val fat = externalNutrition.sumOf { it.totalFat?.inGrams ?: 0.0 }
            days += DailyHealthData(
                date = date,
                calories = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                steps = steps,
                activeCalories = activeCalories,
                exerciseMinutes = exerciseMinutes,
                workoutTag = workoutTags[date] ?: WorkoutTag.NONE,
                hasNutrition = calories > 0 || protein > 0 || carbs > 0 || fat > 0,
                hasActivity = steps > 0 || activeCalories > 0 || exerciseMinutes > 0,
            )
            date = date.plusDays(1)
        }
        return days
    }

    /**
     * VitaMorphの食事記録をNutritionRecordとして書き込む。
     * Client Record IDで対応付け、Data OriginはVitaMorph自身になるため、
     * [readDays] の読み取りでは除外されて二重計上しない。
     * 権限が無い・端末が非対応の場合はfalseを返す(記録自体はアプリ内に残る)。
     */
    suspend fun writeNutrition(entry: FoodEntry): Boolean {
        val healthClient = client ?: return false
        val zone = ZoneId.systemDefault()
        val startTime = entry.date.atTime(mealTimeFor(entry.mealSlot)).atZone(zone)
        val endTime = startTime.plusMinutes(30)
        return runCatching {
            healthClient.insertRecords(
                listOf(
                    NutritionRecord(
                        startTime = startTime.toInstant(),
                        startZoneOffset = startTime.offset,
                        endTime = endTime.toInstant(),
                        endZoneOffset = endTime.offset,
                        energy = Energy.kilocalories(entry.calories),
                        protein = Mass.grams(entry.proteinGrams),
                        totalFat = Mass.grams(entry.fatGrams),
                        totalCarbohydrate = Mass.grams(entry.carbsGrams),
                        name = entry.foodName,
                        mealType = when (entry.mealSlot) {
                            MealSlot.BREAKFAST -> MealType.MEAL_TYPE_BREAKFAST
                            MealSlot.LUNCH -> MealType.MEAL_TYPE_LUNCH
                            MealSlot.DINNER -> MealType.MEAL_TYPE_DINNER
                            MealSlot.SNACK -> MealType.MEAL_TYPE_SNACK
                        },
                        metadata = Metadata.manualEntry(clientRecordId = entry.clientRecordId),
                    ),
                ),
            )
            true
        }.getOrDefault(false)
    }

    /** VitaMorphが書き込んだNutritionRecordをClient Record IDで削除する。 */
    suspend fun deleteNutrition(clientRecordId: String): Boolean {
        val healthClient = client ?: return false
        return runCatching {
            healthClient.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(clientRecordId),
            )
            true
        }.getOrDefault(false)
    }

    private fun mealTimeFor(slot: MealSlot): LocalTime = when (slot) {
        MealSlot.BREAKFAST -> LocalTime.of(8, 0)
        MealSlot.LUNCH -> LocalTime.of(12, 30)
        MealSlot.DINNER -> LocalTime.of(19, 0)
        MealSlot.SNACK -> LocalTime.of(15, 30)
    }
}
