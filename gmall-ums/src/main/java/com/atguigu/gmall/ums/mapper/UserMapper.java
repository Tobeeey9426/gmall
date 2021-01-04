package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author Tobeey
 * @email 1340798171@qq.com
 * @date 2020-12-08 16:40:40
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
