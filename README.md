# InkPaperDiary · 墨纸日记

> 一个完全离线优先、遵循 Apple Human Interface Guidelines (HIG) 的 Android 日记应用。
> 使用 Jetpack Compose 构建，黑白极简排版，毛玻璃材质与 iOS 原生弹簧交互。

<p align="left">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.03-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Android" src="https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Room" src="https://img.shields.io/badge/Room-2.7.2-1A73E8">
  <img alt="Tests" src="https://img.shields.io/badge/Unit%20Tests-335%20passing-brightgreen">
</p>

---

## 简介

InkPaperDiary（墨纸日记）是一款把「纸感书写」与「Apple 原生体验」结合在一起的本地日记本。
所有日记默认保存于设备本地 Room 数据库，无需登录即可使用；开启应用锁后，PIN 与生物识别共同守护隐私。
若需要多设备同步，可选择性地配置 Supabase 凭据，将数据加密同步至自建后端。

**核心理念：**

- **离线优先** — 不联网也能完整记录、检索、导出。
- **HIG 原生质感** — 毛玻璃材质、发丝边框、弹簧按压缩放、无 Material 水波纹。
- **零数据绑架** — 一键导出 JSON / Markdown 压缩包，随时迁移。
- **隐私内建** — FLAG_SECURE 防截屏、应用锁、本地 PIN 加密存储。

---

## 功能特性

### 📖 日记与流

- **Apple Journal 风格日记流**：卡片式列表、多图自适应画廊拼接（1 / 2 / 3 / 4+ 张不同布局）。
- **富文本编辑**：Markdown 工具栏、标题 / 加粗 / 引用 / 列表 / 链接等快捷插入。
- **心情与天气**：8 种心情、7 种天气，胶囊徽章展示。
- **标签与置顶**：多标签归类，置顶日记带左侧强调条。
- **中英文字数统计**：中文按字、英文按词的混合计数算法。
- **回收站**：删除日记保留 30 天，可一键恢复或彻底清空。

### 🗓 检索与回顾

- **日历视图**：按月浏览，标记有日记的日期。
- **回忆（On This Day）**：展示往年今日的日记。
- **全文搜索**：按关键词 / 标签 / 心情检索。
- **统计看板**：日记总数、字数趋势、心情分布等。

### 🔄 同步与备份

- **Supabase 同步**：自建后端，基于邮箱密码登录，增量双向同步。
- **后台自动同步**：WorkManager 周期任务（联网时每小时静默执行）。
- **导出备份**：全量 JSON 备份、Markdown 压缩包（含本地配图）。
- **导入恢复**：支持 JSON 备份与 TXT 纯文本批量导入（自动识别日期、天气、心情、标签，兼容 GBK/UTF-8）。

### 🔒 安全与隐私

- **应用锁**：4 位 PIN 密码，`PinCipher` 加密存储。
- **生物识别**：指纹 / 面容快速解锁。
- **自动锁定延迟**：离开应用后按设定延迟重新验证。
- **防窥保护**：启用应用锁时自动开启 `FLAG_SECURE`，禁止截屏与多任务预览。
- **系统选择器豁免**：调用系统图片 / 文件选择器时不误触发锁屏。

### 🎨 外观

- **主题**：跟随系统 / 强制浅色 / 强制深色。
- **正文字体**：多种书写字体可选。
- **纸张底纹**：横线、方格、空白等信笺样式。

---

## 界面预览

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   日记流      │  │    日历       │  │   回忆       │  │   设置       │
│              │  │              │  │              │  │              │
│ ┌──────────┐ │  │  日 一 二 三 │  │  往年今日    │  │  云端与同步  │
│ │ 今日 · 卡片│ │  │  1  2  3  4 │  │  ┌────────┐ │  │  安全与隐私  │
│ └──────────┘ │  │  ...    ●    │  │  │ 日记卡片│ │  │  外观与排版  │
│ ┌──────────┐ │  │              │  │  └────────┘ │  │  数据与关于  │
│ │ 置顶日记  │ │  │  [选中日期的 │  │              │  │              │
│ └──────────┘ │  │   日记列表]  │  │              │  │              │
│              │  │              │  │              │  │              │
├──────────────┤  ├──────────────┤  ├──────────────┤  ├──────────────┤
│ 📖  🗓  🕘  ⚙️ │  │ 📖  🗓  🕘  ⚙️ │  │ 📖  🗓  🕘  ⚙️ │  │ 📖  🗓  🕘  ⚙️ │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
```

> 应用采用 2 层导航架构：底部 4 个根 Tab（日记 / 日历 / 回忆 / 设置）+ 模态推入页（编辑、搜索、统计、回收站、锁屏）。

---

## 技术栈

| 分类 | 选型 |
|---|---|
| 语言 | Kotlin 2.3.20 |
| UI | Jetpack Compose（BOM 2026.03.01）、Material 3 组件基础 |
| 架构 | MVVM + Repository，单向数据流（StateFlow） |
| 导航 | Navigation 3（`navigation3-runtime` / `navigation3-ui`） |
| 持久化 | Room 2.7.2（KSP 注解处理） |
| 异步 | Kotlin Coroutines 1.10.2、WorkManager 2.9.1 |
| 偏好存储 | DataStore Preferences 1.1.1 |
| 网络 | OkHttp 4.12.0 + kotlinx.serialization 1.7.3（Supabase REST） |
| 图片 | Coil 2.7.0 |
| 安全 | AndroidX Biometric 1.2.0 |
| 最低 / 目标版本 | minSdk 26 · targetSdk 36 · compileSdk 36 |

---

## 项目结构

```
InkPaperDiary/
├── app/src/main/java/com/example/inkpaperdiary/
│   ├── MainActivity.kt                 # 入口：DI 装配、锁屏生命周期、FLAG_SECURE
│   ├── core/
│   │   ├── designsystem/               # ★ Apple HIG 设计系统
│   │   │   ├── AppleMaterial.kt         #   5 级毛玻璃材质 + 0.5dp 发丝边框
│   │   │   ├── Color.kt / Type.kt / Shape.kt / Theme.kt
│   │   │   ├── components/              #   IosListComponents / IosSegmentedControl
│   │   │   │                            #   IosActionSheet / IosModalDialog
│   │   │   │                            #   IosDateTimePickerSheet / PaperCard ...
│   │   │   ├── interaction/             #   IosTouchPhysics.kt (iosClick 弹簧物理)
│   │   │   └── scaffold/                #   IosLargeTitleScaffold (34sp → 17sp 折叠大标题)
│   │   ├── database/                    # Room 数据库、DAO、Entity
│   │   ├── security/                    # AppLockManager / PinCipher / BiometricHelper
│   │   ├── sync/                        # SyncManager / SyncWorker
│   │   ├── backup/                      # BackupManager / TxtDiaryImporter
│   │   └── network/                     # SupabaseClient
│   ├── data/repository/                 # DiaryRepository / MediaRepository / SettingsRepository
│   ├── domain/model/                    # Diary / Tag / Mood / Weather / SyncStatus ...
│   └── ui/
│       ├── navigation/                  # AppNavigation（2 层导航）、IosTabBar、NavRoutes
│       ├── timeline/                    # 日记流
│       ├── editor/                      # 编辑器
│       ├── calendar/                    # 日历
│       ├── onthisday/                   # 回忆
│       ├── search/                      # 搜索
│       ├── stats/                       # 统计
│       ├── settings/                    # 设置
│       ├── trash/                       # 回收站
│       └── lock/                        # 锁屏
└── app/src/test/                        # 335 个 JVM 单元测试（4 层 + 对抗性）
```

---

## 设计系统速览

应用在同名 Material 组件之上，自建了一套贴合 iOS 的交互原语：

```kotlin
// 毛玻璃材质 + 发丝边框
Surface(
    color = AppleMaterials.backgroundColor(MaterialThickness.REGULAR),
    border = AppleMaterials.glassBorder(width = 0.5.dp)
) { /* ... */ }

// iOS 弹簧按压：缩放 0.97x、透明度 0.85x、触感反馈、零水波纹
Box(modifier = Modifier.iosClick { onOpen() })

// 系统分组列表
IosListSection(title = "云端与同步") {
    IosNavigationRow(title = "Supabase 凭据配置", icon = { Icon(...) }, onClick = { ... })
    IosSwitchRow(title = "自动后台同步", checked = autoSync, onCheckedChange = { ... })
}
```

| 原语 | 说明 |
|---|---|
| `AppleMaterials.glassBorder` | 0.5dp 发丝线微光玻璃边框 |
| `Modifier.iosClick` | 弹簧按压缩放 + 触感，取代 Material ripple |
| `IosListSection` / `IosListRow` | 系统 Inset Grouped 分组列表（16dp 圆角、56dp 缩进分隔线） |
| `IosSegmentedControl` | 滑动胶囊分段控件 |
| `IosActionSheet` / `IosModalDialog` | iOS 操作表与 270dp 模态弹窗 |
| `IosLargeTitleScaffold` | 滚动联动的折叠大标题 |

---

## 快速开始

### 环境要求

- Android Studio（支持 AGP 9.0.1）
- JDK 17
- Android SDK 36
- 一台 Android 8.0（API 26）及以上的设备或模拟器

### 构建与运行

```bash
# 克隆仓库
git clone https://github.com/wzhu22750-lang/Dairy.git
cd Dairy

# 编译 Debug APK
./gradlew assembleDebug

# 安装到已连接设备
./gradlew installDebug
```

生成物位于 `app/build/outputs/apk/debug/app-debug.apk`。

### 运行测试

```bash
# 全部 JVM 单元测试（无需模拟器，约 2 秒）
./gradlew test

# 查看 HTML 报告
open app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 数据与隐私

- **本地优先**：日记、标签、附件均存储在设备本地 Room 数据库；未配置 Supabase 时应用完全离线。
- **同步可选**：Supabase 地址与密钥由用户自行填写，保存在 DataStore 中；`SupabaseClient` 会对 URL 做清洗。
- **应用锁**：PIN 经 `PinCipher` 加密后存储，生物识别仅作本地校验，不上传任何凭据。
- **防截屏**：应用锁开启时自动设置 `FLAG_SECURE`。
- **可迁移**：随时导出 JSON / Markdown 备份，不锁定用户数据。

> ⚠️ 请勿将 `local.properties`、Supabase 密钥或个人日记备份提交到公开仓库。仓库中的 `.gitignore` 已忽略 `local.properties` 与构建产物。

---

## 测试体系

项目内置一套 4 层不透明盒（opaque-box）测试体系，并配有对抗性（adversarial）回归测试，累计 **335 个用例**，全部可在本地 JVM 运行、零模拟器依赖：

| 层级 | 关注点 | 用例数 |
|---|---|---|
| Tier 1 · Feature | 设计系统、导航、页面布局、业务逻辑的功能契约 | 91 |
| Tier 2 · Boundary | 边界值、极值、空值、溢出、异常恢复 | 40 |
| Tier 3 · Combination | 跨特性正交组合（TabBar + AppLock、分段控件 + 触摸物理等） | 8 |
| Tier 4 · Scenario | 多步骤真实用户旅程端到端场景 | 5 |
| Challenger · Adversarial | 对抗性回归与 Android 惯用法清除审计 | 152 |
| 其它单元测试 | 设计系统、备份、TXT 导入、领域模型、设置 VM | 39 |

详见 [`TEST_INFRA.md`](TEST_INFRA.md) 与 [`TEST_READY.md`](TEST_READY.md)，重构规划见 [`PROJECT.md`](PROJECT.md)。

---

## 路线图

- [x] Apple HIG 设计系统与交互原语
- [x] 4 Tab 底部导航与折叠大标题
- [x] 日记流 / 设置页 / 编辑器 HIG 重构
- [x] iOS 操作表、模态弹窗、日期时间选择器
- [x] 云端同步、备份导入导出
- [x] 应用锁、生物识别、防截屏
- [ ] 更多纸张底纹与字体
- [ ] 日记分享卡片导出
- [ ] 端到端 UI 测试（Compose UI Test）

---

## 贡献

欢迎提交 Issue 与 Pull Request。

1. Fork 本仓库并创建分支：`git checkout -b feature/your-feature`
2. 提交前请确保 `./gradlew test` 全部通过。
3. 提交 Pull Request，并简要说明改动内容。

---

## 许可证

本项目暂未指定开源许可证。在未获得作者明确授权前，请勿用于商业用途或二次分发。

---

<p align="center">
  用墨与纸，记录每一个值得留下的瞬间。<br>
  <sub>Made with Jetpack Compose · Designed with Apple HIG</sub>
</p>
