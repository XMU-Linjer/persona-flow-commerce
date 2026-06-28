package com.personaflow.commerce.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaflow.commerce.cart.entity.CartItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItemEntity> {
}
