package app.vitalmorph.domain

import java.time.LocalDate

enum class WorkoutTag {
    NONE,
    CARDIO,
    STRENGTH,
    OTHER,
}

data class DailyHealthData(
    val date: LocalDate,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val steps: Long = 0,
    val activeCalories: Double = 0.0,
    val exerciseMinutes: Long = 0,
    val workoutTag: WorkoutTag = WorkoutTag.NONE,
    val hasNutrition: Boolean = false,
    val hasActivity: Boolean = false,
)

data class UserGoals(
    val calories: Int = 2_000,
    val proteinGrams: Int = 80,
    val carbsGrams: Int = 250,
    val fatGrams: Int = 60,
    val dailySteps: Int = 8_000,
    val weeklyExerciseMinutes: Int = 150,
)

enum class MonsterStage(val label: String) {
    BABY("幼生"),
    FAMILY("成長体"),
    INTERMEDIATE("成熟体"),
    FINAL("最終形態"),
}

enum class MonsterFamily(val label: String) {
    BALANCE("調和系"),
    POWER("筋力系"),
    SPEED("俊足系"),
    STORAGE("蓄積系"),
    REST("静養系"),
    OVERDRIVE("過活動系"),
}

data class MonsterForm(
    val id: String,
    val name: String,
    val stage: MonsterStage,
    val family: MonsterFamily,
    val role: String,
    val description: String,
    val accent: Long,
)

data class EvolutionMetrics(
    val nutritionScore: Int = 0,
    val activityScore: Int = 0,
    val consistencyScore: Int = 0,
    val calorieRatio: Double = 0.0,
    val proteinRatio: Double = 0.0,
    val carbsRatio: Double = 0.0,
    val fatRatio: Double = 0.0,
    val stepGoalDays: Int = 0,
    val strengthDays: Int = 0,
    val cardioDays: Int = 0,
    val highActivityDays: Int = 0,
    val restDays: Int = 0,
    val activityTrend: Double = 0.0,
    val calorieTrend: Double = 0.0,
    val weekdayWeekendVariance: Double = 0.0,
    val recordedDays: Int = 0,
)

data class EvolutionResult(
    val seasonDay: Int,
    val form: MonsterForm,
    val path: List<MonsterForm>,
    val finalCandidates: List<MonsterForm>,
    val metrics: EvolutionMetrics,
    val nextEvolutionDay: Int?,
)

data class TrainerProgress(
    val xp: Int = 0,
    val completedSeasons: Int = 0,
)

data class TrainerRank(
    val name: String,
    val minimumXp: Int,
    val minimumSeasons: Int,
    val nextRankXp: Int?,
)

data class BattleMatch(
    val round: String,
    val opponent: String,
    val playerScore: Int,
    val opponentScore: Int,
    val won: Boolean,
)

data class TournamentResult(
    val placement: Int,
    val tournamentPoints: Int,
    val matches: List<BattleMatch>,
)
