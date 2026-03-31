package com.vayen.rdcm.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpInterface;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 指定一条 match 规则
            SaRouter
                    // TODO 先不拦截，开发需要
                .match("/**")    // 拦截所有路径
                .notMatch("/api/auth/login", "/api/auth/register")  // 排除登录/注册接口

                .check(r -> {
                    // 登录校验：未登录则抛出异常
                    // 后续可以根据不同路径设置不同权限
                    // 例如：/api/admin/** 需要管理员权限
                });
        })).addPathPatterns("/**");
    }

    @Bean
    public StpInterface stpInterface() {
        return new StpInterfaceImpl();
    }
}
