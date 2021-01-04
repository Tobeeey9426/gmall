package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 属性分组
 *
 * @author Tobeey
 * @email 1340798171@qq.com
 * @date 2020-12-08 13:47:26
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {

    @Autowired
    private AttrGroupService attrGroupService;

    @GetMapping("withattr/value/category/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrsAndValuesByCidAndSpuIdAndskuId(
            @PathVariable("cid") Long cid,
            @RequestParam("skuId")Long skuId,
            @RequestParam("spuId")Long spuId
    ){
        List<ItemGroupVo> groupVos = this.attrGroupService
                .queryGroupWithAttrsAndValuesByCidAndSpuIdAndskuId(cid,skuId,spuId);

        return ResponseVo.ok(groupVos);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryAttrGroupByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = attrGroupService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<AttrGroupEntity> queryAttrGroupById(@PathVariable("id") Long id){
		AttrGroupEntity attrGroup = attrGroupService.getById(id);

        return ResponseVo.ok(attrGroup);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		attrGroupService.removeByIds(ids);

        return ResponseVo.ok();
    }

    @ApiOperation("根据三级分类id查询")
    @GetMapping("category/{cid}")
    public ResponseVo<List<AttrGroupEntity>> queryByCidPage(
            @PathVariable("cid")Long cid){
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id",cid);
        List<AttrGroupEntity> groupEntities = this.attrGroupService.list(queryWrapper);
        return ResponseVo.ok(groupEntities);
    }

    @ApiOperation("查询分类下的组及规格参数")
    @GetMapping("withattrs/{catId}")
    public ResponseVo<List<AttrGroupEntity>> queryGroupWithAttrByCatId(
            @PathVariable("catId") Long catId){
        List<AttrGroupEntity> attrGroupEntities = this.attrGroupService.queryGroupWithAttrByCatId(catId);
        return ResponseVo.ok(attrGroupEntities);
    }

}
