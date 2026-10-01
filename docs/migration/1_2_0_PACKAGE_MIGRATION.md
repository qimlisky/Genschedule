# SleepDown 1.2.0 应用身份迁移

## 身份

- 历史正式包名：`com.example.courseschedule`
- 新长期正式包名：`com.xiaomanjun.sleepdownschedule`
- Debug：`com.xiaomanjun.sleepdownschedule.debug`
- GitHub/Store 正式版本共用正式包名；渠道由 `BuildConfig.DISTRIBUTION_CHANNEL` 提供。

v1.1.5（versionCode 25）不修改、不覆盖发布历史。新包首个公开版本为 versionCode 26 / versionName 1.2.0。

## 备份迁移

用户在旧 v1.1.5 中显式导出 `.sleepdown`，安装新包后在“备份与恢复”中恢复。`BackupFormatV1` 的 manifest、checksum、ZIP 安全限制、事务 journal 和稳定 ID 均保持不变。

恢复校验通过 `AppIdentity.isTrustedBackupSource` 执行：来源必须等于当前运行包名，或等于 `LEGACY_PACKAGE_NAME`；其他来源（例如 `com.fake.sleepdown`）拒绝。checksum、ZIP 损坏及不支持的格式版本仍由原有 `BackupCodec` 校验拒绝。

## 发行渠道

- `githubRelease`：允许现有 Gitee/GitHub APK 自更新，声明 `REQUEST_INSTALL_PACKAGES` 并包含下载前台服务。
- `storeRelease`：不声明该权限，不合并下载服务，业务层通过 `AppDistribution.supportsSelfUpdate` 关闭更新检查、下载和安装入口。
- Debug 变体使用 debug signing 与 `.debug` suffix；Benchmark 使用 debug signing，不依赖正式私钥。

正式 Release 未配置四项签名参数时，assemble/bundle/package Release 任务会明确失败，绝不回退到 debug key。签名参数可来自 Gradle properties、`SLEEPDOWN_RELEASE_*` 环境变量，或 Android Studio「Generate Signed Bundle or APK」向导注入的 `android.injected.signing.*`。

向导那条链路不进任何配置文件：向导在触发构建时以 `-P` 传入 `android.injected.signing.store.file` / `.store.password` / `.key.alias` / `.key.password`，AGP 读到四项齐全后建一个名为 `externalOverride` 的签名配置覆盖该次构建的签名配置，实际签名由 AGP 完成。`app/build.gradle.kts` 只把这四项用于放行门禁，不据此创建 `release` signing config（`release` config 仍只认 `sleepdown.release*` 的明文值）。因此向导可以打包，而 CI 与命令行构建仍然必须显式配置签名，两者都不会回退到 debug key。

## 暂缓审计

本阶段仅保留 cleartext HTTP 与 Android Auto Backup 现状，未改变教务 WebView 流程或备份架构；隐私政策、用户协议、备案、服务器、AI 合规和 UI/动画性能整改另行处理。
