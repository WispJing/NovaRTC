package io.github.xiaotang.novartc.server.task;

/**
 * 单元测试专用执行目标：提供无参/带参两个方法，
 * 供 {@link JobInvokeExecutor} 反射执行相关测试使用，不参与生产运行。
 *
 * @author xiaotang
 */
public class JobInvokeTestHandler {

    /**
     * 无参测试方法。
     */
    public String demo() {
        return "定时任务测试方法执行成功";
    }

    /**
     * 带参测试方法，参数示例：["单元测试", 3]
     */
    public String demoWithParams(String name, Integer times) {
        return String.format("定时任务测试方法执行成功, name=%s, times=%d", name, times);
    }
}
