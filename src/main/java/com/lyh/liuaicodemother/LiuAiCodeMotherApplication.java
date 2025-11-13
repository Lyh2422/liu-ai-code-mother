package com.lyh.liuaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lyh.liuaicodemother.mapper")
public class LiuAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiuAiCodeMotherApplication.class, args);
    }

}
