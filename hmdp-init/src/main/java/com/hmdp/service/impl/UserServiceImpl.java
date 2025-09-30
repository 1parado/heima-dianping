package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机验证不通过");
        }
        String code = (String) RandomUtil.randomNumbers(6); // 验证码
        // 1. session 实现方式
        // session.setAttribute("code", code);
        // // 发送验证码
        // log.info("验证码:{}", code);
        // 2.redis实现方式  RedisConstants.LOGIN_CODE_KEY+phone为key code为value
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone, code,RedisConstants.LOGIN_CODE_TTL,TimeUnit.MINUTES);
        log.debug("验证码发送成功：{}",code);// 可以通过邮箱实现 阿里云要钱
        return Result.ok();
        


    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();

        // 1、判断手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式不正确");
        }
        // session 方式
        // // 2、判断验证码是否正确
        // Object cacheCode = session.getAttribute("code");
        // String code = loginForm.getCode();
        // if (cacheCode == null || !cacheCode.toString().equals(code)) {
        //     return Result.fail("验证码不正确");
        // }
        // User user = query().eq("phone", phone).one(); // mybatis-plus的查询语句
        // if (user == null) {
        //     // 用户不存在 创建用户
        //     user = createUserWithPhone(phone);
        // }
        // // 保存用户信息到session中
        // // session.setAttribute("user", user); //只需要保存用户部分信息给session即可
        // UserDTO userDTO = new UserDTO();
        // BeanUtils.copyProperties(user, userDTO); // source：源对象，提供属性值 target：目标对象，接收属性值
        // session.setAttribute("user", userDTO); // 将 userDTO 对象以键名 "user" 存储到 HTTP 会话(session)中
        // redis方式
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone);
        System.out.println(cacheCode);
        String code = loginForm.getCode();
        System.out.println(code);
        if(cacheCode==null  || !cacheCode.equals(code)){
            return Result.fail("验证码错误");
        }
        User user = query().eq("phone", phone).one();
        if(user ==  null){
            // 创建新用户
            user = createUserWithPhone(phone);
        }
        // 保存用户信息到redis
        String token = UUID.randomUUID().toString(true); // 随机生成token 作为登录令牌
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class); // hutool包下的BeanUtil
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
            CopyOptions.create()
                    .setIgnoreNullValue(true)
                    .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 存储
        String tokenKey =  RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 设置token有效期
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);

    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user); // mp提供的方法
        return user;
    }

}
