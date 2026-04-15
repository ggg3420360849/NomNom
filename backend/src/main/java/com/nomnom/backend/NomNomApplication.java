package com.nomnom.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nomnom.backend.mapper")
public class NomNomApplication {

    public static void main(String[] args) {
        SpringApplication.run(NomNomApplication.class, args);
    }
}
