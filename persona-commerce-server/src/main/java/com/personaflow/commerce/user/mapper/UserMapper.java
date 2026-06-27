package com.personaflow.commerce.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personaflow.commerce.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
