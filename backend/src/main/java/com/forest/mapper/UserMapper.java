package com.forest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.forest.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}