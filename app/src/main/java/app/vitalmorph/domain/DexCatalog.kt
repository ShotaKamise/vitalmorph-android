package app.vitalmorph.domain

/**
 * 図鑑(v0.11 / COMPLETION_PLAN T4)の表示カタログ。
 * [EvolutionEngine.allForms] の全71体を、系統・ルート・性別で並べた純粋なセクション列にする。
 * UIはこのセクションをそのまま描画し、ロジックはここへ集約して単体テスト可能にする。
 */
object DexCatalog {
    data class Section(val title: String, val forms: List<MonsterForm>)

    /** 共通: 幼生モルフィ + 成長体6体。性別マークは付かない。 */
    private val common: List<MonsterForm> =
        EvolutionEngine.allForms.filter {
            it.stage == MonsterStage.BABY || it.stage == MonsterStage.FAMILY
        }

    /** 人型ルート28体。♂(ID末尾 _m)を先に、♀(ID末尾 _f)を後に並べる。 */
    private val humanoid: List<MonsterForm> =
        HumanoidRoster.all.filter { it.id.endsWith("_m") } +
            HumanoidRoster.all.filter { it.id.endsWith("_f") }

    /** 動物ルート36体(成熟体+最終形態)。♂を先に、♀を後に並べる。 */
    private val beast: List<MonsterForm> = run {
        val animal = EvolutionEngine.allForms.filter { it.id in EvolutionEngine.animalFormSex }
        animal.filter { EvolutionEngine.animalFormSex[it.id] == MonsterSex.MALE } +
            animal.filter { EvolutionEngine.animalFormSex[it.id] == MonsterSex.FEMALE }
    }

    /** 図鑑の表示順セクション(共通→人型→動物)。 */
    val sections: List<Section> = listOf(
        Section("共通", common),
        Section("人型ルート", humanoid),
        Section("動物ルート", beast),
    )

    /** 図鑑の総数(=全71体)。 */
    val totalForms: Int = sections.sumOf { it.forms.size }

    /** フォームの性別マーク。共通(幼生・成長体)は無印でnullを返す。 */
    fun sexMark(formId: String): String? = when {
        formId.endsWith("_m") -> MonsterSex.MALE.mark
        formId.endsWith("_f") -> MonsterSex.FEMALE.mark
        else -> EvolutionEngine.animalFormSex[formId]?.mark
    }
}
