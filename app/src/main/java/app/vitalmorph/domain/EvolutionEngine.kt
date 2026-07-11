package app.vitalmorph.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

object EvolutionEngine {
    private const val GREEN = 0xFF69E6A6
    private const val BLUE = 0xFF66B7FF
    private const val RED = 0xFFFF6B6B
    private const val YELLOW = 0xFFFFD166
    private const val PURPLE = 0xFFB89CFF
    private const val ORANGE = 0xFFFF985E

    private fun form(
        id: String,
        name: String,
        stage: MonsterStage,
        family: MonsterFamily,
        role: String,
        description: String,
        accent: Long,
    ) = MonsterForm(id, name, stage, family, role, description, accent)

    val baby = form(
        "morphy", "モルフィ", MonsterStage.BABY, MonsterFamily.BALANCE,
        "可能性", "生活のエネルギーを吸収し、どの系統にも進化できる幼生。", GREEN,
    )

    private val leafang = form("leafang", "リーファング", MonsterStage.FAMILY, MonsterFamily.BALANCE, "万能", "栄養と活動が調和した若獣。", GREEN)
    private val galvol = form("galvol", "ガルヴォル", MonsterStage.FAMILY, MonsterFamily.POWER, "攻撃", "筋力のコアが発達した格闘獣。", RED)
    private val rapizel = form("rapizel", "ラピゼル", MonsterStage.FAMILY, MonsterFamily.SPEED, "速度", "歩みを電気へ変換する俊足獣。", BLUE)
    private val motchigrow = form("motchigrow", "モッチグロウ", MonsterStage.FAMILY, MonsterFamily.STORAGE, "防御", "余剰エネルギーを柔らかな外殻へ蓄える。", YELLOW)
    private val mossleep = form("mossleep", "モスリープ", MonsterStage.FAMILY, MonsterFamily.REST, "回復", "静かな生活から再生力を得る苔獣。", PURPLE)
    private val runpact = form("runpact", "ランパクト", MonsterStage.FAMILY, MonsterFamily.OVERDRIVE, "高火力", "強い活動エネルギーで常に熱を帯びる。", ORANGE)

    private val solfeon = form("solfeon", "ソルフェオン", MonsterStage.INTERMEDIATE, MonsterFamily.BALANCE, "万能", "安定した生活リズムを光へ変える。", GREEN)
    private val aquanel = form("aquanel", "アクアネル", MonsterStage.INTERMEDIATE, MonsterFamily.BALANCE, "支援", "休養と活動を滑らかに循環させる。", BLUE)
    private val grandguard = form("grandguard", "グランガルド", MonsterStage.INTERMEDIATE, MonsterFamily.POWER, "重装", "鍛錬を積み重ねた重装格闘獣。", RED)
    private val fangrage = form("fangrage", "ファングレイジ", MonsterStage.INTERMEDIATE, MonsterFamily.POWER, "瞬発", "瞬間的な力を牙へ集中させる。", ORANGE)
    private val velox = form("velox", "ヴェロクス", MonsterStage.INTERMEDIATE, MonsterFamily.SPEED, "連撃", "毎日の歩みで加速し続ける。", BLUE)
    private val skyRush = form("sky_rush", "スカイラッシュ", MonsterStage.INTERMEDIATE, MonsterFamily.SPEED, "雷撃", "有酸素運動の熱を雷へ変える。", PURPLE)
    private val bulkDome = form("bulk_dome", "バルクドーム", MonsterStage.INTERMEDIATE, MonsterFamily.STORAGE, "堅守", "蓄えたエネルギーで巨大な甲殻を作る。", YELLOW)
    private val emberPot = form("ember_pot", "エンバーポット", MonsterStage.INTERMEDIATE, MonsterFamily.STORAGE, "蓄積", "体内炉にエネルギーを溜め込む。", ORANGE)
    private val moonMoss = form("moon_moss", "ムーンモス", MonsterStage.INTERMEDIATE, MonsterFamily.REST, "再生", "穏やかなリズムで傷を癒やす。", GREEN)
    private val driftMark = form("drift_mark", "ドリフトマーク", MonsterStage.INTERMEDIATE, MonsterFamily.REST, "幻惑", "変化する生活リズムを霧としてまとう。", PURPLE)
    private val igniDash = form("igni_dash", "イグニダッシュ", MonsterStage.INTERMEDIATE, MonsterFamily.OVERDRIVE, "高速火力", "十分な燃料で高出力を維持する。", ORANGE)
    private val crackRun = form("crack_run", "クラックラン", MonsterStage.INTERMEDIATE, MonsterFamily.OVERDRIVE, "諸刃", "活動に栄養が追いつかずコアに亀裂がある。", RED)

    private val finalsByIntermediate: Map<String, Pair<MonsterForm, MonsterForm>> = mapOf(
        solfeon.id to pair(
            "astelion", "アステリオン", MonsterFamily.BALANCE, "万能", "攻守が高い次元で調和した星護獣。", GREEN,
            "chronoleos", "クロノレオス", "連続攻撃", "途切れない生活リズムで時間を操る獅子獣。", YELLOW,
        ),
        aquanel.id to pair(
            "miraflora", "ミラフローラ", MonsterFamily.BALANCE, "回復防御", "水晶花の力で戦線を維持する聖獣。", BLUE,
            "serenadia", "セレナディア", "長期支援", "毎日の小さな活動を旋律へ変える翼獣。", PURPLE,
        ),
        grandguard.id to pair(
            "armagradian", "アルマグラディオン", MonsterFamily.POWER, "重装格闘", "鍛錬で完成した装甲と剛腕を持つ。", RED,
            "bastion_rex", "バスティオンレックス", "防御反撃", "堅実な栄養から不落の装甲を得た王獣。", YELLOW,
        ),
        fangrage.id to pair(
            "volcarion", "ヴォルカリオン", MonsterFamily.POWER, "短期決戦", "十分な燃料を爆発的な打撃へ変換する。", ORANGE,
            "ragna_fang", "ラグナファング", "一撃必殺", "一点へ全エネルギーを集める双牙獣。", RED,
        ),
        velox.id to pair(
            "zephyrion", "ゼファリオン", MonsterFamily.SPEED, "高速連撃", "毎日の歩みが風の翼となった疾走獣。", BLUE,
            "cross_gale", "クロスゲイル", "初手特化", "一日の長距離移動で嵐を切り裂く。", GREEN,
        ),
        skyRush.id to pair(
            "volt_rave", "ボルトレイヴ", MonsterFamily.SPEED, "雷撃回避", "高強度の鼓動から雷を生む飛竜。", PURPLE,
            "storm_arc", "ストームアーク", "持久戦", "長時間の運動を途切れない電流へ変える。", BLUE,
        ),
        bulkDome.id to pair(
            "titan_bulwark", "タイタンバルワーク", MonsterFamily.STORAGE, "超防御", "大地のような甲殻であらゆる攻撃を受け止める。", YELLOW,
            "gravi_dozer", "グラビドーザー", "重量突進", "増え始めた活動を重力突進へ転換する。", ORANGE,
        ),
        emberPot.id to pair(
            "flare_glum", "フレアグルム", MonsterFamily.STORAGE, "蓄積解放", "蓄えた糖質エネルギーを大火球へ変える。", ORANGE,
            "magna_kiln", "マグナキルン", "反撃火力", "攻撃を受けるほど体内炉が高熱になる。", RED,
        ),
        moonMoss.id to pair(
            "luna_verde", "ルナヴェルデ", MonsterFamily.REST, "回復反撃", "静かな月光で傷を回復する森の守護獣。", GREEN,
            "elder_moss", "エルダーモス", "後半成長", "少しずつ増えた歩みで巨木の力を得る。", YELLOW,
        ),
        driftMark.id to pair(
            "nebra_shade", "ネブラシェイド", MonsterFamily.REST, "回避弱体", "生活の波を濃霧として操る幻獣。", PURPLE,
            "mist_wraith", "ミストレイス", "変則行動", "不規則な活動を読めない攻撃へ変える。", BLUE,
        ),
        igniDash.id to pair(
            "sol_blazer", "ソルブレイザー", MonsterFamily.OVERDRIVE, "高速高火力", "活動と食事が両立した太陽の走竜。", ORANGE,
            "prominence_gear", "プロミネンスギア", "炎の持久戦", "長時間燃え続ける機関炉を持つ。", YELLOW,
        ),
        crackRun.id to pair(
            "revenant_gear", "レヴナントギア", MonsterFamily.OVERDRIVE, "高火力低耐久", "亀裂から漏れる力を攻撃へ転用する機獣。", RED,
            "phoenix_crest", "フェニクレスト", "復活再起", "食事と休養の改善によって再生した不死鳥獣。", GREEN,
        ),
    )

    private fun pair(
        firstId: String,
        firstName: String,
        family: MonsterFamily,
        firstRole: String,
        firstDescription: String,
        firstAccent: Long,
        secondId: String,
        secondName: String,
        secondRole: String,
        secondDescription: String,
        secondAccent: Long,
    ) = MonsterForm(firstId, firstName, MonsterStage.FINAL, family, firstRole, firstDescription, firstAccent) to
        MonsterForm(secondId, secondName, MonsterStage.FINAL, family, secondRole, secondDescription, secondAccent)

    fun evaluate(
        allDays: List<DailyHealthData>,
        goals: UserGoals,
        seasonStart: LocalDate,
        today: LocalDate,
    ): EvolutionResult {
        val seasonDay = (ChronoUnit.DAYS.between(seasonStart, today).toInt() + 1).coerceIn(1, 28)
        val sorted = allDays.filter { !it.date.isBefore(seasonStart) && !it.date.isAfter(today) }.sortedBy { it.date }
        val familyMetrics = metrics(sorted.take(7), goals)
        val family = chooseFamily(familyMetrics)
        val intermediateMetrics = metrics(sorted.takeLast(7), goals)
        val intermediate = chooseIntermediate(family, intermediateMetrics)
        val finalMetrics = metrics(sorted.takeLast(7), goals)
        val candidates = finalsByIntermediate.getValue(intermediate.id).let { listOf(it.first, it.second) }
        val final = chooseFinal(intermediate, candidates, finalMetrics)

        return when (seasonDay) {
            in 1..7 -> EvolutionResult(seasonDay, baby, listOf(baby), listOf(), metrics(sorted, goals), 8)
            in 8..14 -> EvolutionResult(seasonDay, family, listOf(baby, family), listOf(), metrics(sorted, goals), 15)
            in 15..21 -> EvolutionResult(seasonDay, intermediate, listOf(baby, family, intermediate), candidates, finalMetrics, 22)
            else -> EvolutionResult(seasonDay, final, listOf(baby, family, intermediate, final), candidates, finalMetrics, null)
        }
    }

    private fun chooseFamily(m: EvolutionMetrics): MonsterForm = when {
        m.activityScore >= 115 || m.highActivityDays >= 5 -> runpact
        m.strengthDays >= 2 || (m.proteinRatio >= 1.05 && m.activityScore >= 70) -> galvol
        m.cardioDays >= 2 || m.stepGoalDays >= 5 -> rapizel
        m.calorieRatio >= 1.10 || m.carbsRatio >= 1.15 || m.fatRatio >= 1.15 -> motchigrow
        m.activityScore < 65 -> mossleep
        else -> leafang
    }

    private fun chooseIntermediate(family: MonsterForm, m: EvolutionMetrics): MonsterForm = when (family.family) {
        MonsterFamily.BALANCE -> if (m.consistencyScore >= 80) solfeon else aquanel
        MonsterFamily.POWER -> if (m.strengthDays >= 3) grandguard else fangrage
        MonsterFamily.SPEED -> if (m.stepGoalDays >= 5) velox else skyRush
        MonsterFamily.STORAGE -> if (m.fatRatio >= m.carbsRatio) bulkDome else emberPot
        MonsterFamily.REST -> if (m.consistencyScore >= 70) moonMoss else driftMark
        MonsterFamily.OVERDRIVE -> if (m.calorieRatio >= 0.90) igniDash else crackRun
    }

    private fun chooseFinal(intermediate: MonsterForm, candidates: List<MonsterForm>, m: EvolutionMetrics): MonsterForm {
        val chooseFirst = when (intermediate.id) {
            solfeon.id -> m.nutritionScore >= 80 && m.consistencyScore >= 80
            aquanel.id -> m.restDays >= 2 && m.nutritionScore >= 75
            grandguard.id -> m.strengthDays >= 3 && m.consistencyScore >= 70
            fangrage.id -> m.calorieRatio >= 0.90
            velox.id -> m.stepGoalDays >= 6
            skyRush.id -> m.highActivityDays >= 3
            bulkDome.id -> m.activityScore < 70
            emberPot.id -> m.carbsRatio >= m.fatRatio
            moonMoss.id -> m.activityTrend <= 0.15
            driftMark.id -> m.weekdayWeekendVariance >= 0.25
            igniDash.id -> m.calorieRatio >= 0.90 && m.activityScore >= 110
            crackRun.id -> m.calorieTrend <= 0.10 && m.calorieRatio < 0.90
            else -> true
        }
        return if (chooseFirst) candidates.first() else candidates.last()
    }

    fun metrics(days: List<DailyHealthData>, goals: UserGoals): EvolutionMetrics {
        if (days.isEmpty()) return EvolutionMetrics()
        val nutritionDays = days.filter { it.hasNutrition }
        val activityDays = days.filter { it.hasActivity }
        val calorieRatio = nutritionDays.averageOf({ it.calories / goals.calories.coerceAtLeast(1) })
        val proteinRatio = nutritionDays.averageOf({ it.proteinGrams / goals.proteinGrams.coerceAtLeast(1) })
        val carbsRatio = nutritionDays.averageOf({ it.carbsGrams / goals.carbsGrams.coerceAtLeast(1) })
        val fatRatio = nutritionDays.averageOf({ it.fatGrams / goals.fatGrams.coerceAtLeast(1) })
        val stepRatio = activityDays.averageOf({ it.steps.toDouble() / goals.dailySteps.coerceAtLeast(1) })
        val weeklyExerciseGoalPerDay = goals.weeklyExerciseMinutes.coerceAtLeast(1) / 7.0
        val exerciseRatio = activityDays.averageOf({ it.exerciseMinutes / weeklyExerciseGoalPerDay })

        val nutritionScore = if (nutritionDays.isEmpty()) 0 else (
            100.0 - listOf(calorieRatio, proteinRatio, carbsRatio, fatRatio)
                .map { abs(1.0 - it).coerceAtMost(1.0) }
                .average() * 100.0
            ).roundToInt().coerceIn(0, 100)
        val activityScore = if (activityDays.isEmpty()) 0 else
            ((stepRatio.coerceAtMost(1.5) * 60.0) + (exerciseRatio.coerceAtMost(1.5) * 40.0)).roundToInt()
        val recorded = days.count { it.hasNutrition || it.hasActivity }
        val consistency = (recorded.toDouble() / days.size.coerceAtLeast(1) * 100).roundToInt()
        val firstHalf = activityDays.take((activityDays.size / 2).coerceAtLeast(1)).sumOf { it.steps }.toDouble()
        val secondHalf = activityDays.drop(activityDays.size / 2).sumOf { it.steps }.toDouble()
        val activityTrend = if (firstHalf <= 0) 0.0 else (secondHalf - firstHalf) / firstHalf
        val firstCalories = nutritionDays.take((nutritionDays.size / 2).coerceAtLeast(1)).sumOf { it.calories }
        val secondCalories = nutritionDays.drop(nutritionDays.size / 2).sumOf { it.calories }
        val calorieTrend = if (firstCalories <= 0) 0.0 else (secondCalories - firstCalories) / firstCalories
        val weekday = activityDays.filter { it.date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }.map { it.steps.toDouble() }
        val weekend = activityDays.filter { it.date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }.map { it.steps.toDouble() }
        val weekdayAvg = weekday.averageOrZero()
        val weekendAvg = weekend.averageOrZero()
        val variance = if (weekdayAvg <= 0 || weekendAvg <= 0) 0.0 else abs(weekdayAvg - weekendAvg) / maxOf(weekdayAvg, weekendAvg)

        return EvolutionMetrics(
            nutritionScore = nutritionScore,
            activityScore = activityScore,
            consistencyScore = consistency,
            calorieRatio = calorieRatio,
            proteinRatio = proteinRatio,
            carbsRatio = carbsRatio,
            fatRatio = fatRatio,
            stepGoalDays = activityDays.count { it.steps >= goals.dailySteps },
            strengthDays = days.count { it.workoutTag == WorkoutTag.STRENGTH },
            cardioDays = days.count { it.workoutTag == WorkoutTag.CARDIO },
            highActivityDays = activityDays.count { it.steps >= goals.dailySteps * 1.2 || it.exerciseMinutes >= weeklyExerciseGoalPerDay * 1.4 },
            restDays = activityDays.count { it.steps < goals.dailySteps * 0.7 && it.exerciseMinutes < 10 },
            activityTrend = activityTrend,
            calorieTrend = calorieTrend,
            weekdayWeekendVariance = variance,
            recordedDays = recorded,
        )
    }

    private inline fun <T> List<T>.averageOf(selector: (T) -> Double): Double =
        if (isEmpty()) 0.0 else sumOf(selector) / size

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
}
