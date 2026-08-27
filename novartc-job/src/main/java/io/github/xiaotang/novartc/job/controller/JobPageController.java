package io.github.xiaotang.novartc.job.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 调度平台页面路由。
 *
 * @author xiaotang
 */
@Controller
public class JobPageController {

    /**
     * 任务管理主页（Thymeleaf 模板）。
     */
    @GetMapping("/")
    public String index() {
        return "job/index";
    }
}
