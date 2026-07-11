package app.vitalmorph.data

import app.vitalmorph.domain.DailyHealthData
import app.vitalmorph.domain.UserGoals
import app.vitalmorph.domain.WorkoutTag
import java.time.LocalDate
import kotlin.math.roundToInt

object DemoDataFactory {
    fun create(start: LocalDate, end: LocalDate, goals: UserGoals): List<DailyHealthData> {
        val result = mutableListOf<DailyHealthData>()
        var date = start
        var index = 0
        while (!date.isAfter(end)) {
            val wave = ((index % 5) - 2) * 0.035
            val strength = index % 3 == 0
            val cardio = index % 3 == 1
            val rest = index % 7 == 6
            result += DailyHealthData(
                date = date,
                calories = goals.calories * (1.0 + wave),
                proteinGrams = goals.proteinGrams * (if (strength) 1.12 else 0.96),
                carbsGrams = goals.carbsGrams * (1.0 + wave / 2),
                fatGrams = goals.fatGrams * (0.98 - wave / 3),
                steps = if (rest) (goals.dailySteps * 0.55).roundToInt().toLong() else (goals.dailySteps * (0.92 + (index % 4) * 0.08)).roundToInt().toLong(),
                activeCalories = if (rest) 140.0 else 340.0 + index % 4 * 45,
                exerciseMinutes = when {
                    rest -> 0
                    strength -> 35
                    cardio -> 45
                    else -> 20
                },
                workoutTag = when {
                    strength -> WorkoutTag.STRENGTH
                    cardio -> WorkoutTag.CARDIO
                    else -> WorkoutTag.OTHER
                },
                hasNutrition = true,
                hasActivity = true,
            )
            date = date.plusDays(1)
            index++
        }
        return result
    }
}
