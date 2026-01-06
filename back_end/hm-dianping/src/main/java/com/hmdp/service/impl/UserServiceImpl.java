package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


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
        //1.校验手机号，用正则表达式进行判断
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.不符合则返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
//        //4.保存验证码到session
//        session.setAttribute("code", code);
        //4.缓存验证码到redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码
        log.debug("发送验证码成功：{}", code);
        return Result.ok("发送成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            //2.不符合则返回错误信息
            return Result.fail("手机号格式错误");
        }
        //2.校验验证码:从session获取验证码
//        Object cacheCode = session.getAttribute("code");
//        if (cacheCode == null || !cacheCode.toString().equals(loginForm.getCode())){
//            //3.不一致，返回错误信息
//            return Result.fail("验证码错误");
//        }

        //2.校验验证码:从redis获取验证码
        String cacheCode =stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + loginForm.getPhone());
        if (cacheCode == null || !cacheCode.toString().equals(loginForm.getCode())){
            //3.不一致，返回错误信息
            return Result.fail("验证码错误");
        }
        //4.一致，根据手机号查询用户
        User user = query().eq("phone", loginForm.getPhone()).one();
        //判断用户是否存在
        if (user== null){
            //5.不存在，创建新用户并保存
            user= createUserWithPhone(loginForm.getPhone());
        }
//        //6.保存用户到session并返回结果
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        //6.保存用户到redis
        //6.1生成token作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //6.2将user对象转为hash
        UserDTO userDTO= BeanUtil.copyProperties(user, UserDTO.class);
        // NOTE 将对象转成Map时必须要2将所有字段转换为字符串这样才可以符合stringRedisTemplate得要求
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, value) -> value.toString()));
        //6.3存储到redis，并设置过期时间
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //6.4将token返回给前端
        return Result.ok(token);

    }

    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));
        //保存用户
        save(user);
        return user;
    }
}
