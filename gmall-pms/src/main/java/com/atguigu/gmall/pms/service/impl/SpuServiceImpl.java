package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.service.SpuService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper spuDescMapper;

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public PageResultVo querySpyByCidAndPage(Long categoryId, PageParamVo pageParamVo) {

        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();

        //如果用户选择了分类,并且查询本类
        if (categoryId != 0){
            queryWrapper.eq("category_id",categoryId);
        }

        String key = pageParamVo.getKey();
        //判断关键字是否为空
        if (StringUtils.isNotBlank(key)){
            queryWrapper.and(t ->t.eq("id",key).or().like("name",key));
        }


        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                queryWrapper);

        return new PageResultVo(page);
    }


    @GlobalTransactional
    @Override
    public void newSave(SpuVo spuVo) {
        //1保存spu相关
        //1.1保存spu基本信息 pms_spu
        Long spuId = saveSpu(spuVo);

        //1.2保存spu的描述信息 pms_spu_desc
        saveSpuDesc(spuVo, spuId);
        //1.3保存spu的规格参数信息 pms_spu_attr_value
        saveBaseAttr(spuVo, spuId);

        //2保存sku相关信息

        saveSkuInfo(spuVo, spuId);

        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE","item.insert",spuId);

    }

    public void saveSkuInfo(SpuVo spuVo, Long spuId) {
        //先获取sku信息
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return;
        }
        skus.forEach(sku -> {

            //2.1保存sku基本信息 pms_sku
            //设置spuId
            sku.setSpuId(spuId);
            //设置品牌Id和categoryId 从spu中获取
            sku.setBrandId(spuVo.getBrandId());
            sku.setCatagoryId(spuVo.getCategoryId());
            //获取图片列表
            List<String> images = sku.getImages();
            //如果图片列表不为空则设置默认图片
            if (!CollectionUtils.isEmpty(images)){
                sku.setDefaultImage(StringUtils.isNotBlank(sku.getDefaultImage())?
                        sku.getDefaultImage() : images.get(0));
            }
            this.skuMapper.insert(sku);
            //获取skuId
            Long skuId = sku.getId();

            //2.2保存sku的图片信息 pms_spu_images
            if(!CollectionUtils.isEmpty(images)){


                skuImagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(sku.getDefaultImage(),image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }

            //2.3. 保存sku的规格参数（销售属性）pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            saleAttrs.forEach(saleAttr->{
                //设置属性名,根据id查询AttrEntity
                saleAttr.setSkuId(skuId);
            });
            this.skuAttrValueService.saveBatch(saleAttrs);


            //3保存营销相关信息,需要远程调用gmall-sms
            //3.1积分优惠 sms_sku_bounds
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(sku ,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.saveSkuSales(skuSaleVo);


            //3.2满减优惠 sms_sku_full_reduction
            //3.3数量折扣 sms_sku_ladder
        });
    }

    public void saveBaseAttr(SpuVo spuVo, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){

            this.spuAttrValueService.saveBatch( baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo,spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }
    }

    public void saveSpuDesc(SpuVo spuVo, Long spuId) {
        List<String> spuImages = spuVo.getSpuImages();

        if (!CollectionUtils.isEmpty(spuImages)){
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            //spu_info_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
            spuDescEntity.setSpuId(spuId);
            //把商品的图片描述,保存到spu详情中,图片地址以逗号进行分割
            spuDescEntity.setDecript(StringUtils.join(spuImages ,","));
            this.spuDescMapper.insert(spuDescEntity);
        }
    }

    public Long saveSpu(SpuVo spuVo) {
        //spuVo.setPublishStatus(1);//默认已上架
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());//更新时间默认和创建时间一样
        this.save(spuVo);
        return spuVo.getId();
    }

}