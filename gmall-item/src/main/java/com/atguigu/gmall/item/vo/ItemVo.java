package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    //一级 二级 三级
    private List<CategoryEntity> categories;

    //品牌信息
    private Long brandId;
    private String brandName;

    //spu信息
    private Long spuId;
    private String spuName;

    // sku相关信息

    private Long skuId;
    private String title;
    private String subtitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;

    //图片列表
    private List<SkuImagesEntity> skuImages;

    //营销信息
    private List<ItemSaleVo> sales;

    //是否有货
    private Boolean store = false;

    //销售属性
    //{{attrId;4,attrName: 颜色, attrValues:["白","黑"]}}
    //{{attrId;5,attrName: 内存, attrValues:["8g","12g"]}}
    //{{attrId;6,attrName: 存储, attrValues:["128g","256g"]}}
    private List<SaleAttrValueVo> saleAttrs;

    //单个商品的信息
    //{4:白, 5:'8g',6:'128G'}
    private Map<Long,String> saleAttr;

    //销售属性组合和skuId的映射关系
    //{'白色,8g,128g':100,'白色,8g,256g': 101}
    private String skusJson;

    //商品详情
    private List<String> spuImages;

    //规格参数
    private List<ItemGroupVo> groups;

    //1.根据skuId查询sku信息 *
    //2.根据3级分类的id查询一二三级分类 *
    //3.根据brandId查询品牌 *
    //4.根据spuId查询spu信息 *
    //5.根据skuId查询sku的图片列表信息 *
    //6.根据skuId查询营销信息 *
    //7.根据skuId查询sku的库存信息 *
    //8.根据spuId查询查询spu下所有sku的销售属性 *
    //9.根据skuId查询sku的销售属性 *
    //10.根据spuId查询spu下所有销售属性与skuId的映射关系 *
    //11.根据spuId查询spu的描述信息 *
    //12.根据分类id,skuId,spuId查询分组及组下的规格参数和值


}
