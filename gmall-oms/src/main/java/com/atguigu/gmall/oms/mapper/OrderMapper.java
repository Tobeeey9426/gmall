package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author Tobeey
 * @email 1340798171@qq.com
 * @date 2020-12-08 16:35:33
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
