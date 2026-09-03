package io.github.xiaotang.novartc.common.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务调度平台的任务声明注解，标注在 Spring Bean 的公共方法上。
 * <p>
 * Starter 仅注册标注的方法，拒绝任意类名/方法名反射调用；调度方式、参数和并发策略
 * 仍由调度平台侧配置管理。
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
