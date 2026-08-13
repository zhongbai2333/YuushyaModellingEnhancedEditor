# Yuushya Modelling Enhanced Editor

一个面向 **Minecraft 26.1.2 / NeoForge** 的客户端增强 Mod，为 Yuushya Modelling 的实体方块、物品方块和文本方块提供现代化场景编辑器。

项目已进入 beta 测试阶段。核心编辑、保存和真实客户端验收已经可用；兼容范围、数据安全和交互回归仍以发布前审计结果为准。

## 它解决什么问题

本项目把 [NetMusicCanPlayBili](https://github.com/zhongbai2333/NetMusicCanPlayBili) 中成熟的三维编辑器交互与渲染思路提取出来，适配到 [Yuushya Modelling](https://github.com/Crystal1921/Yuushya-Modelling) 26.1 的原有数据和网络协议上。

它不会复制或接管 Yuushya 的方块实体、存档格式和服务端协议。增强编辑器只维护临时草稿，保存时仍由 Yuushya 原有数据结构和数据包提交；兼容绑定失败时会保留原编辑器作为回退。

主要能力包括：

- NCPB 风格黑金三栏界面、场景层级、检查器、透视/正交相机和六向控制手柄；
- 方块、物品、文本三类建模，以及带实时预览的注册表选择器；物品层可编辑颜色、切换物品/方块渲染并逐项调整方块状态；
- 移动、三轴旋转和 Yuushya 原生 X/Y/Z 独立缩放，支持三档吸附精度及多选组变换；
- 图层多选、批量复制/粘贴、删除、显隐、撤销/重做、碰撞箱预设和退出自动保存；
- 直接复用 Yuushya 原版剪贴板格式导入/导出方块、物品和文本模型；
- 保存前按方块外轮廓体素面检测潜在 Z-fighting，可用递增的 `0.0001` 格可撤销微偏移自动优化；
- 以当前模型为中心的直径 25 球形环境预览，周边 Yuushya 方块/物品/文本模型也会显示；
- 中心建模单元格常驻线框，初始摄像机保留进入前相对方块的位置、距离、方向和 FOV；
- 根据距离和像素密度调整线宽，控制手柄会自动避开大型模型；
- 分段异步环境网格、GPU 缓存、模型缓存、视锥裁剪和局部失效更新。

## 兼容性

下表区分发布元数据允许的范围与实际完成过兼容审计的基线。超出审计基线不代表一定不可用，但发布前必须重新验证 Yuushya 私有界面字段、数据结构和网络协议。

| 组件 | 发布范围 | 审计基线 | 安装位置 |
| --- | --- | --- | --- |
| Minecraft | `[26.1.2,27)` | `26.1.2` | 客户端与服务端 |
| NeoForge | `[26,)` | `26.1.2.76` | 客户端与服务端 |
| Yuushya Modelling | `[2.4.2]` | `2.4.2` | 客户端与服务端 |
| Scene Editor Core | Gradle 中声明的兼容区间 | 当前构建版本 | 由本项目 JiJ，无需单独安装 |
| Java | `25` | `25` | 开发、构建和测试 |

Yuushya Townscape 的 Mod ID 是 `yuushya`，Yuushya Modelling 的 Mod ID 是 `yuushya_modelling`；在已审计的 26.1 分支和 26.1.2 客户端中它们仍是两个独立 Mod。本项目依赖后者，Townscape 只是可选内容，不包含这里需要的建模界面类。

## 安装

1. 安装 Minecraft `26.1.2` 和 NeoForge `26.1.2.76`。
2. 客户端 `mods` 目录同时放入 Yuushya Modelling `2.4.2` 与本项目发布 Jar。
3. 联机时，服务端也必须安装 Yuushya Modelling；服务端不需要本增强编辑器。
4. Scene Editor Core 已包含在本项目发布 Jar 中，不需要额外下载；不要把仓库内 `libs/` 的审计开发 Jar 当作发布物。

更完整的操作流程、快捷键和排错方式见 [用户指南](docs/user-guide.md)。

## 快速使用

使用 Yuushya Modelling 的“实体方块界面终端”右键实体方块、物品方块或文本方块。兼容检查通过后，原界面会被增强编辑器接替。

- 左侧管理图层并添加/删除内容；中间视口负责选择和 Gizmo 操作；右侧可精确输入变换值。
- `W` / `E` / `R` 切换移动、旋转、三轴缩放。
- 默认移动/缩放步长为 `0.1`，按住 `Shift` 为 `0.05`，按住 `Ctrl` 为 `0.001`。
- 默认旋转步长为 `15°`，按住 `Shift` 为 `5°`，按住 `Ctrl` 为 `0.001°`。
- `Ctrl` + 点击增减多选、`Shift` + 点击范围选择、`Ctrl+A` 全选；Gizmo 和检查器会同时变换选中层。
- `Delete` 或 `Backspace` 删除选中层，`Ctrl+Z` / `Ctrl+Y` 撤销/重做。
- 每个模型保留 128 步；保存关闭后本次游戏会话内仍可恢复，后台池最多缓存 8 个模型、总计 512 步并在 30 分钟后过期。
- `Ctrl+C` / `Ctrl+V` 或左侧按钮批量复制/粘贴选中图层；右侧“原版导出/导入”使用系统剪贴板交换整套模型。
- 正常关闭、按 Esc 或点击“保存并关闭”都会自动保存；只有“放弃更改”不会提交草稿。

缩放面板分别提供 X/Y/Z 数值，缩放 Gizmo 的红、绿、蓝手柄分别修改对应轴。数据直接写入 Yuushya 原生 `SCALE_X / SCALE_Y / SCALE_Z`，不引入私有存档字段或额外网络协议；修改时会逐轴补偿位置，避免可见枢轴跳动。

## 验证状态

- 完整单元测试通过；
- 干净发布构建会校验 Yuushya 开发 Jar 的版本与 SHA-256、Scene Editor JiJ 版本区间和嵌套 API，并拒绝混入 Bench、Yuushya 本体、已迁出的编辑器副本或冲突 class；
- 真实 Minecraft + NeoForge + Yuushya + ModBench 客户端场景通过，覆盖三类编辑器、文本多行编辑/新增/背面选项、物品方块状态与颜色回写、检查器及 Gizmo 的逐轴缩放隔离、自动保存、删除、碰撞箱、Z-fighting、环境渲染和局部缓存失效；
- Bench 对环境采集切片、单 Tick 开销、渲染缓存复用和帧间隔设置硬门槛；具体测量值保存在每次运行生成的报告中，不在文档中固化单机结果。

完整验收边界和报告位置见 [项目交接文档](docs/project-handoff.md)。

## 开发

```shell
./gradlew eclipseClasspath verifyIdeClasspath
./gradlew test
./gradlew releaseBuild --no-build-cache --no-configuration-cache --no-daemon
./gradlew runClient
./gradlew verifyYuushyaEditorBench -PenableModBench=true --no-daemon
```

ModBench Gradle 插件和运行时固定使用 [BenchMod 0.1.2](https://github.com/zhongbai2333/BenchMod/tree/0.1.2)，
由 JitPack 自动解析，无需预先克隆或发布到 Maven Local。插件默认不参与普通 IDE/Gradle 导入，
仅在显式传入 `-PenableModBench=true` 时加载。

开发环境、VS Code 修复和 Gradle 任务详见 [开发指南](docs/development.md)；发版规则见 [发布指南](docs/release.md)。

## 文档

- [用户指南](docs/user-guide.md)
- [开发指南](docs/development.md)
- [发布指南](docs/release.md)
- [架构与项目交接](docs/project-handoff.md)
- [贡献指南](CONTRIBUTING.md)
- [变更记录](CHANGELOG.md)
- [安全策略](SECURITY.md)

## 上游与许可证

- 通用相机、投影、选择、Gizmo 和历史能力来自独立的 [SceneEditor](https://github.com/zhongbai2333/SceneEditor)，并以 JiJ 方式随本项目发布；Yuushya 专用数据、渲染和保存适配仍由本项目维护。
- SceneEditor 的交互模型提取并改编自 NCPB；其首个共享 editor-core 实现在提交 `be964ac` 中引入。
- Yuushya 适配审计锚点是 `Yuushya-Modelling` 的 `26.1` 分支提交 `57f4d86407510f76191e35c280d8e1c8d347bf42`。
- Yuushya Townscape 26.1 的独立性检查锚点为 `3478bb99b3a1b2bb7df7dd8a974364c12e49f410`。

本项目使用 [MIT License](LICENSE)。Yuushya Modelling、Yuushya Townscape、Minecraft、NeoForge 和其他依赖仍归各自项目及权利人所有，并遵循各自许可证。
