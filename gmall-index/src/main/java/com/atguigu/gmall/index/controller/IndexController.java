package com.atguigu.gmall.index.controller;


import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping({"index.html","/"})
    public String toIndex(Model model , HttpServletRequest request){
        System.out.println(request.getHeader("userId")+ "==============");

        List<CategoryEntity> categoryEntities = this.indexService.queryLv1CategoriesById();
        model.addAttribute("categories",categoryEntities);

        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    //代表返回是真实数据 要不会认为是视图名称
    public ResponseVo<List<CategoryEntity>> querylv2CategoriesWithSubByPid(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.querylv2CategoriesWithSubByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }


}
