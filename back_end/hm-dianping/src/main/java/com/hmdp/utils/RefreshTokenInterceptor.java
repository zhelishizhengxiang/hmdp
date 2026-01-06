package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0
 * @ProjectName: hm-dianping
 * @Package: com.hmdp.utils
 * @Description: 专门做刷新token有效期的拦截器
 * @Author: Simon
 * @CreateDate: 2026/1/5
 */


public class RefreshTokenInterceptor implements HandlerInterceptor {


    private RedisTemplate redisTemplate;

    public RefreshTokenInterceptor(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //1.获取session中的 用户
//        Object user = request.getSession().getAttribute("user");
        //1.基于token从redis中获取用户信息
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)){
            //未登录状态直接放行，由后面拦截器判断是否拦截
            return true;
        }
        //3.获取用户信息（所有键值对）
        String redisKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = redisTemplate.opsForHash().entries(redisKey);
        if (userMap.isEmpty()){
            //未登录状态直接放行
            return true;
        }
        //4.将查询到的hash转换成userDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //5.保存用户信息
        UserHolder.saveUser((UserDTO) userDTO);
        //6.刷新token有效期
        redisTemplate.expire(redisKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
