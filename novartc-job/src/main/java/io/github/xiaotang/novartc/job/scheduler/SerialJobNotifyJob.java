package io.github.xiaotang.novartc.job.scheduler;

import org.quartz.DisallowConcurrentExecution;

/**
 * 禁止并发执行的任务：同一任务的上一次触发未完成时，下一次触发排队等待。
 *
 * @author xiaotang
 */
@DisallowConcurrentExecution
public class SerialJobNotifyJob extends AbstractJobNotifyJob {
}
