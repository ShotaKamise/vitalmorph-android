# VitaMorph 引き継ぎ状況

最終更新: 2026-07-12

## 現在の完成範囲

- Androidアプリ基盤
- Health Connectから歩数、活動カロリー、運動時間、栄養を読み取る処理
- あすけんの目標値に合わせたカロリー・PFC目標設定
- 28日シーズン、7日ごとの4段階進化
- 現行43形態の動物系モンスター画像
- 新構成用の人型28体（男女、成熟体、最終形態）の画像と正式ID
- 待機、攻撃、被弾、勝利アニメーション
- 技とアイテムを選ぶCPUターン制トーナメント
- 系統固有技、エネルギー、ガード、回復、CPU判断、バトルログ
- 13シーズンと1,100XPのトレーナーランク
- デモモード
- SharedPreferencesによる既存ゲーム状態保存
- GitHub Actionsによるテストとdebug APK生成
- タグによる署名済みrelease APK公開ワークフロー

## 現在のバージョン

- applicationId: `app.vitalmorph`
- versionCode: `4`
- versionName: `0.3.1`
- minSdk: `28`
- targetSdk / compileSdk: `36`

## まだ実装していないもの

- トレーナー名
- 孵化時にランダム決定されるオス・メス
- 性別固定の進化ルート
- 人型28体を進化ルートと図鑑へ組み込む処理
- タッチリアクション
- 機嫌、絆、性格
- トレーナー名を含むローカルテキスト会話
- ミニゲーム
- 機嫌と絆のバトル反映
- 28日終了時の能力継承
- VitaMorph内での食事入力、食品検索、履歴、お気に入り
- Health ConnectへのNutritionRecord書き込み
- バーコード検索

## 現在の既知の制約

- ターン制大会の進行状態はメモリ上にあり、アプリを終了すると試合途中から再開できない。
- 栄養データはHealth Connectからの読み取りのみ。
- 現行進化エンジンはオス・メスを扱わない。
- 現在のゲーム表示は現行43形態を利用する。新規人型28体の画像とIDは完成済みだが、進化エンジンへの組み込みは未実施。
- 実機接続による最終操作確認は未実施。ローカルのテスト、Lint、APKビルドは成功済み。

## Claudeが最初に行う作業

`docs/IMPLEMENTATION_PLAN.md` のPhase 1だけを先に実装する。

1. Room導入方針を提示
2. 既存SharedPreferencesからの移行設計
3. TrainerProfile、MonsterSex、MonsterGeneration、Mood、Bond、LegacyStatsのモデル追加
4. 既存ユーザー向けトレーナー名設定画面
5. 孵化時の性別決定と永続化
6. 単体テストと移行テスト

Phase 1完了前に、食事管理やミニゲームへ着手しない。

## Codexへの依頼

- 既存動物系14体の選抜と性別差分
- `TOUCH_HAPPY`、`TOUCH_SHY`、`TOUCH_ANNOYED`、`TALK`、`SAD`、`MINIGAME_SUCCESS` モーション追加
