# Monster Asset Contract

この文書は、Claude実装とCodex制作の境界を定義する。

## 現在の公開API

実装場所:

```text
app/src/main/java/app/vitalmorph/ui/MonsterArtwork.kt
```

フォームから画像とアニメーションを表示する。

```kotlin
MonsterVisual(
    form = monsterForm,
    modifier = Modifier.size(180.dp),
    motion = MonsterMotion.IDLE,
    facingRight = true,
    showAura = true,
)
```

CPUなど、フォームモデルを持たない表示:

```kotlin
MonsterSprite(
    drawableRes = MonsterArtwork.resourceForOpponent(opponentName),
    contentDescription = opponentName,
    accent = accentColor,
    motion = MonsterMotion.HIT,
    facingRight = false,
)
```

現在のモーション:

- `IDLE`
- `ATTACK`
- `VICTORY`
- `HIT`

## 追加予定モーション

Codexが次を追加する予定。Claudeはenumを先に追加しない。

- `TOUCH_HAPPY`
- `TOUCH_SHY`
- `TOUCH_ANNOYED`
- `TALK`
- `SAD`
- `MINIGAME_SUCCESS`

必要になった場合は `docs/HANDOFF_STATUS.md` の「Codexへの依頼」へ記載する。

## 画像仕様

- 保存先: `app/src/main/res/drawable-nodpi/`
- 命名: `monster_<stable_form_id>.webp`
- 形式: 透過WebP
- 標準キャンバス: 512 x 512
- 左上を含む四隅が透明であること
- 画像内へキャラクター名やUI文字を描き込まない
- `contentDescription` は画面側で設定する

## ID規則

- 公開後の `formId` はセーブデータキーとして扱い、名前変更しない。
- 表示名変更と `formId` 変更を分離する。
- 性別別フォームは別IDを持つ。
- 正式IDはCodexが `docs/MONSTER_ROSTER.md` で承認済みにしてからClaudeが利用する。
- 未確定期間は職業enumなどの論理分類を使い、仮の永続IDを作らない。

## Claudeがしてよいこと

- `MonsterVisual` を画面へ配置
- 状態に応じて既存 `MonsterMotion` を選択
- サイズ、向き、オーラ表示を指定
- タップイベントを外側のコンテナで受ける
- キャラクターの状態や会話ロジックを実装

## Claudeがしてはいけないこと

- 既存モンスター画像の上書き、削除、色変換
- `MonsterArtwork` のIDマッピング変更
- 独自の代替キャラクター画像追加
- 既存の生成原画やプロンプトの削除
- Codex未承認の人型キャラクターIDをセーブデータへ保存

## タッチ統合予定

タッチ座標や部位判定はClaude側で管理し、表示モーションは公開APIへ渡す。

```text
UI tap
  -> InteractionEngine determines reaction
  -> mood / bond update
  -> requested MonsterMotion
  -> MonsterVisual renders animation
  -> DialogueEngine selects local text
```

