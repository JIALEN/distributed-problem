package com.alen.distributed.problem.lock.redis;

import com.alen.distributed.problem.util.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.Collections;

/**
 * 基于redis的分布式锁的实现
 *
 * @author alen
 * @create 2018-11-19 14:09
 **/
public class RedisLock {
    private   Jedis jedis =null;
    private static String LOCK_MSG = "OK";
    private static long DEFAULT_SLEEP_TIME = 100;


  //非阻塞锁
    public static  boolean tryLock(String key, long timeout, String requestId) {
        String result = RedisUtil.getJedis().set(key, requestId, "NX", "EX", timeout);
        if (LOCK_MSG.equals(result)) {
            return true;
        } else {
            return false;
        }
    }

    //实现一个阻塞锁
    public static  void lock(String key, long timeout, String requestId) throws Exception {
        for (; ; ) {
            String result = RedisUtil.getJedis().set(key, requestId, "NX", "EX", timeout);
            if (LOCK_MSG.equals(result)) {
                break;
            }
            //防止一直消耗 CPU
            Thread.sleep(DEFAULT_SLEEP_TIME);
        }
    }

    //自定义阻塞时间获取锁
    public static  boolean lock(String key, String requestId, int blockTime, long timeout) throws InterruptedException {
        while (blockTime >= 0) {
            String result = RedisUtil.getJedis().set(key, requestId, "NX", "EX", timeout);
            if (LOCK_MSG.equals(result)) {
                return true;
            }
            blockTime -= DEFAULT_SLEEP_TIME;
            Thread.sleep(DEFAULT_SLEEP_TIME);
        }
        return false;
    }

    /**
     * 解锁
     *
     * @param key
     * @param requestId
     * @return
     */
    public static  boolean unlock(String key, String requestId) {
        //lua script
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = null;
        result = RedisUtil.getJedis().eval(script, Collections.singletonList(key), Collections.singletonList(requestId));
        if (!LOCK_MSG.equals(result)) {
            System.out.println(requestId+"--释放锁成功");
            return true;
        } else {
            System.out.println(requestId+"--释放锁失败");
            return false;
        }
    }
}
