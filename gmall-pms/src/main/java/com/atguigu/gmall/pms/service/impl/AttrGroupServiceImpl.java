package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrMapper attrMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<AttrGroupEntity> queryGroupWithAttrByCatId(Long catId) {



        //根据分类id查询分组
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id" ,catId);
        List<AttrGroupEntity> attrGroupEntities = this.list(queryWrapper);

        if (CollectionUtils.isEmpty(attrGroupEntities)){
            return null;
        }

        //遍历分组,查询每组下面的规格参数
        attrGroupEntities.forEach(group->{
            // 查询规格参数，只需查询出每个分组下的通用属性就可以了（不需要销售属性
            List<AttrEntity> attrEntities = this.attrMapper.selectList(
                    new QueryWrapper<AttrEntity>()
                    .eq("group_id", group.getId())
                    .eq("type", 1));
            group.setAttrEntities(attrEntities);
        });

        return  attrGroupEntities;
    }

    @Override
    public List<ItemGroupVo> queryGroupWithAttrsAndValuesByCidAndSpuIdAndskuId(Long cid, Long skuId, Long spuId) {
        // 1.根据分类id查询分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id",cid));
        if (CollectionUtils.isEmpty(attrGroupEntities)){
            return null;
        }

        // 2.遍历每一个分组查询组下的规格参数
        return attrGroupEntities.stream().map(groupEntity -> {
            ItemGroupVo groupVo = new ItemGroupVo();
            groupVo.setId(groupEntity.getId());
            groupVo.setName(groupEntity.getName());

            // 查询组下的规格参数
            List<AttrEntity> attrEntities =this.attrMapper
                    .selectList(new QueryWrapper<AttrEntity>().eq("group_id",groupEntity.getId()));
            if (!CollectionUtils.isEmpty(attrEntities)){
                //获取规格参数的id集合
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

                //存放两个List属性的List集合
                List<AttrValueVo> bigAttrValueVos = new ArrayList<>();

                // 3.查询销售属性的规格参数及值
                List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper
                        .selectList(new QueryWrapper<SkuAttrValueEntity>()
                        .in("attr_id",attrIds)
                        .eq("sku_id",skuId));
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    bigAttrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity,attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                // 4.查询基本属性的规格参数及值
                List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper
                        .selectList(new QueryWrapper<SpuAttrValueEntity>()
                        .in("attr_id",attrIds)
                        .eq("spu_id",spuId));
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    bigAttrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity,attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                groupVo.setAttrs(bigAttrValueVos);
            }

            return  groupVo;
        }).collect(Collectors.toList());
    }

}