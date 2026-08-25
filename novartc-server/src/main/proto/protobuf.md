# protobuf 代码生成

`ChatMessage.java` 不再手工生成和提交，由 Maven 插件在构建期自动生成。

## 生成方式

在 `NovaRTC` 根目录执行（任意包含 compile 阶段的目标均可）：

```bash
mvn -pl novartc-server -am clean compile
```

`protobuf-maven-plugin` 会：

1. 按当前操作系统自动从 Maven 仓库下载对应版本的 `protoc`（版本取自父 POM 的 `protobuf.version`，需与 `protobuf-java` 运行时一致）；
2. 编译 `novartc-server/src/main/proto` 下的全部 `*.proto` 文件；
3. 生成 Java 代码到 `novartc-server/target/generated-sources/protobuf/java`，并自动加入编译源路径。

## 注意事项

- 无需本地安装 `protoc`，编译器随构建自动解析。
- 生成产物位于 `target/` 目录，已被 `.gitignore` 忽略，请勿再提交生成的 Java 文件。
- 修改 `ChatMessage.proto` 后直接重新构建即可，Java 侧包名由文件内 `option java_package` 决定。
- `index.html` 为调试页面，通过相对路径加载同目录的 `ChatMessage.proto`，两者请保持在同一目录。
