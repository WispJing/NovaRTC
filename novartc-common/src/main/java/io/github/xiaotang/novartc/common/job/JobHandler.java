package io.github.xiaotang.novartc.common.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务调度平台的任务声明注解，标注在 Spring Bean 的公共无参方法上。
 * <p>
 * 注解携带任务元信息（名称、调度方式、并发策略、备注），后续由扫描器
 * 将标注的方法注册写入 job_info，交由调度平台统一调度执行；
 * 扫描与写库逻辑待开发，当前仅作为声明式标记使用。
 * <p>
 * 调度方式二选一：{@link #cron()} 非空时按 CRON 调度（UTC 时区），
 * 否则按 {@link #fixedRateSeconds()} 固定频率调度。
 *
 * @author xiaotang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobHandler {

    /**
     * 任务名称，对应 job_info.job_name
     */
    String jobName();

    /**
     * CRON 表达式（UTC 时区），支持 {@code ${...}} 配置占位符，
     * 由扫描器通过 Spring Environment 解析；为空时使用 {@link #fixedRateSeconds()}
     */
    String cron() default "";

    /**
     * 固定频率间隔（秒），{@link #cron()} 为空时生效
     */
    int fixedRateSeconds() default 0;

    /**
     * 是否允许并发执行，对应 job_info.concurrent_allowed；
     * 默认不允许，重复触发时上一轮未结束则跳过
     */
    boolean concurrentAllowed() default false;

    /**
     * 备注，对应 job_info.remark
     */
    String remark() default "";
}
