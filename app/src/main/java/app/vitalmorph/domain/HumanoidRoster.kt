package app.vitalmorph.domain

/**
 * 人型7職業。`docs/MONSTER_ROSTER.md` の承認済み構成に対応する。
 * 系統は職業ごとに固定で、性別による能力差は付けない。
 */
enum class HumanoidJob(val label: String, val family: MonsterFamily) {
    SWORDSMAN("剣士", MonsterFamily.BALANCE),
    DUAL_BLADE("二刀流剣士", MonsterFamily.SPEED),
    GREATSWORD("大剣使い", MonsterFamily.POWER),
    LANCER("槍使い", MonsterFamily.SPEED),
    GUNNER("ショットガン使い", MonsterFamily.OVERDRIVE),
    MAGE("魔法使い", MonsterFamily.REST),
    NINJA("忍者", MonsterFamily.REST),
}

/**
 * 人型28体(7職業 × 成熟体/最終形態 × オス/メス)。
 * IDは `docs/MONSTER_ROSTER.md` で承認済みの正式IDで、公開後は変更しない。
 */
object HumanoidRoster {
    // 職業テーマカラー(docs/MONSTER_ROSTER.mdの「色」列に対応)。
    // 黒・白はダーク背景のオーラとして見えるよう明度を調整している。
    private const val RED = 0xFFFF6B6B // 剣士
    private const val BLUE = 0xFF66B7FF // 二刀流剣士
    private const val GREEN = 0xFF69E6A6 // 大剣使い
    private const val YELLOW = 0xFFFFD166 // 槍使い
    private const val BLACK = 0xFF8E9AAF // ショットガン使い(黒鉄)
    private const val WHITE = 0xFFF1F7FA // 魔法使い
    private const val PURPLE = 0xFFB89CFF // 忍者

    private data class JobLine(
        val matureMale: MonsterForm,
        val finalMale: MonsterForm,
        val matureFemale: MonsterForm,
        val finalFemale: MonsterForm,
    )

    private fun matureOf(id: String, name: String, job: HumanoidJob, description: String, accent: Long) =
        MonsterForm(id, name, MonsterStage.INTERMEDIATE, job.family, job.label, description, accent)

    private fun finalOf(id: String, name: String, job: HumanoidJob, description: String, accent: Long) =
        MonsterForm(id, name, MonsterStage.FINAL, job.family, "上位${job.label}", description, accent)

    private val lines: Map<HumanoidJob, JobLine> = mapOf(
        HumanoidJob.SWORDSMAN to JobLine(
            matureOf("leon_saber_m", "レオンセイバー", HumanoidJob.SWORDSMAN, "調和の取れた生活から生まれた若き剣士。", RED),
            finalOf("sol_regnard_m", "ソルレグナード", HumanoidJob.SWORDSMAN, "太陽の剣で仲間を守り抜く剣王。", RED),
            matureOf("valeria_f", "ヴァレリア", HumanoidJob.SWORDSMAN, "均整の取れた鍛錬を積み重ねる女性剣士。", RED),
            finalOf("val_rose_f", "ヴァルローゼ", HumanoidJob.SWORDSMAN, "薔薇の剣舞で戦場を制する剣姫。", RED),
        ),
        HumanoidJob.DUAL_BLADE to JobLine(
            matureOf("twin_fang_m", "ツインファング", HumanoidJob.DUAL_BLADE, "日々の歩みを二本の剣速へ変える剣士。", BLUE),
            finalOf("zero_dualion_m", "ゼロデュアリオン", HumanoidJob.DUAL_BLADE, "残像すら置き去りにする二刀の極致。", BLUE),
            matureOf("lila_twin_f", "リラツイン", HumanoidJob.DUAL_BLADE, "軽やかな足取りで舞う双剣使い。", BLUE),
            finalOf("lumina_duella_f", "ルミナデュエラ", HumanoidJob.DUAL_BLADE, "光の軌跡を描く双剣の舞姫。", BLUE),
        ),
        HumanoidJob.GREATSWORD to JobLine(
            matureOf("grand_breaker_m", "グランブレイカー", HumanoidJob.GREATSWORD, "鍛え上げた筋力で大剣を振るう戦士。", GREEN),
            finalOf("titan_glaive_m", "タイタングレイヴ", HumanoidJob.GREATSWORD, "巨人の名を冠する大剣の覇者。", GREEN),
            matureOf("crim_arge_f", "クリムアージュ", HumanoidJob.GREATSWORD, "紅の大剣を軽々と操る女戦士。", GREEN),
            finalOf("grand_empress_f", "グランエンプレス", HumanoidJob.GREATSWORD, "戦場に君臨する大剣の女帝。", GREEN),
        ),
        HumanoidJob.LANCER to JobLine(
            matureOf("volt_lancer_m", "ヴォルトランサー", HumanoidJob.LANCER, "運動の熱を雷の槍へ変える槍兵。", YELLOW),
            finalOf("tempest_dragoon_m", "テンペストドラグーン", HumanoidJob.LANCER, "嵐を纏い空を駆ける竜騎士。", YELLOW),
            matureOf("celes_lancer_f", "セレスランサー", HumanoidJob.LANCER, "天空の槍を操る俊敏な槍使い。", YELLOW),
            finalOf("astra_reina_f", "アストラレイナ", HumanoidJob.LANCER, "星の槍で天を貫く槍の女王。", YELLOW),
        ),
        HumanoidJob.GUNNER to JobLine(
            matureOf("barrel_guard_m", "バレルガード", HumanoidJob.GUNNER, "あふれる活動エネルギーを弾丸に込める銃士。", BLACK),
            finalOf("arc_buster_m", "アークバスター", HumanoidJob.GUNNER, "電光の砲撃で戦場を薙ぎ払う重銃士。", BLACK),
            matureOf("rouge_shell_f", "ルージュシェル", HumanoidJob.GUNNER, "紅の散弾で敵を寄せ付けない銃使い。", BLACK),
            finalOf("nova_valeria_f", "ノヴァバレリア", HumanoidJob.GUNNER, "新星の砲火を操る銃の女傑。", BLACK),
        ),
        HumanoidJob.MAGE to JobLine(
            matureOf("rune_sage_m", "ルーンセージ", HumanoidJob.MAGE, "静かな暮らしから魔力を紡ぐ賢者。", WHITE),
            finalOf("astra_magius_m", "アストラマギウス", HumanoidJob.MAGE, "星々の魔導を極めた大魔導士。", WHITE),
            matureOf("mystica_f", "ミスティカ", HumanoidJob.MAGE, "穏やかな休息を魔法の泉に変える魔女。", WHITE),
            finalOf("eclipsia_f", "エクリプシア", HumanoidJob.MAGE, "月蝕の魔力を操る大魔女。", WHITE),
        ),
        HumanoidJob.NINJA to JobLine(
            matureOf("kagerou_m", "カゲロウ", HumanoidJob.NINJA, "気配を消して夜を駆ける忍び。", PURPLE),
            finalOf("mugen_shinobi_m", "ムゲンシノビ", HumanoidJob.NINJA, "幻影を無限に生み出す忍の頭領。", PURPLE),
            matureOf("yoidzuki_f", "ヨイヅキ", HumanoidJob.NINJA, "宵闇に紛れて舞うくノ一。", PURPLE),
            finalOf("tsukikage_hime_f", "ツキカゲヒメ", HumanoidJob.NINJA, "月影を統べる忍の姫。", PURPLE),
        ),
    )

    fun mature(job: HumanoidJob, sex: MonsterSex): MonsterForm = lines.getValue(job).let {
        if (sex == MonsterSex.MALE) it.matureMale else it.matureFemale
    }

    fun finalForm(job: HumanoidJob, sex: MonsterSex): MonsterForm = lines.getValue(job).let {
        if (sex == MonsterSex.MALE) it.finalMale else it.finalFemale
    }

    /** 図鑑や検証で使う全28体。 */
    val all: List<MonsterForm> = lines.values.flatMap {
        listOf(it.matureMale, it.finalMale, it.matureFemale, it.finalFemale)
    }
}
