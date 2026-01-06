package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 1.从redis中查询缓存
        String key = RedisConstants.CACHE_SHOP_TYPE_LIST_KEY;
        String shopTypeListJson = stringRedisTemplate.opsForValue().get(key);
        // 2.如果存在直接返回
        if (StrUtil.isNotBlank(shopTypeListJson)) {
            List<ShopType> typeList = JSONUtil.toList(shopTypeListJson, ShopType.class);
            return Result.ok(typeList);
        }
        // 3.不存在，从数据库中查询
        List<ShopType> typeList = this
                .query().orderByAsc("sort").list();
        // 4.数据库查询为空，返回空列表并设置缓存
        if (typeList == null || typeList.isEmpty()) {
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList), RedisConstants.CACHE_SHOP_TYPE_LIST_TTL, TimeUnit.MINUTES);
            return Result.ok(typeList);
        }
        // 5.存在，写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList), RedisConstants.CACHE_SHOP_TYPE_LIST_TTL, TimeUnit.MINUTES);
        // 6.返回
        return Result.ok(typeList);
    }
}