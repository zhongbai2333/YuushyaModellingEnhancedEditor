# Yuushya Modelling Enhanced Editor

一个面向 **Minecraft 26.1.2 / NeoForge** 的客户端增强 Mod，为 Yuushya Modelling 的实体方块、物品方块和文本方块提供现代化场景编辑器。

> 当前版本：`0.1.0-alpha`。核心编辑、保存与真实客户端验收已经可用，但升级 Yuushya Modelling 前仍需重新做兼容性审计。

## 它解决什么问题

本项目把 [NetMusicCanPlayBili](https://github.com/zhongbai2333/NetMusicCanPlayBili) 中成熟的三维编辑器交互与渲染思路提取出来，适配到 [Yuushya Modelling](https://github.com/Crystal1921/Yuushya-Modelling) 26.1 的原有数据和网络协议上。

它不会复制或接管 Yuushya 的方块实体、存档格式和服务端协议。增强编辑器只维护临时草稿，保存时仍由 Yuushya 原有数据结构和数据包提交；兼容绑定失败时会保留原编辑器作为回退。

主要能力包括：

- NCPB 风格黑金三栏界面、场景层级、检查器、透视/正交相机和六向控制手柄；
- 方块、物品、文本三类建模，以及带实时预览的注册表选择器；
- 移动、三轴旋转和 Yuushya 兼容的整体缩放，支持三档吸附精度；
- 删除、显隐、撤销/重做、碰撞箱预设和退出自动保存；
- 保存前检测潜在 Z-fighting，可用 `0.0000001` 格的可撤销偏移自动优化；
- 以当前模型为中心的直径 25 球形环境预览，外圈使用稳定散布；
- 根据距离和像素密度调整线宽，控制手柄会自动避开大型模型；
- 分段异步环境网格、GPU 缓存、模型缓存、视锥裁剪和局部失效更新。

## 兼容性

| 组件 | 支持版本 | 安装位置 |
| --- | --- | --- |
| Minecraft | `26.1.2` | 客户端与服务端 |
| NeoForge | `26.1.2.76` | 客户端与服务端 |
| Yuushya Modelling | `2.4.2` | 客户端与服务端 |
| Enhanced Editor | `0.1.0-alpha` | 只需客户端 |
| Java | `25` | 启动开发环境、构建和测试 |

Yuushya Townscape 的 Mod ID 是 `yuushya`，Yuushya Modelling 的 Mod ID 是 `yuushya_modelling`；在已审计的 26.1 分支和 26.1.2 客户端中它们仍是两个独立 Mod。本项目依赖后者，Townscape 只是可选内容，不包含这里需要的建模界面类。

## 安装

1. 安装 Minecraft `26.1.2` 和 NeoForge `26.1.2.76`。
2. 客户端 `mods` 目录同时放入 Yuushya Modelling `2.4.2` 与本项目发布 Jar。
3. 联机时，服务端也必须安装 Yuushya Modelling；服务端不需要本增强编辑器。
4. 不要把仓库内 `libs/` 的审计开发 Jar 当作本项目发布物，也不要把它与增强编辑器 Jar 合并。

更完整的操作流程、快捷键和排错方式见 [用户指南](docs/user-guide.md)。

## 快速使用

使用 Yuushya Modelling 的“实体方块界面终端”右键实体方块、物品方块或文本方块。兼容检查通过后，原界面会被增强编辑器接替。

- 左侧管理图层并添加/删除内容；中间视口负责选择和 Gizmo 操作；右侧可精确输入变换值。
- `W` / `E` / `R` 切换移动、旋转、整体缩放。
- 默认移动/缩放步长为 `0.1`，按住 `Shift` 为 `0.05`，按住 `Ctrl` 为 `0.001`。
- 默认旋转步长为 `15°`，按住 `Shift` 为 `5°`，按住 `Ctrl` 为 `0.001°`。
- `Delete` 或 `Backspace` 删除当前层，`Ctrl+Z` / `Ctrl+Y` 撤销/重做。
- 正常关闭、按 Esc 或点击“保存并关闭”都会自动保存；只有“放弃更改”不会提交草稿。

整体缩放只有一个控制值，这是 Yuushya `ShowBlockScreen` 的现有契约，不是遗漏的单轴缩放。拖动时 X/Y/Z 会写入相同值，并按旧/新缩放比补偿位置，避免可见枢轴跳动。

## 当前验证状态

- 55 个单元测试通过；
- 干净发布构建会校验 Yuushya 开发 Jar 的版本与 SHA-256，并拒绝混入 Bench、Yuushya 本体或冲突 class 的制品；
- 真实 Minecraft + NeoForge + Yuushya + ModBench 客户端场景通过，覆盖三类编辑器、自动保存、删除、碰撞箱、Z-fighting、环境渲染和局部缓存失效；
- 最近一次测量阶段帧间隔均值约 `4.18 ms`、P95 约 `6.99 ms`；环境采集最大切片约 `3.17 ms`，单 Tick 最大约 `4.85 ms`，均低于 `10 ms` / `12 ms` 硬门槛。

性能数字只描述该次本机验收，不是所有硬件的帧率承诺。完整验收边界见 [项目交接文档](docs/project-handoff.md)。

## 开发

```shell
./gradlew eclipseClasspath verifyIdeClasspath
./gradlew test
./gradlew releaseBuild --no-build-cache --no-configuration-cache --no-daemon
./gradlew runClient
./gradlew verifyYuushyaEditorBench --no-daemon
```

ModBench Gradle 插件和运行时固定使用 [BenchMod 0.1.1](https://github.com/zhongbai2333/BenchMod/tree/0.1.1)，
由 JitPack 自动解析，无需预先克隆或发布到 Maven Local。

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

- 编辑器交互模型提取并改编自 NCPB；其首个共享 editor-core 实现在提交 `be964ac` 中引入。
- Yuushya 适配审计锚点是 `Yuushya-Modelling` 的 `26.1` 分支提交 `57f4d86407510f76191e35c280d8e1c8d347bf42`。
- Yuushya Townscape 26.1 的独立性检查锚点为 `3478bb99b3a1b2bb7df7dd8a974364c12e49f410`。

本项目使用 [MIT License](LICENSE)。Yuushya Modelling、Yuushya Townscape、Minecraft、NeoForge 和其他依赖仍归各自项目及权利人所有，并遵循各自许可证。
