package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = RedisConstants.CACHE_TYPE_KEY;
        // 从redis查询type缓存
        //stringRedisTemplate.opsForList().range(key, 0, -1) 从 Redis 中获取列表类型的数据
        /*
        *   通过 stream() 将 Redis 返回的字符串列表转换为流
            使用 map(type -> JSONUtil.toBean(type, ShopType.class)) 将每个 JSON 字符串转换为 ShopType 对象
            collect(Collectors.toList()) 将流收集为 List<ShopType> 列表
        * */
        List<ShopType> typeList = stringRedisTemplate.opsForList().range(key, 0, -1)
                .stream().map(type -> JSONUtil.toBean(type, ShopType.class)).collect(Collectors.toList());
        if(!typeList.isEmpty()){
            // 存在 直接返回
            return Result.ok(typeList);
        }
        // 不存在
        List<ShopType> list = this.query().orderByAsc("sort").list();
        if(list.isEmpty()){
            return Result.fail("没有商家类别");
        }
        List<String> listJson = list.stream().map(JSONUtil::toJsonStr).collect(Collectors.toList());
        stringRedisTemplate.opsForList().leftPushAll(key,listJson);
        return Result.ok(list);
    }
}
