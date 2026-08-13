# Development runtime

`yuushya_modelling-26.1.2-2.4.2.jar` is a development-only runtime dependency built without source changes
from the official [Yuushya Modelling](https://github.com/Crystal1921/Yuushya-Modelling) `26.1` branch at
commit `57f4d86407510f76191e35c280d8e1c8d347bf42`.

- Upstream license: MIT
- SHA-256: `3cbb934c6a4e94146eb9b7fc2fc42a94f6730ae0d6b12ff0c3f2f7b6824616b4`
- Purpose: compile against the audited host API and load the required host mod in development and Bench runs
- Packaging: Gradle declares it as `compileOnly`, `runtimeOnly`, and `benchImplementation`; it is not included in
  the enhanced editor's output Jar

The official Modrinth project currently has no Minecraft 26.1.2 NeoForge artifact, so there is no valid public
Maven coordinate for this audited build. To test another compatible Jar without replacing this file, run:

```shell
./gradlew runClient \
  -Pyuushya_runtime_jar=/absolute/path/to/yuushya_modelling.jar \
  -Pyuushya_runtime_sha256=<lowercase-sha256>
```

The path and digest must be overridden together. Supplying only a different path intentionally fails the
runtime audit.
