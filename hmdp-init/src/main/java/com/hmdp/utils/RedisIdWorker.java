package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    /*@Component 将一个类标记为 Spring 容器管理的组件
      使该类成为 Spring IoC（控制反转）容器中的 Bean
    * */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public static final long BEGIN_TIMESTAMP = 1640995299L;//2021年1月1日时间戳
    public static final int COUNT_BITS = 32;//序列号位数
//    ID: 1位符号位 + 31位时间戳 + 32位序列号  全局唯一id (订单id)
    public long nextId(String keyPrefix){
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        return timestamp << COUNT_BITS | increment; // timestamp左移32bit 再拼接 increment

    }

}
