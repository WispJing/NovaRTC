package io.github.xiaotang.novartc.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.apache.ibatis.type.JdbcType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Created on 2026/3/18 11:35
 *
 * @author tangj
 * @description
 */
public class CodeGenerator {
    private static final IColumnType INSTANT_COLUMN_TYPE = new IColumnType() {
        @Override
        public String getType() {
            return "Instant";
        }

        @Override
        public String getPkg() {
            return "java.time.Instant";
        }
    };

    public static void main(String[] args) {
        Path projectRoot = resolveProjectRoot();
        Path javaOutputDir = projectRoot.resolve("novartc-server").resolve("src").resolve("main").resolve("java");
        Path mapperOutputDir = projectRoot.resolve("novartc-server").resolve("src").resolve("main").resolve("resources").resolve("mappers");

        // 使用 FastAutoGenerator 快速配置代码生成器
        FastAutoGenerator.create("jdbc:mysql://localhost:3307/novartc?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                        , "root", "123456")
                .globalConfig(builder -> {
                    builder.author("xiaotang") // 设置作者
                            .outputDir(javaOutputDir.toString())
                            .dateType(DateType.TIME_PACK); // 输出目录
                })
                .dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                    JdbcType jdbcType = metaInfo.getJdbcType();
                    if (jdbcType == JdbcType.TIMESTAMP) {
                        String typeName = metaInfo.getTypeName();
                        if ("datetime".equalsIgnoreCase(typeName)) {
                            return INSTANT_COLUMN_TYPE;
                        }
                    }
                    return typeRegistry.getColumnType(metaInfo);
                }))
                .packageConfig(builder -> {
                    builder.parent("io.github.xiaotang.novartc.server") // 设置父包名
                            .entity("entity.mysql") // 设置实体类包名
                            .mapper("mapper") // 设置 Mapper 接口包名
                            .service("service") // 设置 Service 接口包名
                            .serviceImpl("service.impl") // 设置 Service 实现类包名
                            .pathInfo(Collections.singletonMap(
                                    OutputFile.xml,
                                    mapperOutputDir.toString()
                            )); // 设置 Mapper XML 文件包名
                })
                .strategyConfig(builder -> {
                    // "im_user", "im_friend", "im_friend_request", "im_group_chat", "im_group_member", "im_single_chat"
                    builder.addInclude("admin_login_log", "im_friend_request", "risk_group_mute", "risk_login_alert", "risk_user_control")
                            .entityBuilder()
                            .superClass("io.github.xiaotang.novartc.server.entity.mysql.BaseEntity")
                            .addSuperEntityColumns("id", "is_deleted", "created_at", "updated_at")
                            .enableLombok() // 启用 Lombok
                            .enableTableFieldAnnotation() // 启用字段注解
                            .logicDeleteColumnName("is_deleted")
                            .disableSerialVersionUID()
                            .enableFileOverride()
                            .mapperBuilder()
                            .serviceBuilder()
                            .controllerBuilder()
                            .disable();
                })
                // 使用 Freemarker 模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private static Path resolveProjectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("novartc-generator"))
                    && Files.isDirectory(current.resolve("novartc-server"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NovaRTC project root from current working directory.");
    }
}
