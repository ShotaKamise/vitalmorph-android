# VitaMorph 引き継ぎ状況

最終更新: 2026-07-12

## 現在の完成範囲

- Androidアプリ基盤
- Health Connectから歩数、活動カロリー、運動時間、栄養を読み取る処理
- あすけんの目標値に合わせたカロリー・PFC目標設定
- 28日シーズン、7日ごとの4段階進化
- 現行43形態の動物系モンスター画像
- 新構成用の人型28体(男女、成熟体、最終形態)の画像と正式ID
- 待機、攻撃、被弾、勝利アニメーション
- 技とアイテムを選ぶCPUターン制トーナメント
- 系統固有技、エネルギー、ガード、回復、CPU判断、バトルログ
- 13シーズンと1,100XPのトレーナーランク
- デモモード
- SharedPreferencesによる既存ゲーム状態保存
- GitHub Actionsによるテストとdebug APK生成(main、claude/**、codex/**ブランチで実行)
- タグによる署名済みrelease APK公開ワークフロー
- (v0.4 / Phase 1) Roomデータベース導入(trainer_profile、monster_generation、legacy_stats)
- (v0.4 / Phase 1) トレーナー名の初回設定(ホーム)と変更(設定タブ)、1〜12文字・制御文字禁止の検証
- (v0.4 / Phase 1) 孵化時のオス・メス決定と永続化、ホーム画面での♂/♀表示
- (v0.4 / Phase 1) 機嫌・絆・継承ポイントの保存基盤(バトル反映は未実装)

## 現在のバージョン

- applicationId: `app.vitalmorph`
- versionCode: `5`
- versionName: `0.4.0`
- minSdk: `28`
- targetSdk / compileSdk: `36`
- Room: `2.8.4`(KSP `2.3.9`、スキーマは `app/schemas/` へ出力)

## Phase 1の実装メモ

- 既存SharedPreferences(`GameStore`)は一切変更していない。新規データのみRoomへ保存する段階的移行。
- 既存ユーザーの現在世代の性別は、保存済み `seasonStart` と固定ソルト `vitalmorph-sex-v1` から一度だけ決定的に求めて永続化する(`SexAssigner.deterministicFor`)。起動ごとの再抽選はしない。
- シーズン完了後の新しい孵化は約50%のランダムで決定し、即座に永続化する(`GenerationPlanner`)。同じ `generationId` では性別が変化しない。
- 世代の作成・維持・クローズの判断は純粋関数 `GenerationPlanner.plan` に集約し、Androidに依存しない単体テストで検証済み。
- 継承ポイント(`LegacyStats`)は1世代最大3pt、能力ごと15pt上限、決して減らない仕様で実装済み。Phase 4でバトルへ反映する。
- 機嫌・絆は0〜100でクランプして保存する。Phase 2〜3で交流・バトルへ反映する。

## まだ実装していないもの

- 性別固定の進化ルート(Phase 2)
- 人型28体を進化ルートと図鑑へ組み込む処理
- タッチリアクション
- 性格
- トレーナー名を含むローカルテキスト会話(名前の永続化は完了、会話はPhase 2)
- ミニゲーム
- 機嫌と絆のバトル反映
- 28日終了時の能力継承ポイントの付与ロジック(保存基盤は完了)
- VitaMorph内での食事入力、食品検索、履歴、お気に入り
- Health ConnectへのNutritionRecord書き込み
- バーコード検索

## 現在の既知の制約

- ターン制大会の進行状態はメモリ上にあり、アプリを終了すると試合途中から再開できない。
- 栄養データはHealth Connectからの読み取りのみ。
- 現在のゲーム表示は現行43形態を利用する。新規人型28体の画像とIDは完成済みだが、進化エンジンへの組み込みは未実施。
- 現行進化エンジンは性別を進化先の分岐に使っていない(Phase 2で対応)。
- Roomのマイグレーション・DAOの計装テスト(androidTest)は未追加。ドメイン層の移行ロジックはJVM単体テストで検証済み。
- 実機接続による最終操作確認は未実施。CI上で `test` と `assembleDebug` は成功済み。

## Claudeが次に行う作業

Phase 1は実装完了。`docs/IMPLEMENTATION_PLAN.md` のPhase 2(新進化表、交流、会話)のうちClaude担当分に着手する。

1. 人型28体の正式ID(`MonsterArtwork.kt`)を使った性別別進化ルートの組み込み
2. 会話テンプレートエンジン(トレーナー名の安全な挿入、時刻・食事・運動・機嫌コンテキスト)
3. タッチリアクションのロジック(部位、連続タッチ、クールダウン、`InteractionState`)
4. 機嫌・絆の変動ルール

Phase 2完了前に、食事管理やミニゲームへ着手しない。

## Codexへの依頼

- 既存動物系14体の選抜と性別差分
- `TOUCH_HAPPY`、`TOUCH_SHY`、`TOUCH_ANNOYED`、`TALK`、`SAD`、`MINIGAME_SUCCESS` モーション追加
- 幼生・成長体のオス・メス外見差分の方針決定。Phase 1では全形態で共通画像に♂/♀マークを重ねて表示している
