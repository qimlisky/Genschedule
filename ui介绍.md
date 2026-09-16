从这份文件树看，UI 并没有集中在一个目录，而是分散在 **Activity/WidgetProvider、`app/ui`、`core/ui`、`feature/*Ui.kt`、`glass/ui`、`transition/legacy`** 里。  
Activity 多数只是“壳”，真正绘制界面的是对应的 `*Ui.kt`、`*Overlay.kt`、`*Dialog.kt`、`*Sheet.kt`、`*Provider.kt`、`glass/ui` 和 `transition` 里的动画类。

下面按“谁管 UI”和“绘制什么 UI”分类说明。

---

## 1. 根目录：Activity 与桌面小组件 Provider

这些文件直接承载页面或桌面小组件，属于 UI 入口层。

| 文件 | 绘制/承载的 UI |
|---|---|
| `MainActivity.kt` | 应用主界面入口 |
| `CourseManagementActivity.kt` | 课程管理页 |
| `CourseManagementDetailActivity.kt` | 课程管理详情页 |
| `ScheduleManagerActivity.kt` | 课表管理页 |
| `SettingsDetailActivity.kt` | 设置详情页 |
| `EduImportActivity.kt` | 教育系统导入页 |
| `EduSchoolSelectActivity.kt` | 学校选择页 |
| `AiEduImportProgressActivity.kt` | AI/教育导入进度页 |
| `AiImportHistoryActivity.kt` | AI 导入历史页 |
| `AiImportHistoryDetailActivity.kt` | AI 导入历史详情页 |
| `OplusCourseManagementActivity.kt` | OPPO/一加/realme 变体的课程管理页 |
| `OplusCourseManagementDetailActivity.kt` | 厂商变体课程管理详情页 |
| `OplusEduSchoolSelectActivity.kt` | 厂商变体学校选择页 |
| `OplusAiImportHistoryActivity.kt` | 厂商变体 AI 导入历史页 |
| `TodayAssistantWidgetProvider.kt` | 今日助手桌面小组件 |
| `TodayCoursesWidgetProvider.kt` | 今日课程桌面小组件 |
| `TodayCoursesSquareWidgetProvider.kt` | 今日课程方形小组件 |
| `TodayTomorrowWidgetProvider.kt` | 今天/明天桌面小组件 |
| `WeekScheduleWidgetProvider.kt` | 周课表桌面小组件 |

这些 Activity 通常只负责生命周期、路由、状态接收，真正 Compose 内容在对应的 `*Ui.kt` 中。

---

## 2. `app/ui`：应用顶层 UI

| 文件 | 绘制内容 |
|---|---|
| `ScheduleAppUi.kt` | 应用顶层 Compose UI，组织主界面结构 |
| `AppCommonControlPreviews.kt` | 通用控件预览 |

`app` 下还有 `state/ScheduleViewModel.kt`、`startup/StartupEntrance.kt`、`config/SleepDownRemoteConfig.kt`，这些不是直接画 UI，而是给 UI 提供状态和启动逻辑。

---

## 3. `core/ui`：通用设计系统与基础 UI 组件

这是整个应用的 UI 基础设施层。

### `core/ui/designsystem`

| 文件 | 绘制内容 |
|---|---|
| `ContinuousCorners.kt` | 连续圆角形状 |
| `PreviewFrame.kt` | Compose 预览框架 |
| `SleepDownDesignTokens.kt` | 设计令牌：颜色、间距、圆角、字体等 |
| `SleepDownDialog.kt` | 通用对话框 |
| `SleepDownDialogPreviews.kt` | 对话框预览 |
| `SleepDownQuickSheet.kt` | 快捷底部面板 |
| `SleepDownQuickSheetPreviews.kt` | 快捷面板预览 |
| `SleepDownSecondaryPage.kt` | 二级页面通用容器 |
| `SleepDownSecondaryPagePreviews.kt` | 二级页面预览 |

### `core/ui/interaction`

| 文件 | 绘制/处理内容 |
|---|---|
| `MiuixHapticOverscroll.kt` | Miuix 风格震动反馈与过度滚动交互 |

### `core/ui/settings`

| 文件 | 绘制内容 |
|---|---|
| `GlassMiuixPopup.kt` | 玻璃风格 Miuix 弹窗 |
| `GlassMiuixSettings.kt` | 玻璃风格设置组件 |

### `core/ui/text`

| 文件 | 绘制内容 |
|---|---|
| `AutoFitText.kt` | 自适应大小文本 |
| `AutoFitTextPreviews.kt` | 自适应文本预览 |

---

## 4. `feature` 下：各业务页面 UI

`feature` 是 UI 最集中的地方，按业务模块划分。

### 4.1 AI Agent

| 文件 | 绘制内容 |
|---|---|
| `feature/agent/DayAgentUi.kt` | AI 日助理聊天/交互界面 |
| `feature/agent/AgentMarkdown.kt` | Agent 消息 Markdown 渲染 |

### 4.2 备份与恢复

| 文件 | 绘制内容 |
|---|---|
| `feature/backup/BackupRestoreSettingsUi.kt` | 备份恢复设置界面 |
| `feature/backup/BackupPreview.kt` | 备份内容预览 UI |

### 4.3 课程编辑与管理

| 文件 | 绘制内容 |
|---|---|
| `feature/course/editor/CourseEditorUi.kt` | 课程编辑主界面 |
| `feature/course/editor/CourseColorPicker.kt` | 课程颜色选择器 |
| `feature/course/editor/CourseEditorContainerOverlay.kt` | 课程编辑容器覆盖层 |
| `feature/course/management/CourseManagementUi.kt` | 课程管理界面 |

### 4.4 首页/日视图/周视图

| 文件 | 绘制内容 |
|---|---|
| `feature/home/day/HomeScheduleUi.kt` | 首页日课表 UI |
| `feature/home/week/WeekScheduleUi.kt` | 周课表 UI |
| `feature/home/HomeJumpWeekDialog.kt` | 跳转周对话框 |
| `feature/home/PersonalizationPreview.kt` | 个性化预览 |
| `feature/home/WallpaperEditorOverlay.kt` | 壁纸编辑覆盖层 |
| `feature/home/overlay/CopiedCourseEditorOverlay.kt` | 复制课程编辑覆盖层 |
| `feature/home/overlay/CourseShortcutOverlay.kt` | 课程快捷操作覆盖层 |
| `feature/home/overlay/HomeAnchoredMorphOverlay.kt` | 首页锚定形变覆盖层 |
| `feature/home/overlay/HomeMenuDestinationOverlay.kt` | 首页菜单目标覆盖层 |

### 4.5 AI 导入、教育导入、ICS 导入

| 文件 | 绘制内容 |
|---|---|
| `feature/importing/AiManualImportUi.kt` | AI 手动导入界面 |
| `feature/importing/ConfirmScheduleUi.kt` | 确认导入课表界面 |
| `feature/importing/DonateUi.kt` | 捐赠界面 |
| `feature/importing/EduImportBrowserUi.kt` | 教育系统导入浏览器界面 |
| `feature/importing/EduSchoolSelectionUi.kt` | 学校选择界面 |
| `feature/importing/history/AiImportHistoryUi.kt` | AI 导入历史列表 |
| `feature/importing/history/AiImportHistoryDetailUi.kt` | AI 导入历史详情 |
| `feature/importing/progress/AiEduImportProgressUi.kt` | AI/教育导入进度界面 |

### 4.6 课表管理与选择器

| 文件 | 绘制内容 |
|---|---|
| `feature/schedule/manager/ScheduleManagerUi.kt` | 课表管理界面 |
| `feature/schedule/picker/SchedulePickerOverlay.kt` | 课表选择器覆盖层 |

### 4.7 设置

| 文件 | 绘制内容 |
|---|---|
| `feature/settings/SettingsScreensUi.kt` | 设置页面集合 |
| `feature/settings/SettingsRowsUi.kt` | 设置行组件 |
| `feature/settings/SettingsRowsPreviews.kt` | 设置行预览 |
| `feature/settings/ScheduleSettingsContentUi.kt` | 课表设置内容 |
| `feature/settings/ScheduleConfigScreenUi.kt` | 课表配置页面 |
| `feature/settings/PeriodSchemeEditorUi.kt` | 节次方案编辑器 |
| `feature/settings/LiquidGlassSettingsUi.kt` | 液态玻璃设置界面 |
| `feature/settings/LegalAndDonationUi.kt` | 法律信息与捐赠界面 |

### 4.8 桌面小组件

| 文件 | 绘制内容 |
|---|---|
| `feature/widget/WidgetCustomizationUi.kt` | 小组件自定义界面 |
| `feature/widget/providers/ExpandedScheduleWidgetProviders.kt` | 扩展课表小组件 |
| `feature/widget/providers/MiuixTodayWidgetProviders.kt` | Miuix 风格今日小组件 |

根目录的 `Today*WidgetProvider` 和 `WeekScheduleWidgetProvider` 也属于这一层。

---

## 5. `glass/ui`：玻璃拟态 UI

这是自研玻璃视觉效果层，负责绘制毛玻璃、模糊、透镜、背景缩放等。

| 文件 | 绘制内容 |
|---|---|
| `glass/ui/AdaptiveGlass.kt` | 自适应玻璃组件 |
| `glass/ui/GlassUi.kt` | 通用玻璃 UI |
| `glass/ui/PresetCourseCardHighlight.kt` | 预设课程卡片高光 |
| `glass/ui/ProgressiveBlur.kt` | 渐进模糊 |
| `glass/ui/RenderEffectCompat.kt` | RenderEffect 兼容渲染 |
| `glass/ui/ScaledBackdrop.kt` | 缩放背景 |

`glass` 根目录下的 `LiquidMorph.kt`、`GlassSampling.kt`、`GlassOcclusion.kt`、`SharedCourseBackdrop.kt`、`SleepDownGlassSurface.kt` 等，主要是玻璃渲染引擎和场景状态，不一定直接画页面，但支撑玻璃 UI。

---

## 6. `transition`：转场与形变动画

这些文件不直接绘制静态页面，但控制 Activity 之间、详情页展开、形变过渡等 UI 动画。

| 文件 | 作用 |
|---|---|
| `transition/ActivityTransitionCoordinator.kt` | Activity 转场协调 |
| `transition/CrossActivityTransitionHost.kt` | 跨 Activity 转场宿主 |
| `transition/LegacyTransitionBackend.kt` | 旧版转场后端 |
| `transition/OplusSeamlessBackend.kt` | OPPO/一加无缝转场 |
| `transition/TransitionBackend.kt` | 转场后端抽象 |
| `transition/TransitionIntent.kt` | 转场意图 |
| `transition/TransitionPayload.kt` | 转场数据 |
| `transition/TransitionRoute.kt` | 转场路由 |
| `transition/TransitionSession.kt` | 转场会话 |
| `transition/legacy/ActivityTransitions.kt` | 旧版 Activity 转场 |
| `transition/legacy/AnchoredDetailActivityMorph.kt` | 锚定详情形变 |
| `transition/legacy/DetailMorphOverlay.kt` | 详情形变覆盖层 |
| `transition/legacy/LegacyLiquidMorphSpecs.kt` | 旧版液态形变规格 |
| `transition/legacy/MorphSnapshotBackground.kt` | 形变快照背景 |

---

## 7. 壁纸与个性化相关 UI

| 文件 | 绘制/支持内容 |
|---|---|
| `core/wallpaper/FocusCroppedWallpaper.kt` | 焦点裁剪壁纸 |
| `core/wallpaper/WallpaperCropMath.kt` | 壁纸裁剪计算 |
| `core/wallpaper/WallpaperImageStore.kt` | 壁纸图片存储 |
| `feature/home/WallpaperEditorOverlay.kt` | 壁纸编辑覆盖层 |
| `feature/home/PersonalizationPreview.kt` | 个性化预览 |

---

## 8. 总结：UI 主要分布

真正“管 UI”的文件主要集中在这些地方：

1. **页面入口**：根目录 `*Activity.kt`
2. **顶层 UI**：`app/ui/ScheduleAppUi.kt`
3. **通用设计系统**：`core/ui/designsystem/*`
4. **业务页面**：`feature/**/*Ui.kt`、`*Overlay.kt`、`*Dialog.kt`、`*Sheet.kt`
5. **桌面小组件**：根目录 `*WidgetProvider.kt`、`feature/widget/**`
6. **玻璃拟态**：`glass/ui/*`
7. **转场动画**：`transition/**`、`transition/legacy/**`
8. **壁纸个性化**：`core/wallpaper/*`、`feature/home/WallpaperEditorOverlay.kt`

简单说：  
**页面壳在 Activity，页面内容在 `feature/*Ui.kt`，通用组件在 `core/ui`，玻璃效果在 `glass/ui`，转场动画在 `transition`，桌面小组件在 `WidgetProvider` 和 `feature/widget`。**