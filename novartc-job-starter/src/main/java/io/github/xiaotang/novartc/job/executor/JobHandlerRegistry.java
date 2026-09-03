package io.github.xiaotang.novartc.job.executor;

import io.github.xiaotang.novartc.common.job.JobHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 仅收集显式标注的 Spring Bean 方法。
 * <p>
 * 消息携带的类名与方法名只是处理器定位键；未登记的目标一律拒绝，避免将消息内容作为任意反射调用入口。
 */
public class JobHandlerRegistry {
    private final Map<String, Handler> handlers = new HashMap<>();

    public JobHandlerRegistry(ApplicationContext context) {
        context.getBeansWithAnnotation(org.springframework.stereotype.Component.class)
                .forEach((name, bean) -> registerBean(bean));
    }

    private void registerBean(Object bean) {
        Class<?> type = AopUtils.getTargetClass(bean);
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(JobHandler.class)) {
                String key = key(type.getName(), method.getName());
                if (handlers.putIfAbsent(key, new Handler(bean, method)) != null) {
                    throw new IllegalStateException("重复注册任务处理器: " + key);
                }
            }
        }
    }

    public Handler require(String className, String methodName) {
        Handler handler = handlers.get(key(className, methodName));
        if (handler == null) {
            throw new IllegalArgumentException(
                    "未注册的任务处理器: " + className + "#" + methodName);
        }
        return handler;
    }

    private String key(String className, String methodName) {
        return className + "#" + methodName;
    }

    public record Handler(Object bean, Method method) {
    }
}
