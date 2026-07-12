package app.vitalmorph.domain

import java.time.LocalDate

/**
 * モンスターの性別。能力差は付けず、Phase 2以降で進化ルートと外見のみを分ける。
 */
enum class MonsterSex(val label: String, val mark: String) {
    MALE("オス", "♂"),
    FEMALE("メス", "♀"),
}

/**
 * トレーナープロフィール。名前は会話表示(Phase 2)へ利用する。
 */
data class TrainerProfile(
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * トレーナー名のルール。1〜12文字、改行・制御文字を含まない。
 */
object TrainerNameRules {
    const val MIN_LENGTH = 1
    const val MAX_LENGTH = 12

    /** 保存用に前後の空白を除去する。 */
    fun normalize(raw: String): String = raw.trim()

    /** 妥当なら null、不正ならユーザー向けエラーメッセージを返す。 */
    fun validate(raw: String): String? {
        val name = normalize(raw)
        if (name.length < MIN_LENGTH) return "トレーナー名を入力してください"
        if (name.length > MAX_LENGTH) return "トレーナー名は${MAX_LENGTH}文字以内にしてください"
        if (name.any { it == '\n' || it == '\r' || it.isISOControl() }) {
            return "改行や特殊文字は使えません"
        }
        return null
    }
}

/**
 * 進化ルートの適性。孵化時に約50%で抽選し、同じ世代では変更しない(v0.10)。
 * - HUMANOID: 第3週から人型7職業へ進む
 * - BEAST: 動物のまま成熟し、系統固有の最終形態へ進む
 */
enum class EvolutionRoute(val label: String) {
    HUMANOID("人型ルート"),
    BEAST("動物ルート"),
}

/**
 * 28日シーズンごとの1世代。性別とルート適性は孵化時に決定し、同じ世代では変更しない。
 * シーズン完了時に最終形態・大会順位・継承内容を記録し、系譜画面で表示する。
 */
data class MonsterGeneration(
    val generationId: Long = 0,
    val generationNumber: Int,
    val sex: MonsterSex,
    val route: EvolutionRoute = EvolutionRoute.HUMANOID,
    val seasonStart: LocalDate,
    val seasonEnd: LocalDate? = null,
    val mood: Int = DEFAULT_MOOD,
    val bond: Int = DEFAULT_BOND,
    /** シーズン完了時点の形態ID。進行中はnull。 */
    val finalFormId: String? = null,
    /** シーズン中の大会最高順位(1=優勝)。大会未参加はnull。 */
    val finalPlacement: Int? = null,
    /** この世代で実際に付与された継承ポイント(上限適用後)。 */
    val awardedHp: Int = 0,
    val awardedAttack: Int = 0,
    val awardedDefense: Int = 0,
    val awardedSpeed: Int = 0,
) {
    companion object {
        const val MOOD_MIN = 0
        const val MOOD_MAX = 100
        const val BOND_MIN = 0
        const val BOND_MAX = 100
        const val DEFAULT_MOOD = 50
        const val DEFAULT_BOND = 0

        fun clampMood(value: Int): Int = value.coerceIn(MOOD_MIN, MOOD_MAX)
        fun clampBond(value: Int): Int = value.coerceIn(BOND_MIN, BOND_MAX)
    }
}

/**
 * 世代をまたぐ能力継承ポイント(Phase 4でバトルへ反映)。
 * - 1世代で獲得できるのは最大 [MAX_POINTS_PER_GENERATION]
 * - 1能力の上限は [MAX_POINTS_PER_STAT]
 * - 移行や再計算でポイントが減ることはない
 */
data class LegacyStats(
    val hpPoints: Int = 0,
    val attackPoints: Int = 0,
    val defensePoints: Int = 0,
    val speedPoints: Int = 0,
    val totalGenerations: Int = 0,
) {
    /**
     * 1世代分のポイントを加算した新しい値を返す。
     * 合計が [MAX_POINTS_PER_GENERATION] を超える入力は先頭から切り詰め、
     * 各能力は [MAX_POINTS_PER_STAT] で頭打ちにする。既存値は決して減らない。
     */
    fun addingGeneration(hp: Int = 0, attack: Int = 0, defense: Int = 0, speed: Int = 0): LegacyStats {
        var budget = MAX_POINTS_PER_GENERATION
        fun take(requested: Int): Int {
            val granted = requested.coerceIn(0, budget)
            budget -= granted
            return granted
        }
        val grantedHp = take(hp)
        val grantedAttack = take(attack)
        val grantedDefense = take(defense)
        val grantedSpeed = take(speed)
        return LegacyStats(
            hpPoints = (hpPoints + grantedHp).coerceAtMost(MAX_POINTS_PER_STAT).coerceAtLeast(hpPoints),
            attackPoints = (attackPoints + grantedAttack).coerceAtMost(MAX_POINTS_PER_STAT).coerceAtLeast(attackPoints),
            defensePoints = (defensePoints + grantedDefense).coerceAtMost(MAX_POINTS_PER_STAT).coerceAtLeast(defensePoints),
            speedPoints = (speedPoints + grantedSpeed).coerceAtMost(MAX_POINTS_PER_STAT).coerceAtLeast(speedPoints),
            totalGenerations = totalGenerations + 1,
        )
    }

    companion object {
        const val MAX_POINTS_PER_GENERATION = 3
        const val MAX_POINTS_PER_STAT = 15

        /** 1ポイントあたりの基礎能力倍率(約1%)。 */
        const val RATIO_PER_POINT = 0.01
    }
}
