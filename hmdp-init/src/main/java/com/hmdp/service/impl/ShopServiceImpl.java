package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
//        // 缓存和数据库双写一致    根据id查询 缓存未命中 查询数据库 将数据库结果写入缓存设置超时时间   根据id修改 先修改数据库 再删除缓存
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        // 从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//        if(StrUtil.isNotBlank(shopJson)){ // 存在 直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return Result.ok(shop);
//        }
//        if(shopJson == ""){
//            return Result.fail("店铺不存在");
//        }
//        // 不存在 根据id查询数据库
//        Shop shop = this.getById(id);
//        if(shop == null){
//            // 为解决缓存穿透 将null写入redis
//            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
//            return Result.fail("店铺不存在");
//        }
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 互斥锁解决缓存穿透
        
        return queryWithMutex(id);
    }

    private Result queryWithMutex(Long id) {
        
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        while (true) {
            // 1 从redis查询商铺缓存
            String shopJson = stringRedisTemplate.opsForValue().get(key);
            // 2 判断是否存在
            if (StrUtil.isNotBlank(shopJson)) {
                // 3 存在，直接返回
                Shop shop = JSONUtil.toBean(shopJson, Shop.class);
                return Result.ok(shop);
            }
            /**
             * isNotBlank()方法会检测到空字符串
             * 为解决缓存穿透，额外判断是否为""空字符串，是则返回错误信息
             */
            if (shopJson != null) {
                return Result.fail("店铺不存在！");
            }

            // TODO 4 实现缓存重建
            // TODO 4.1 获取互斥锁
            boolean isLock = tryLock(lockKey);
            // TODO 4.2 判断是否获取成功
            if (!isLock) {
                // TODO 4.3 失败：则休眠重试
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        try {
            //double check
            String shopJson = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(shopJson)) {
                return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
            }

            // TODO 4.4 成功，根据id查询数据库
            Shop shop = this.getById(id);
            // 模拟缓存击穿重建业务耗时久情况
            Thread.sleep(200);
            // 5 数据库不存在，返回错误
            if (shop == null) {
                /**
                 * 为解决缓存穿透，将空值写入redis
                 */
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return Result.fail("店铺不存在！");
            }
            // 6  存在，写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

            // 7 存在，返回数据
            return Result.ok(shop);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // TODO 7 释放互斥锁
            unlock(lockKey);
        }
    }
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long shopId = shop.getId();
        if(shopId == null){
            // 店铺id为空 返回错误信息
            return Result.fail("店铺id不能为空");
        }
        // 不为空 先更新数据库 再删除缓存
        this.updateById(shop);
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+shopId);
        return Result.ok();
    }

    private boolean tryLock(String key) { // 获得锁
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(isLock);  //包装类会做拆箱可能出现空指针，于是主动拆箱
    }

    private void unlock(String key){ // 释放锁
        stringRedisTemplate.delete(key);
    }


}
