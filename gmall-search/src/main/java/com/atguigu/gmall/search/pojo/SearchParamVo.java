package com.atguigu.gmall.search.pojo;


import lombok.Data;

import java.util.List;
/**
 * ?keyword=手机&brandId=1,2&categoryId=225&props=4:8G-12G&props=5:128G-256G
 *  &priceFrom=1000&priceTo=3000&store=true&sort=1&pageNum=2
 */
@Data
public class SearchParamVo {

    //检索条件
    private String keyword;
    //品牌过滤
    private List<Long> brandId;
    //分类过滤
    private List<Long> categoryId;
    //过滤检索的参数 ["4:8G-12G"]
    private List<String> props;

    //排序 0-默认排序,得分降序, 1-价格降序 2-价格升序 3-销量的降序 4-新品降序
    private Integer sort;

    //价格区间
    private Double priceFrom;
    private Double priceTo;

    //默认页码
    private Integer pageNum = 1;
    //默认每页记录数
    private final Integer pageSize = 20;

    //是否有货
    private Boolean store;

}
