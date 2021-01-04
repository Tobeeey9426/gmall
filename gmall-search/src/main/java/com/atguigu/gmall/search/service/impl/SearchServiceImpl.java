package com.atguigu.gmall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public SearchResponseVo search(SearchParamVo paramVo) {

        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, buildDsl(paramVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析结果集
            SearchResponseVo responseVo = this.parseResult(response);
            //从查询条件中获取分页数据
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());

            return  responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private SearchSourceBuilder buildDsl (SearchParamVo paramVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = paramVo.getKeyword();
        if (StringUtils.isEmpty(keyword)){
            // TODO: 打广告
            return sourceBuilder;
        }

        //1.构建查询及过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        //1.1 构建查询条件 匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));

        //1.2 构建过滤条件
        //1.2.1 品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }

        //1.2.2 分类过滤
        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",categoryId));
        }

        //1.2.3 价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceFrom != null){
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null){
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }

        //1.2.4 是否有货过滤
        Boolean store = paramVo.getStore();
        if (store != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }

        //1.2.5 规格参数过滤
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)){
            props.forEach(prop->{

                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length ==2){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    // 规格参数id 单词条 查询条件
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attr[0]));
                    // 规格参数值 多词条查询条件
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));

                }
            });
        }
        

        //2.排序
        Integer sort = paramVo.getSort();
        if (sort != null){
            switch (sort){
                case 1: sourceBuilder.sort("price" , SortOrder.DESC); break;
                case 2: sourceBuilder.sort("price" , SortOrder.ASC); break;
                case 3: sourceBuilder.sort("sales" , SortOrder.DESC); break;
                case 4: sourceBuilder.sort("createTime" , SortOrder.DESC); break;
                default:
                    break;
            }
        }


        //3.分页
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        //4.高亮
        sourceBuilder.highlighter(
                new HighlightBuilder()
                .field("title")
                .preTags("<font style='color:red'>")
                .postTags("</font>"));

        //5.聚合
        //5.1 品牌的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo"))
        );
        //5.2 分类的聚合
        sourceBuilder.aggregation(
                AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                    .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );

        //5.3 规格参数的嵌套聚合
        sourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg","searchAttrs")
                    .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                            .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                            .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))

        );

        //6 结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId","defaultImage","title","subTitle","price"},null);

        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

    private SearchResponseVo parseResult (SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();

        // 解析hits
        SearchHits hits = response.getHits();
        //总命中记录数
        long totalHits = hits.totalHits;
        responseVo.setTotal(totalHits);

        //解析出当前页的数据
        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
            String json = hitsHit.getSourceAsString();
            //没有高亮
            //把_source反序列化为goods对象
            Goods goods = JSON.parseObject(json, Goods.class);

            //获取高亮结果集替换掉普通title
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            Text[] fragments = highlightField.getFragments();
            goods.setTitle(fragments[0].string());
            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        // 解析聚合结果集
        // 获取集合结果集 ,以map形式接收
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();

        //获取品牌Id聚合
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandBuckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(brandBuckets)){

            //设置品牌参数
            responseVo.setBrands(brandBuckets.stream().map(bucket ->{
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                //获取桶里的子聚合
                Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                //解析品牌名称 子聚合获取品牌名称
                ParsedStringTerms brandNameAgg = (ParsedStringTerms)subAggregationMap.get("brandNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = brandNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)){
                    // 桶里的第一个key的字段
                    String nameKey = nameAggBuckets.get(0).getKeyAsString();
                    brandEntity.setName(nameKey);
                }
                //解析logo子聚合 获取logo

                ParsedStringTerms logoAgg = (ParsedStringTerms)subAggregationMap.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoAggBuckets)){
                    // 桶里的第一个key的logo字段
                    String logoKey = logoAggBuckets.get(0).getKeyAsString();
                    brandEntity.setLogo(logoKey);
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }

        //获取分类聚合
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryBuckets)){

            //设置分类参数
            responseVo.setCategories(categoryBuckets.stream().map(bucket->{
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 通过子聚合获取分类名称
                ParsedStringTerms categoryNameAgg = ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)){
                    categoryEntity.setName(nameAggBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        // 获取规格参数聚合并解析出规格参数的过滤列表
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //获取 attrAgg嵌套聚合的子聚合
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)){

            //设置规格参数
            responseVo.setFilters(attrIdAggBuckets.stream().map(bucket->{
                SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();

                //获取桶中的key 就是attr的id
                responseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                //根据attrIdAgg获取所有的子聚合 attrName attrValue
                Map<String, Aggregation> subAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
                //获取规格参数名称的子聚合,解析出规格参数参数名
                ParsedStringTerms attrNameAgg = (ParsedStringTerms)subAggregationMap.get("attrNameAgg");
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(nameAggBuckets)){
                    //设置attrName
                    responseAttrVo.setAttrName(nameAggBuckets.get(0).getKeyAsString());
                }

                //获取规格参数值的子聚合,解析出规格参数可选值
                ParsedStringTerms attrValueAgg = (ParsedStringTerms) subAggregationMap.get("attrValueAgg");
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(valueAggBuckets)){

                    responseAttrVo.setAttrValues( valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }



                return responseAttrVo;
            }).collect(Collectors.toList()));
        }


        return responseVo;
    }
}
