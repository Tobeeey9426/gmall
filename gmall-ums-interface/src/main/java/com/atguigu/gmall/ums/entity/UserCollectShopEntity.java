package com.atguigu.gmall.ums.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注店铺表
 * 
 * @author Tobeey
 * @email 1340798171@qq.com
 * @date 2020-12-08 16:40:40
 */
@Data
@TableName("ums_user_collect_shop")
public class UserCollectShopEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 店铺id
	 */
	private Long shopId;
	/**
	 * 店铺名
	 */
	private String shopName;
	/**
	 * 店铺logo
	 */
	private String shopLogo;
	/**
	 * 关注时间
	 */
	private Date createtime;

}
