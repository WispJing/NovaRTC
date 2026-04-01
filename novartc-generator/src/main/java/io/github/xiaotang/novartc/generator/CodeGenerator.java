package io.github.xiaotang.novartc.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * Created on 2026/3/18 11:35
 *
 * @author tangj
 * @description
 */
public class CodeGenerator {
    public static void main(String[] args) {
        // 使用 FastAutoGenerator 快速配置代码生成器
        FastAutoGenerator.create("jdbc:mysql://localhost:3307/novartc?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                        , "root", "123456")
                .globalConfig(builder -> {
                    builder.author("xiaotang") // 设置作者
                            .outputDir("D:\\NovaRTC\\NovaRTC\\novartc-server\\src\\main\\java")
                            .dateType(DateType.TIME_PACK); // 输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("io.github.xiaotang.novartc.server") // 设置父包名
                            .entity("entity.mysql") // 设置实体类包名
                            .mapper("mapper") // 设置 Mapper 接口包名
                            .service("service") // 设置 Service 接口包名
                            .serviceImpl("service.impl") // 设置 Service 实现类包名
                            .pathInfo(Collections.singletonMap(
                                    OutputFile.xml,
                                    "D:\\NovaRTC\\NovaRTC\\novartc-server\\src\\main\\resources\\mappers"
                            )); // 设置 Mapper XML 文件包名
                })
                .strategyConfig(builder -> {
                    // "im_user", "im_friend", "im_friend_request", "im_group_chat", "im_group_member", "im_single_chat"
                    builder.addInclude("admin_menu", "admin_role", "admin_user_role", "admin_role_menu", "admin_user_menu", "admin_operate_log", "admin_login_log")
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
}
