package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.Set;

@Data
public class SaleAttrValueVo {

    private Long attrId;
    private String attrName;
    //attr可选值
    private Set<String> attrValues;
}
