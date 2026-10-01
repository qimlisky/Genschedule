# 课程卡固定高光描边

用户怀疑实时高光描边导致卡顿，要求改为不受背景驱动的预制描边。

检查确认原 Kyant Default 高光没有读取背景纹理或滚动位置；它使用方向光 RuntimeShader、独立 GraphicsLayer 和 Plus 混合。已有录制缓存不代表免除 GPU Shader 和合成成本，不能据此确认它就是掉帧主因。

本次在 `glass/ui/PresetCourseCardHighlight.kt` 实现卡片局部坐标下的固定渐变描边。`drawWithCache` 缓存形状路径、画刷和描边样式，位置与背景不作为输入；尺寸、形状、强度或 Morph 几何变化时更新。不是位图烘焙，不新增离屏纹理。`GlassUi.kt` 的液态课程卡将 Kyant highlight 设为 null，在原材质入口后应用固定描边，普通与独立装饰路径均覆盖。

保留卡片形状及原描边宽度取整规则。去掉原高光的方向 Shader、柔化与独立高光层，改为固定对角渐变和 SrcOver 混合；这是用户授权的描边替换，不宣称与原效果像素等价。透明描边仍会与底色正常合成，因此“无背景输入”不等于显示像素完全不随背景改变。模糊、折射、内外阴影和底部光照未修改。

`compileGithubReleaseKotlin` 与完整 `assembleGithubRelease` 通过，耗时 3m 32s；保留 R8、资源压缩与 lintVital。本轮未新增逻辑测试，未重复运行上一轮 40 项测试；此视觉改动需要实机验收，未执行实机截图、帧采集或安装。

APK：`app/build/outputs/apk/github/release/app-github-release.apk`，2026-09-08 22:38:14，6,466,567 字节。SHA-256：`bb7bb268d5e7198b5f96b9d31392efcf2d025031d48daac2a284e98756582fc5`。包内资源、YAML、protobuf 检查无 LYIT，学校索引 150 项。无数据库或导入逻辑变更。

## 后续：新增独立的白边覆盖层（2026-09-15）

用户要求课程卡有白色、略微透明的边框。本轮**未修改上面的预制对角渐变高光**（0.045 alpha，仍只在真玻璃分支挂载）；它是用户授权的设计，未被授权删除。新增的是另一层**均匀**内描边：`CourseGlassCard` 新增 `cardOutline: Boolean = false`，为 `true` 时在 `content()` 之后插入一个覆盖层，用 `clip(shape)` + 对**内缩半个描边宽**的轮廓 `drawOutline` 绘制 1dp 白边。

放在 `content()` 的兄弟位置是为了让真玻璃、高斯模糊、纯色降级三个材质分支都不需要知道自己被描了边，符合设计系统「降级只替换材质，不改变圆角、布局」。内缩而非居中描边是必须的：周视图卡片间距只有 4dp，居中描边会把半个描边画到邻卡上。alpha 复用现有 `tokens.borderAlpha`（深色 0.24、浅色 ×0.75 = 0.18），与桌面小组件 `WidgetBackgroundRenderer` 的白边基线一致，不新造魔数。该参数默认 `false`，因此日视图、快捷菜单、课程管理与编辑器浮层逐像素不变，只有主页周视图的 4 处调用（含长按拖起的浮卡）传 `true`。

本轮的验证边界：只对改动文件跑了 kotlinc 语法检查（0 条语法错误，其余为无 classpath 导致的引用解析级联）与 `git diff` 复核，**没有**执行 `compileGithubReleaseKotlin` / `assembleGithubRelease`，也**没有**实机截图、帧采集或安装。绘制成本与是否掉帧尚未实测；新增了一层与卡片同尺寸的覆盖层，理论上只多一次描边绘制（非填充、非离屏、无 Shader）。实机验收项见 PR/报告。

## 后续变更（2026-10-01）：描边改回内缩，笔宽与 alpha 固定

`f787e1d`（同日的提交）把这段描边从内缩改成居中（`createOutline(size = size)`），笔宽 1dp→2dp，并把 `.clip(shape)` 挪到 `drawBehind` 之后，注释写作「先画描边（不被裁）」。居中的后果是 2dp 里有 1dp 落在卡片**外面**：拖动浮卡没有祖先裁切，显示完整 2dp；而周视图静态卡的祖先节点带 `WeekScheduleUi.kt` 的 `clipToBounds()`（矩形裁切），落在卡外的半边被切掉，四条直边只剩半条、四个圆角仍是满宽。同一段代码在两类卡上观感因此不一致：静态卡那条粗细不均、发虚，浮卡那条干净完整。

修复即回到本文档原本的「内缩半个描边宽」：`translate(w / 2f, w / 2f)` + `createOutline(size = Size(size.width - w, size.height - w))`。笔宽与 alpha 保留 `f787e1d` 之后的 2dp / 0.6f（用户明确要更粗更亮的白边），不退回 1dp / `tokens.borderAlpha`。整条落在卡内后，矩形与形状裁切都切不到它，四处 `cardOutline = true`（周视图静态卡、长按拖起的浮卡、两张冲突卡）呈现同一条线。

本轮的验证边界：只对 `GlassUi.kt` 单文件跑了 kotlinc 解析检查（无 classpath，输出全是 `Unresolved reference` 级联，无语法错误）与 `git diff` 复核，**没有**执行 `compileGithubReleaseKotlin` / `assembleGithubRelease`，也**没有**实机截图。裁切机制是按代码路径推导的（`WeekScheduleUi.kt` 静态卡的祖先 `clipToBounds`），尚未用实机像素复核。
