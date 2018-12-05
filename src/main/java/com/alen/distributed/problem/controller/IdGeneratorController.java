package com.alen.distributed.problem.controller;

import com.alen.distributed.problem.id.redis.RedisIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author alen
 * @create 2018-11-27 20:30
 **/
@RestController
public class IdGeneratorController {
    @Autowired
    private RedisIdGenerator redisIdGenerator;

    @GetMapping("/generateId")
    public String generateId() {
        String id = redisIdGenerator.generateCode("0001");
        System.out.println("--1-:" + id);
        return id;
    }
}
