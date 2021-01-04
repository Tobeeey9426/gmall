package com.atguigu.gmall.search.listener;


import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ItemListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository  goodsRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_ITEM_QUEUE",durable = "true"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message){

        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();

        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuBySpuId(spuId);
        List<SkuEntity> skuEntities = skuResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntities)){
            //3.把sku集合转化为goods集合
            List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();
                //sku相关
                goods.setSkuId(skuEntity.getId());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubTitle(skuEntity.getSubtitle());
                goods.setDefaultImage(skuEntity.getDefaultImage());
                goods.setPrice(skuEntity.getPrice().doubleValue());

                //设置spu相关
                goods.setCreateTime(spuEntity.getCreateTime());


                //设置库存的相关信息
                ResponseVo<List<WareSkuEntity>> wareResponseVo =
                        this.wmsClient.queryWareSkuBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)){
                    goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b) ->a+b).get());
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                }

                //品牌
                ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResponseVo.getData();

                if (brandEntity != null){
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }


                //设置分类
                ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                if (categoryEntity != null){
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                //获取检索参数
                List<SearchAttrValueVo> searchAttrValueVos = new ArrayList<>();
                //查询销售类型的检索参数
                ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo =
                        this.pmsClient.querySearchSkuAttrValuesByCidAndSkuId(skuEntity.getCatagoryId(), skuEntity.getId());
                List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    searchAttrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity ,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));

                }

                //查询基本类型的检索参数
                ResponseVo<List<SpuAttrValueEntity>> spuAttrValueResponseVO =
                        this.pmsClient.querySearchSpuAttrValuesByCidAndSpuId(skuEntity.getCatagoryId(), spuId);
                List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueResponseVO.getData();
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    searchAttrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity ,searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));

                }
                goods.setSearchAttrs(searchAttrValueVos);

                return goods;
            }).collect(Collectors.toList());

            this.goodsRepository.saveAll(goodsList);
        }
    }
}
