package com.alen.distributed.problem.id.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 用Redis的原子操作 INCR和INCRBY来实现
 *
 * @author alen
 * @create 2018-11-27 18:00
 **/
@Component
public class RedisIdGenerator {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Logger log = LoggerFactory.getLogger(RedisIdGenerator.class);

    private static final String REDIS_DISTRIBUTED_ID = "REDIS_DISTRIBUTED_ID";

    private static final int minLength = 36;

    public Long generateId(String key) {
        RedisAtomicLong counter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        // counter.expireAt(null);
        return counter.incrementAndGet();
    }


    /**
     * @param type 三位业务编码
     * @return
     */
    public String generateCode(String type) {
        try {
            Long id = null;
            id = this.generateId(REDIS_DISTRIBUTED_ID);
            if (id != null) {
                return format(id, type, minLength);
            }
        } catch (Exception e) {
            log.info("error-->redis生成id时出现异常");
            log.error(e.getMessage(), e);
        }
        return null;
    }

    //设定格式
    private static String format(Long id, String type, Integer minLength) {
        StringBuffer sb = new StringBuffer();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        sb.append(dtf.format(LocalDateTime.now()));
        sb.append(type);
        String strId = String.valueOf(id);
        sb.append(getRandomNumber());
        int length = strId.length()+sb.length();
        if (length < minLength) {
            for (int i = 0; i < minLength - length; i++) {
                sb.append("0");
            }
            sb.append(strId);
        } else {
            sb.append(strId);
        }
        return sb.toString();
    }
    //得到四位随机数
    private static String getRandomNumber(){
        int  random=(int)(Math.random()*8999)+1000;
        return String.valueOf(random);
    }

    public static void main(String[] args) throws InterruptedException {
        RedisIdGenerator redisIdGenerator = new RedisIdGenerator();
        String id = redisIdGenerator.generateCode("0001");
        System.out.println(id);
    }
}
