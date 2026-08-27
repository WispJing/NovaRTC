package io.github.xiaotang.novartc.common.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务调度平台的任务声明注解，标注在 Spring Bean 的公共无参方法上。
 * <p>
 * 注解仅携带任务名称与备注，后续由扫描器将标注的方法注册写入 job_info，
 * 交由调度平台统一调度执行；调度方式、并发策略等由调度平台侧配置管理。
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
     * 备注，对应 job_info.remark
     */
    String remark() default "";
}
