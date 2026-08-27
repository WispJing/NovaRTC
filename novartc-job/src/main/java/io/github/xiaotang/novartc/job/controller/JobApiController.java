package io.github.xiaotang.novartc.job.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.xiaotang.novartc.common.result.Result;
import io.github.xiaotang.novartc.job.dto.JobInfoVO;
import io.github.xiaotang.novartc.job.dto.JobSaveRequest;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import io.github.xiaotang.novartc.job.service.JobManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务管理接口。
 *
 * @author xiaotang
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobApiController {

    private final JobManageService jobManageService;

    /**
     * 分页查询任务列表。
     */
    @GetMapping
    public Result<IPage<JobInfoVO>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                         @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(jobManageService.pageJobs(keyword, pageNum, pageSize));
    }

    /**
     * 查询单个任务详情。
     */
    @GetMapping("/{id}")
    public Result<JobInfoVO> detail(@PathVariable("id") String id) {
        return Result.success(jobManageService.getJob(id));
    }

    /**
     * 新增任务。
     */
    @PostMapping
    public Result<JobInfoVO> create(@RequestBody @Valid JobSaveRequest request) {
        return Result.success(jobManageService.create(request));
    }

    /**
     * 编辑任务。
     */
    @PutMapping("/{id}")
    public Result<JobInfoVO> update(@PathVariable("id") String id, @RequestBody @Valid JobSaveRequest request) {
        return Result.success(jobManageService.update(id, request));
    }

    /**
     * 删除任务。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") String id) {
        jobManageService.delete(id);
        return Result.success();
    }

    /**
     * 启用/停用任务。
     */
    @PostMapping("/{id}/toggle")
    public Result<JobInfoVO> toggle(@PathVariable("id") String id) {
        return Result.success(jobManageService.toggle(id));
    }

    /**
     * 手动执行一次。
     */
    @PostMapping("/{id}/run")
    public Result<Void> run(@PathVariable("id") String id) {
        jobManageService.runNow(id);
        return Result.success();
    }

    /**
     * 分页查询任务调度日志。
     */
    @GetMapping("/{id}/logs")
    public Result<IPage<JobLog>> logs(@PathVariable("id") String id,
                                      @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                      @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(jobManageService.pageLogs(id, pageNum, pageSize));
    }
}
