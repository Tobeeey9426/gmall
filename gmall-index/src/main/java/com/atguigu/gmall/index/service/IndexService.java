package com.atguigu.gmall.index.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexService {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:cates";


    public List<CategoryEntity> queryLv1CategoriesById(){

        ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.queryCategory(0L);

        return responseVo.getData();
    }

    @GmallCache(prefix = KEY_PREFIX,timeout = 24*60*30 ,random = 24*60*3, lock = "index:lock")
    public List<CategoryEntity> querylv2CategoriesWithSubByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubsByPid(pid);
        List<CategoryEntity> data = listResponseVo.getData();

        return data;
    }
}
