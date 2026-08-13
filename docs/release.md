# 发布指南

## 版本规则

项目版本只在 `gradle.properties` 的 `mod_version` 中维护，文档和脚本示例不复制当前版本号。版本采用 SemVer 风格：

- `alpha`：功能可验收，但兼容面和交互仍可能变化；
- `beta`：支持范围冻结，集中修复缺陷；
- 稳定版：兼容矩阵、迁移与回退路径均完成发布审计。

Git tag 使用 `v<version>`。tag 中去掉前导 `v` 后必须与准备发布的版本一致；CI 会用 tag 版本覆盖 Gradle 的构建版本。

## 发布前检查

1. 更新 `CHANGELOG.md`，把 Unreleased 内容整理为本次版本。
2. 核对 README 兼容矩阵和 `gradle.properties`。
3. 重新审计 Yuushya 目标版本的 Screen、TransformData、BlockShape 和 packet 类。
4. 核对 `modbench_version` 仍指向已通过发布审计的不可变 JitPack tag。
5. 执行：

```shell
./gradlew eclipseClasspath verifyIdeClasspath --no-daemon
./gradlew releaseBuild --no-build-cache --no-configuration-cache --no-daemon
./gradlew verifyYuushyaEditorBench -PenableModBench=true --no-daemon
```

6. 检查 `build.nosync/libs/` 只有本项目生产 Jar，且 `verifyProductionJar` 已确认没有 `com/yuushya/`、ModBench 或冲突 class。

## CI

`.github/workflows/ci.yml` 在以下情况运行：向 `master` 推送、向 `master` 或 `dev` 提交 Pull Request、推送 `v*` tag，以及手动触发。普通分支推送不会单独运行，从而避免和 Pull Request 重复验证。

生产构建会：

1. 检出本项目并设置 Java 25；
2. 校验 IDE classpath、测试、翻译、宿主 Jar 和生产 Jar；
3. 从 `build.nosync/libs/` 复制唯一生产 Jar；
4. 生成统一文件名和 `.sha256`；
5. 上传 `mod-build` Artifact。

文件名格式：

```text
yuushya_modelling_enhanced_editor-<version>+mc26.1.2-neoforge.jar
```

真实客户端 Bench 只在推送 `v*` tag 时运行，并在需要时通过 JitPack 加载固定版本的 BenchMod 插件、API 和 Runtime。生产构建通过后才启动 Bench，Bench 也通过后才创建 GitHub Release；构建失败时不会浪费客户端验收资源。Bench 报告和截图无论成功失败都会上传。普通 Pull Request 和 `master` 推送只运行生产构建。

自动发布只把带 `-alpha` 后缀的 tag 标记为 prerelease；带 `-beta`、`-rc` 或不带阶段后缀的 tag 均创建正常 GitHub Release。

## 发布 tag

只在本地和 CI 均通过后创建 tag：

```shell
VERSION="$(sed -n 's/^mod_version=//p' gradle.properties)"
test -n "${VERSION}"
git tag -a "v${VERSION}" -m "Yuushya Modelling Enhanced Editor v${VERSION}"
git push origin "v${VERSION}"
```

等待 Build & Release 工作完成，随后核对 GitHub Release：

- Jar 名称和版本正确；
- `.sha256` 中的文件名与 Jar 一致；
- 下载后 `sha256sum -c` 或 `shasum -a 256 -c` 能通过；
- Release 说明与 CHANGELOG 相符。

不要手工上传 `libs/yuushya_modelling-*.jar`，也不要发布 `src/bench` 或 ModBench Runtime 产生的 Jar。

## 回滚

发现会写坏模型、提交错误数据包或导致客户端崩溃的问题时，先把对应 GitHub Release 标为预发布或撤下附件，并在 README/CHANGELOG 标明受影响版本。修复应发布新版本；不要移动已经公开的 tag 指向。
