package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserCollectShopEntity;

import java.util.Map;

/**
 * 关注店铺表
 *
 * @author Tobeey
 * @email 1340798171@qq.com
 * @date 2020-12-08 16:40:40
 */
public interface UserCollectShopService extends IService<UserCollectShopEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

