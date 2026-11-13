package com.booth.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.exception.CommonErrorCode;
import com.booth.search.dto.ProductSearchQuery;
import com.booth.search.enums.SearchSort;
import com.booth.search.error.SearchErrorCode;
import com.booth.search.support.SearchIndexNames;
import com.booth.search.vo.ProductSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final long WINDOW = 10_000;

    private final ElasticsearchClient client;

    public ProductSearchVO search(ProductSearchQuery query) {
        if (query.getPriceMin() != null && query.getPriceMax() != null
                && query.getPriceMin().compareTo(query.getPriceMax()) > 0) {
            throw new BizException(CommonErrorCode.PARAM_INVALID, "最低价不能高于最高价");
        }
        if (query.getPageNum() > WINDOW || query.getPageNum() * query.getPageSize() > WINDOW) {
            throw new BizException(SearchErrorCode.PAGE_TOO_DEEP);
        }
        List<AttrFilter> attrs = parseAttrs(query.getAttrs());
        int from = (int) ((query.getPageNum() - 1) * query.getPageSize());
        int size = (int) query.getPageSize();
        SearchResponse<ProductIndexDoc> response;
        try {
            response = client.search(request -> {
                request.index(SearchIndexNames.ALIAS)
                        .from(from)
                        .size(size)
                        .query(buildQuery(query, attrs))
                        .highlight(highlight -> highlight.fields("name", field ->
                                field.preTags("<em>").postTags("</em>").numberOfFragments(0)))
                        .aggregations("brands", agg -> agg.terms(terms -> terms.field("brandId").size(30))
                                .aggregations("name", name -> name.terms(terms -> terms.field("brandName").size(1))))
                        .aggregations("categories", agg -> agg.terms(terms -> terms.field("categoryId").size(30))
                                .aggregations("name", name -> name.terms(terms -> terms.field("categoryName").size(1))))
                        .aggregations("attrs", agg -> agg.nested(nested -> nested.path("attrs"))
                                .aggregations("names", names -> names.terms(terms -> terms.field("attrs.name").size(20))
                                        .aggregations("values", values -> values.terms(terms ->
                                                terms.field("attrs.value").size(20)))));
                applySort(request, query.getSort());
                return request;
            }, ProductIndexDoc.class);
        } catch (IOException e) {
            throw new IllegalStateException("搜索引擎访问失败", e);
        }
        List<ProductSearchVO.Item> list = new ArrayList<>();
        for (Hit<ProductIndexDoc> hit : response.hits().hits()) {
            ProductIndexDoc doc = hit.source();
            if (doc == null) {
                continue;
            }
            List<String> highlights = hit.highlight() == null ? null : hit.highlight().get("name");
            String highlight = highlights == null || highlights.isEmpty() ? doc.name() : highlights.get(0);
            list.add(new ProductSearchVO.Item(doc.id(), doc.name(), highlight, doc.subTitle(), doc.pic(),
                    doc.price(), doc.sale(), doc.brandName(), doc.categoryName()));
        }
        long total = response.hits().total() == null ? list.size() : response.hits().total().value();
        return new ProductSearchVO(total, query.getPageNum(), query.getPageSize(), list,
                brandBuckets(response), categoryBuckets(response), attrBuckets(response));
    }

    private static Query buildQuery(ProductSearchQuery query, List<AttrFilter> attrs) {
        return Query.of(root -> root.bool(bool -> {
            if (StringUtils.hasText(query.getKeyword())) {
                bool.must(must -> must.multiMatch(match -> match
                        .query(query.getKeyword())
                        .fields("name^3", "subTitle", "keywords")));
            } else {
                bool.must(must -> must.matchAll(match -> match));
            }
            if (query.getCategoryId() != null) {
                bool.filter(filter -> filter.term(term -> term.field("categoryId").value(query.getCategoryId())));
            }
            if (query.getBrandId() != null) {
                bool.filter(filter -> filter.term(term -> term.field("brandId").value(query.getBrandId())));
            }
            if (query.getPriceMin() != null || query.getPriceMax() != null) {
                bool.filter(filter -> filter.range(range -> range.number(number -> {
                    number.field("price");
                    if (query.getPriceMin() != null) {
                        number.gte(query.getPriceMin().doubleValue());
                    }
                    if (query.getPriceMax() != null) {
                        number.lte(query.getPriceMax().doubleValue());
                    }
                    return number;
                })));
            }
            for (AttrFilter attr : attrs) {
                bool.filter(filter -> filter.nested(nested -> nested.path("attrs").query(inner -> inner.bool(innerBool ->
                        innerBool.filter(name -> name.term(term -> term.field("attrs.name").value(attr.name())))
                                .filter(value -> value.term(term -> term.field("attrs.value").value(attr.value())))))));
            }
            return bool;
        }));
    }

    private static void applySort(co.elastic.clients.elasticsearch.core.SearchRequest.Builder request, SearchSort sort) {
        SearchSort actual = sort == null ? SearchSort.DEFAULT : sort;
        switch (actual) {
            case SALE -> request.sort(item -> item.field(field -> field.field("sale").order(SortOrder.Desc)));
            case PRICE_ASC -> request.sort(item -> item.field(field -> field.field("price").order(SortOrder.Asc)));
            case PRICE_DESC -> request.sort(item -> item.field(field -> field.field("price").order(SortOrder.Desc)));
            case NEW -> request.sort(item -> item.field(field -> field.field("createTime").order(SortOrder.Desc)));
            default -> request.sort(item -> item.score(score -> score.order(SortOrder.Desc)))
                    .sort(item -> item.field(field -> field.field("sale").order(SortOrder.Desc)));
        }
    }

    private static List<ProductSearchVO.Bucket> brandBuckets(SearchResponse<ProductIndexDoc> response) {
        if (response.aggregations() == null || !response.aggregations().containsKey("brands")) {
            return List.of();
        }
        List<ProductSearchVO.Bucket> buckets = new ArrayList<>();
        for (LongTermsBucket bucket : response.aggregations().get("brands").lterms().buckets().array()) {
            buckets.add(new ProductSearchVO.Bucket(bucket.key(), subName(bucket), bucket.docCount()));
        }
        return buckets;
    }

    private static List<ProductSearchVO.Bucket> categoryBuckets(SearchResponse<ProductIndexDoc> response) {
        if (response.aggregations() == null || !response.aggregations().containsKey("categories")) {
            return List.of();
        }
        List<ProductSearchVO.Bucket> buckets = new ArrayList<>();
        for (LongTermsBucket bucket : response.aggregations().get("categories").lterms().buckets().array()) {
            buckets.add(new ProductSearchVO.Bucket(bucket.key(), subName(bucket), bucket.docCount()));
        }
        return buckets;
    }

    private static List<ProductSearchVO.AttrBucket> attrBuckets(SearchResponse<ProductIndexDoc> response) {
        if (response.aggregations() == null || !response.aggregations().containsKey("attrs")) {
            return List.of();
        }
        List<ProductSearchVO.AttrBucket> buckets = new ArrayList<>();
        var names = response.aggregations().get("attrs").nested().aggregations().get("names").sterms();
        for (StringTermsBucket name : names.buckets().array()) {
            List<ProductSearchVO.ValueBucket> values = new ArrayList<>();
            for (StringTermsBucket value : name.aggregations().get("values").sterms().buckets().array()) {
                values.add(new ProductSearchVO.ValueBucket(value.key().stringValue(), value.docCount()));
            }
            buckets.add(new ProductSearchVO.AttrBucket(name.key().stringValue(), values));
        }
        return buckets;
    }

    private static String subName(LongTermsBucket bucket) {
        if (bucket.aggregations() == null || !bucket.aggregations().containsKey("name")) {
            return "";
        }
        var terms = bucket.aggregations().get("name").sterms().buckets().array();
        return terms.isEmpty() ? "" : terms.get(0).key().stringValue();
    }

    private static List<AttrFilter> parseAttrs(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<AttrFilter> filters = new ArrayList<>();
        for (String item : raw) {
            if (!StringUtils.hasText(item) || !item.contains(":")) {
                throw new BizException(CommonErrorCode.PARAM_INVALID, "属性筛选格式为 名称:值");
            }
            int split = item.indexOf(':');
            String name = item.substring(0, split).trim();
            String value = item.substring(split + 1).trim();
            if (name.isEmpty() || value.isEmpty()) {
                throw new BizException(CommonErrorCode.PARAM_INVALID, "属性筛选格式为 名称:值");
            }
            filters.add(new AttrFilter(name, value));
        }
        return filters;
    }

    private record AttrFilter(String name, String value) {
    }
}
