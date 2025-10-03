package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    // 使用UUID 作为JVM的区分
    public static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long timeoutSec) {
//        long threadId = Thread.currentThread().getId(); //当前线程id
        String threadId = Thread.currentThread().getId()+ID_PREFIX;// 防止误删锁 获取锁时 把线程标识也存入
        // 同时设置锁和超时时间
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(RedisConstants.KEY_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(success); // 返回当前线程是否获取到锁 成功为1 失败为0
    }

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("test.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    @Override
    public void unlock() { // 调用lua脚本  保证删除锁的原子性
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(RedisConstants.KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());

    }

//    @Override
//    public void unlock() {
//        // 释放当前线程自己的锁 不能删除别人的锁
//        String threadId = ID_PREFIX +Thread.currentThread();  // 获取线程自己的标识
//        String id = stringRedisTemplate.opsForValue().get(RedisConstants.KEY_PREFIX + name); // 获取锁中的标识  RedisConstants.KEY_PREFIX + name是key
//        if(threadId.equals(id)){
//            stringRedisTemplate.delete(RedisConstants.KEY_PREFIX + name);
//        }
//
//    }


}
