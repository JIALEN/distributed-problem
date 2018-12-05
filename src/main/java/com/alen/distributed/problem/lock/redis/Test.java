package com.alen.distributed.problem.lock.redis;

import com.alen.distributed.problem.lock.zk.javaapi.DistributeLock;
import com.alen.distributed.problem.util.RedisUtil;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author alen
 * @create 2018-11-19 16:05
 **/
public class Test {
    public static void main(String[] args) {
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                String name=Thread.currentThread().getName();
                Boolean result= null;
                try {
                    result = RedisLock.lock("Lock_test", name,500,1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(result){
                       System.out.println("线程获得锁："+name);
                       RedisLock.unlock("Lock_test",name);
                   }else{
                       System.out.println("线程没获得锁："+Thread.currentThread().toString());
                   }
            }).start();
        }

    }
}
