package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;

import com.hmdp.utils.UserHolder;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
//        1.查询优惠券
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
//        2.判断秒杀是否开始
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){ // isAfter() - 比较时间先后关系的方法
            return Result.fail("秒杀未开始");
        }

//        3.判断秒杀是否结束
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){ // isAfter() - 比较时间先后关系的方法
            return Result.fail("秒杀已经结束");
        }
//        4.判断库存是否充足
        if(voucher.getStock() < 1){
            return Result.fail("库存不足");

        }
//        一人一单 需要根据优惠券id和用户id查询订单 判断订单是否存在 存在不可以再买 从而实现一人一单
        Long userId = UserHolder.getUser().getId();
        // 创建锁对象
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("order:" + userId); //redission 获取锁
        boolean isLock = lock.tryLock();
        if(!isLock){
            //获取失败
            return Result.fail("不允许重复下单");

        }
        // 成功，执行业务
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            //需要使用代理来调用方法，否则会造成事务失效（这里是造成Spring事务失效的场景之一）
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            lock.unlock();
        }

//        return createVoucherOrder(voucherId);


    }




    @Transactional
    public  Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        Long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count>0){
            return Result.fail("用户已购买过");
        }
        synchronized(userId.toString()){


//        5.扣减库存
//        UPDATE seckill_voucher SET stock = stock - 1 WHERE voucher_id = ?  orderId 是set条件 eq是where条件 即xx = ?  gt是大于 即 xx>?
        boolean success = iSeckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock",0).update();
        if(!success){
            return Result.fail("库存不足");
        }
//        6.创建订单
        VoucherOrder order = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");

        order.setId(orderId); //订单id
        order.setUserId(userId);
        order.setVoucherId(voucherId);//代金券id
        save(order);
        return Result.ok(orderId);
    }
    }

}
