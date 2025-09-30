package com.hmdp.intercepter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

public class LoginIntercepter implements HandlerInterceptor{

    private StringRedisTemplate stringRedisTemplate;

    public LoginIntercepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

//    // 若想使拦截器生效 需要配置拦截器
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
//            throws Exception {
//        // 避免内存泄露
//        UserHolder.removeUser();
//    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // // 前置拦截器 session方式
        // HttpSession session = request.getSession();
        // UserDTO user = (UserDTO) session.getAttribute("user");
        // if(user == null){
        //         // 拦截
        //         response.setStatus(401);
        //         return false;
        // }
        // // 用户存在 存入threadlocal
        // UserHolder.saveUser(user);
        // return true;

        // redis方式(单个拦截器方式)
//        String token = request.getHeader("authorization"); // 获取请求头中的token
//        if(StrUtil.isBlank(token)){
//            response.setStatus(401);
//            return false;
//        }
//        // 基于token获取redis中的用户
//        Map<Object,Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY+token);
//        if(userMap.isEmpty()){
//            //用户不存在 拦截 返回401状态码
//            response.setStatus(401);
//            return false;
//        }
//        // 存在 将hash数据转为userDTO对象
//        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//        UserHolder.saveUser(userDTO);
//        //刷新token有效期
//        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token, RedisConstants.LOGIN_USER_TTL,TimeUnit.MINUTES);
//
//        return true;

        // 多个拦截器
        // 判断threadlocal中是否有用户 无用户拦截 由用户 放行
        if(UserHolder.getUser() == null){
            response.setStatus(401);
            return false;
        }
        return true;
                


    }
    


}
