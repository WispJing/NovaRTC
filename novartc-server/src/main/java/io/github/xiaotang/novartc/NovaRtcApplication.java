package io.github.xiaotang.novartc;

import io.github.xiaotang.novartc.repository.mongodb.IMUserRepository;
import io.github.xiaotang.novartc.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created on 2026/2/27 16:14
 *
 * @author tangj
 */
@SpringBootApplication
public class NovaRtcApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(NovaRtcApplication.class, args);
    }

    @Autowired
    private IMUserRepository imUserRepository;

    @Override
    public void run(String... args) {
        System.out.println("查询系统所有用户...");
        System.out.println(JacksonUtil.toJson(imUserRepository.findAll()));
    }
}
