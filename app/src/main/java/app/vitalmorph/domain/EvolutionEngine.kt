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

    // 新43枠構成(docs/MONSTER_ROSTER.md)で最終形態として使う動物系6体。
    // オス: アステリオン、バスティオンレックス、ゼファリオン
    // メス: ミラフローラ、ルナヴェルデ、フェニクレスト
    private val astelion = finalsByIntermediate.getValue(solfeon.id).first
    private val miraflora = finalsByIntermediate.getValue(aquanel.id).first
    private val bastionRex = finalsByIntermediate.getValue(grandguard.id).second
    private val zephyrion = finalsByIntermediate.getValue(velox.id).first
    private val lunaVerde = finalsByIntermediate.getValue(moonMoss.id).first
    private val phoenixCrest = finalsByIntermediate.getValue(crackRun.id).second

    /**
     * 図鑑・検証用の全形態カタログ。承認済みIDのみを含み、公開済みIDは削除しない。
     * 旧進化表の成熟体・最終形態も、動物系14体の最終選抜が確定するまで保持する。
     */
    val allForms: List<MonsterForm> =
        listOf(baby, leafang, galvol, rapizel, motchigrow, mossleep, runpact) +
            listOf(
                solfeon, aquanel, grandguard, fangrage, velox, skyRush,
                bulkDome, emberPot, moonMoss, driftMark, igniDash, crackRun,
            ) +
            finalsByIntermediate.values.flatMap { listOf(it.first, it.second) } +
            HumanoidRoster.all

    /**
     * 新進化表(Phase 2)の評価。
     * - 第1週: 共通幼生(モルフィ)
     * - 第2週: 生活傾向による動物系成長体6種(性別差分画像は制作中のため共通画像+♂/♀表示)
     * - 第3週: 第2週の傾向から人型7職業の成熟体(性別別ID)
     * - 第4週: 第3週の生活・機嫌・絆で同職業の上位人型、または動物系最終形態
     */
    fun evaluate(
        allDays: List<DailyHealthData>,
        goals: UserGoals,
        seasonStart: LocalDate,
        today: LocalDate,
        sex: MonsterSex,
        mood: Int = MonsterGeneration.DEFAULT_MOOD,
        bond: Int = MonsterGeneration.DEFAULT_BOND,
    ): EvolutionResult {
        val seasonDay = (ChronoUnit.DAYS.between(seasonStart, today).toInt() + 1).coerceIn(1, 28)
        val sorted = allDays.filter { !it.date.isBefore(seasonStart) && !it.date.isAfter(today) }.sortedBy { it.date }
        fun window(range: IntRange) = sorted.filter {
            (ChronoUnit.DAYS.between(seasonStart, it.date).toInt() + 1) in range
        }
        val week1 = window(1..7)
        val week2 = window(8..14).ifEmpty { week1 }
        // 第4週の分岐は仕様どおり第3週の記録で判定する。第3週が無記録なら継続性0となり
        // 動物系最終形態(罰ではない完成ルート)へ向かう。
        val week3 = window(15..21)

        val family = chooseFamily(metrics(week1, goals))
        val job = chooseJob(family, metrics(week2, goals))
        val mature = HumanoidRoster.mature(job, sex)
        val finalDecisionMetrics = metrics(week3, goals)
        val humanoidFinal = HumanoidRoster.finalForm(job, sex)
        val animalFinal = animalFinalFor(job.family, sex)
        val towardHumanoid = choosesHumanoidFinal(finalDecisionMetrics, mood, bond)
        val final = if (towardHumanoid) humanoidFinal else animalFinal
        val candidates = if (towardHumanoid) listOf(humanoidFinal, animalFinal) else listOf(animalFinal, humanoidFinal)

        return when (seasonDay) {
            in 1..7 -> EvolutionResult(seasonDay, baby, listOf(baby), listOf(), metrics(sorted, goals), 8)
            in 8..14 -> EvolutionResult(seasonDay, family, listOf(baby, family), listOf(), metrics(sorted, goals), 15)
            in 15..21 -> EvolutionResult(seasonDay, mature, listOf(baby, family, mature), candidates, finalDecisionMetrics, 22)
            else -> EvolutionResult(seasonDay, final, listOf(baby, family, mature, final), candidates, metrics(sorted.takeLast(7), goals), null)
        }
    }

    /**
     * 第2週の傾向から人型7職業を決める(2026-07-12ユーザー承認のマッピング)。
     * - 調和→剣士 / 筋力・蓄積→大剣使い / 過活動→ショットガン使い
     * - 俊足は歩数型→二刀流剣士、運動時間型→槍使い
     * - 静養は規則的→魔法使い、不規則→忍者
     */
    fun chooseJob(family: MonsterForm, m: EvolutionMetrics): HumanoidJob = when (family.family) {
        MonsterFamily.BALANCE -> HumanoidJob.SWORDSMAN
        MonsterFamily.SPEED -> if (m.stepGoalDays >= 4) HumanoidJob.DUAL_BLADE else HumanoidJob.LANCER
        MonsterFamily.POWER, MonsterFamily.STORAGE -> HumanoidJob.GREATSWORD
        MonsterFamily.OVERDRIVE -> HumanoidJob.GUNNER
        MonsterFamily.REST -> if (m.consistencyScore >= 70) HumanoidJob.MAGE else HumanoidJob.NINJA
    }

    /**
     * 第4週の分岐。記録の継続と、機嫌または絆が高ければ同職業の上位人型へ。
     * 交流が少なくても動物系最終形態という完成ルートが残る(罰にしない)。
     */
    fun choosesHumanoidFinal(m: EvolutionMetrics, mood: Int, bond: Int): Boolean =
        m.consistencyScore >= 50 && (mood >= 60 || bond >= 40)

    /** 職業の系統と性別から動物系最終形態を選ぶ。 */
    fun animalFinalFor(family: MonsterFamily, sex: MonsterSex): MonsterForm = when (sex) {
        MonsterSex.MALE -> when (family) {
            MonsterFamily.BALANCE, MonsterFamily.REST -> astelion
            MonsterFamily.POWER, MonsterFamily.STORAGE -> bastionRex
            MonsterFamily.SPEED, MonsterFamily.OVERDRIVE -> zephyrion
        }
        MonsterSex.FEMALE -> when (family) {
            MonsterFamily.BALANCE, MonsterFamily.STORAGE -> miraflora
            MonsterFamily.REST -> lunaVerde
            MonsterFamily.SPEED, MonsterFamily.POWER, MonsterFamily.OVERDRIVE -> phoenixCrest
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
