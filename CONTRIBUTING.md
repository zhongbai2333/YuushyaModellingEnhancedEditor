# 贡献指南

感谢参与 Yuushya Modelling Enhanced Editor。项目当前处于 alpha，优先接受可复现的兼容、数据安全、交互和渲染性能改进。

## 开始之前

阅读 [开发指南](docs/development.md) 和 [项目交接](docs/project-handoff.md)。尤其需要遵守：

- 增强编辑器是客户端适配层，Yuushya 仍拥有世界数据、验证和 C2S 协议；
- 兼容绑定失败必须回退原 Screen；
- 编辑期间只改不可变草稿，不能直接写方块实体 NBT；
- 生产 Jar 不得包含 Yuushya 本体、Bench Provider 或 ModBench Runtime；
- 从 NCPB 继续移植代码时保留来源说明，并只引入编辑器所需部分。

## 提交改动

1. 从小而明确的分支开始。
2. 为 host-neutral 行为添加或更新单元测试。
3. UI、输入、渲染、自动保存、数据包和缓存行为应有 ModBench 覆盖。
4. 同步更新中英文翻译；`verifyLanguageKeys` 必须通过。
5. 如果改动用户行为、兼容矩阵或发布流程，同步更新文档和 CHANGELOG。

提交前至少执行：

```shell
./gradlew eclipseClasspath verifyIdeClasspath --no-daemon
./gradlew releaseBuild --no-build-cache --no-configuration-cache --no-daemon
```

涉及 Minecraft 客户端行为时再执行：

```shell
./gradlew verifyYuushyaEditorBench --no-daemon
```

Pull Request 请说明问题、用户可见变化、验证命令和兼容风险；视觉变化附截图，性能变化附同一环境下的前后指标。不要提交 `build/`、`build.nosync/`、`run/`、`.classpath`、自动生成 launch 配置或本机缓存。

## 报告问题

报告缺陷时提供 Minecraft、NeoForge、Yuushya Modelling 和增强编辑器的准确版本，复现步骤、客户端日志，以及问题是否只出现在特定资源包或服务器。若可能涉及存档损坏或安全问题，请按 [安全策略](SECURITY.md) 处理。
