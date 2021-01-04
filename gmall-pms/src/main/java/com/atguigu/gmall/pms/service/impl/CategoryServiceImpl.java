package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {



    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryEntity> queryCategory(Long parentId) {
        //构造查询条件
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        //如果 parentId = -1,说明用户没有传该字段, 查询所有
        if (parentId != -1){
            wrapper.eq("parent_id",parentId);
        }

        return this.categoryMapper.selectList(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSubsByPid(Long pid) {
        return this.categoryMapper.queryCategoriesWithSubsByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryLvl123CategoriesByCid3(Long id) {
        //查询三级分类
        CategoryEntity lvl3Category = this.getById(id);
        if (lvl3Category == null){
            return null;
        }
        //查询二级分类
        CategoryEntity lvl2Category = this.getById(lvl3Category.getParentId());
        //查询一级分类
        CategoryEntity lvl1Category = this.getById(lvl2Category.getParentId());


        return Arrays.asList(lvl1Category,lvl2Category,lvl3Category);
    }

}