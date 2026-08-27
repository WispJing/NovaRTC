package io.github.xiaotang.novartc.job.scheduler;

/**
 * 允许并发执行的任务：上一次触发的消息发送未完成时，下一次触发仍可并行执行。
 *
 * @author xiaotang
 */
public class ConcurrentJobNotifyJob extends AbstractJobNotifyJob {
}
