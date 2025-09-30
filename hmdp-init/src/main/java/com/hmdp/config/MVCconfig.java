package com.hmdp.config;


import javax.annotation.Resource;

import com.hmdp.intercepter.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hmdp.intercepter.LoginIntercepter;

@Configuration
public class MVCconfig implements WebMvcConfigurer{
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginIntercepter(stringRedisTemplate)).excludePathPatterns(
            "/user/code",
            "/user/login",
            "/blog/hot",
            "/shop-type/**",
            "/voucher/**",
            "upload",
            "shop/**").order(1);// 排除拦截路径 默认拦截一切
        // token刷新的拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0); //.order()控制拦截器的执行顺序
    }
}
