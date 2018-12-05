package com.alen.distributed.problem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.alen.distributed.problem.*")
public class DistributedProblemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedProblemApplication.class, args);
    }
}
