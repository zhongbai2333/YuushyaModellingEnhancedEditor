# 开发指南

## 环境

- JDK 25；项目 Java toolchain 固定为 25。
- Gradle Wrapper 9.5.0。
- Minecraft 26.1.2、NeoForge 26.1.2.76。
- 仓库内审计过的 Yuushya Modelling 2.4.2 开发 Jar。
- 本地发布的 BenchMod `0.1.0-SNAPSHOT` Gradle 插件和 API。

先准备 BenchMod：

```shell
git clone https://github.com/zhongbai2333/BenchMod.git ../BenchMod
../BenchMod/gradlew -p ../BenchMod publishToMavenLocal --no-daemon
```

然后初始化项目：

```shell
./gradlew eclipseClasspath verifyIdeClasspath --no-daemon
./gradlew test --no-daemon
```

## VS Code / Code Insiders

仓库提交 `.vscode/settings.json` 和可移植的 `.vscode/tasks.json`，但忽略 NeoForge 自动生成且包含本机绝对路径的 `.vscode/launch.json`。

本项目的 ModBench Gradle 插件要求 Java 25。当前 Java 扩展的 Gradle Build Server 可能在语言服务器切换运行时前用内置 Java 21 导入项目，导致插件解析失败；失败后 JDT 又可能保留旧 `build/moddev` 路径，而项目输出实际位于 `build.nosync/moddev`。工作区配置因此：

- 用 `java.jdt.ls.java.home` 从 Java 25 启动语言服务器；
- 用 `java.import.gradle.java.home` 让 Gradle 导入使用 Java 25；
- 关闭 `java.gradle.buildServer.enabled`，由 Buildship 导入 Gradle 模型；
- 自动更新构建配置，并生成根目录 Eclipse metadata。

配置中的 Zulu 25 路径是当前 macOS 开发环境的安装路径。如果 JDK 安装在其他位置，请在 VS Code 用户设置中用本机路径覆盖 `java.jdt.ls.java.home`、`java.import.gradle.java.home` 和 JavaSE-25 runtime，不要提交个人路径变化。

首次使用或曾看到 `Screen cannot be resolved` 时：

1. 执行 VS Code 任务 `NeoForge: sync IDE classpath`，或运行 `./gradlew eclipseClasspath verifyIdeClasspath`；
2. 从命令面板执行 `Java: Clean Java Language Server Workspace`；
3. 选择重启并重新载入窗口，等待 Gradle 导入完成。

`verifyIdeClasspath` 会检查 Java 25、main/test/bench 源集、Yuushya Jar、当前 `build.nosync` Minecraft patched Jar 及其中的 `Screen.class`，并拒绝旧 `build/moddev` 路径。

## 常用 Gradle 任务

| 任务 | 用途 |
| --- | --- |
| `test` | 执行 host-neutral JUnit 测试 |
| `build` | 编译、测试并打包 |
| `releaseBuild` | clean 后禁用陈旧编译缓存并完成发布校验 |
| `verifyYuushyaRuntime` | 校验宿主 Jar 版本、类集合和 SHA-256 |
| `verifyProductionJar` | 拒绝 Bench、Yuushya 本体和冲突 class 混入成品 |
| `verifyLanguageKeys` | 要求中英翻译 key 完全一致 |
| `eclipseClasspath` | 生成 JDT/Buildship 可导入的 `.classpath` |
| `verifyIdeClasspath` | 校验 IDE classpath 完整性和实际 Jar 内容 |
| `runClient` | 启动包含 Yuushya 2.4.2 的开发客户端 |
| `verifyYuushyaEditorBench` | 启动并验证真实集成客户端场景 |

发布构建建议始终运行：

```shell
./gradlew releaseBuild --no-build-cache --no-configuration-cache --no-daemon
```

输出位于 `build.nosync/libs/`。`releaseBuild` 同时删除项目迁移前的 `build/`，避免文件提供器恢复旧 class。

## 依赖边界

`libs/yuushya_modelling-26.1.2-2.4.2.jar` 只声明为 `runtimeOnly` 和 `benchImplementation`，绝不能 shade 进生产 Jar。它的审计信息见 [libs/README.md](../libs/README.md)。测试其他兼容构建时必须同时覆盖路径和已核实的 SHA-256：

```shell
./gradlew runClient \
  -Pyuushya_runtime_jar=/absolute/path/to/yuushya_modelling.jar \
  -Pyuushya_runtime_sha256=<lowercase-sha256>
```

只换路径而不换摘要会故意失败。

## 代码结构

- `core/`：不依赖 Minecraft 的场景、相机、投影、Gizmo、历史和几何检测。
- `client/`：NeoForge Screen、选择器、环境捕获和渲染。
- `compat/`：Yuushya 2.4.2 反射绑定、数据转换、验证和数据包提交。
- `mixin/`：客户端世界更新的局部环境失效通知。
- `src/bench/`：只存在于验收运行中的真实客户端 Provider。
- `src/test/`：host-neutral 单元测试。

任何兼容层失败都必须保留原 Yuushya Screen，不能直接写方块实体 NBT，也不能引入第二套协议。

## 测试策略

改动纯数学、历史、保存决策或兼容探测时添加 JUnit。改动 Screen、输入、渲染、数据包或环境缓存时，还应扩展 ModBench 场景。

Bench 验证报告位于：

```text
build.nosync/modBench/raw-results/default/client/summary.json
build.nosync/modBench/bundles/default/client/
```

验收必须同时确认场景为 `PASSED`、预期 Mod ID 全部加载、必需截图和诊断文件存在，并满足环境采集切片/Tick 的性能门槛。
