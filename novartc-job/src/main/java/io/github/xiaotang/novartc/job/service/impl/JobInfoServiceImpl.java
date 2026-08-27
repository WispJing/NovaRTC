package io.github.xiaotang.novartc.job.service.impl;

import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.mapper.JobInfoMapper;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 定时任务定义表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-08-27
 */
@Service
public class JobInfoServiceImpl extends ServiceImpl<JobInfoMapper, JobInfo> implements IJobInfoService {

}
