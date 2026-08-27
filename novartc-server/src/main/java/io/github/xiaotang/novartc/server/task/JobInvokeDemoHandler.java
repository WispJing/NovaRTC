package io.github.xiaotang.novartc.server.task;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 定时任务调度平台联调演示处理器：提供无参/带参两个示例方法，
 * 供调度平台配置执行类、方法、参数进行验证。
 *
 * @author xiaotang
 */
@Component
@RequiredArgsConstructor
public class JobInvokeDemoHandler {
    private static final Logger logger = LoggerFactory.getLogger(JobInvokeDemoHandler.class);

    /**
     * 无参演示方法。
     */
    public String demo() {
        String result = "定时任务演示方法执行成功, 时间=" + Instant.now();
        logger.info(result);
        return result;
    }

    /**
     * 带参演示方法，参数示例：["测试任务", 3]
     */
    public String demoWithParams(String name, Integer times) {
        String result = String.format("定时任务演示方法执行成功, name=%s, times=%d, 时间=%s", name, times, Instant.now());
        logger.info(result);
        return result;
    }
}
