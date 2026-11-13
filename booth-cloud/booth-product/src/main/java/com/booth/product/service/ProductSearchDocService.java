package com.booth.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.product.dto.ProductIdQuery;
import com.booth.api.product.dto.ProductSearchDocDTO;
import com.booth.api.product.dto.ProductSearchExportDTO;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.exception.CommonErrorCode;
import com.booth.product.entity.Attr;
import com.booth.product.entity.Product;
import com.booth.product.entity.ProductAttrValue;
import com.booth.product.entity.Sku;
import com.booth.product.enums.PublishStatus;
import com.booth.product.mapper.ProductAttrValueMapper;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组装搜索文档。版本号取商品、SKU、参数里最大的 update_time，旧消息带着更小的版本写不回去。
 */
@Service
@RequiredArgsConstructor
public class ProductSearchDocService {

    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final int EXPORT_MAX = 200;

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final ProductAttrValueMapper attrValueMapper;
    private final AttrService attrService;
    private final JdbcTemplate jdbcTemplate;

    public List<ProductSearchDocDTO> docs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        BizException.throwIf(ids.size() > EXPORT_MAX, CommonErrorCode.PARAM_INVALID, "一次最多查询 200 个商品");
        List<RawProduct> raws = selectRaw(ids);
        if (raws.isEmpty()) {
            return List.of();
        }
        List<Long> foundIds = raws.stream().map(RawProduct::id).toList();
        Related related = loadRelated(foundIds);
        List<Long> onShelfIds = raws.stream().filter(RawProduct::onShelf).map(RawProduct::id).toList();
        Map<Long, Product> onShelf = onShelfIds.isEmpty() ? Map.of()
                : productMapper.selectByIds(onShelfIds).stream()
                .filter(product -> product.getPublishStatus() == PublishStatus.ON_SHELF)
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<ProductSearchDocDTO> result = new ArrayList<>();
        for (RawProduct raw : raws) {
            Product product = onShelf.get(raw.id());
            if (product == null) {
                result.add(tombstone(raw, related));
            } else {
                result.add(toDoc(product, related));
            }
        }
        return result;
    }

    public ProductSearchExportDTO export(long afterId, int size) {
        BizException.throwIf(afterId < 0 || size < 1 || size > EXPORT_MAX, CommonErrorCode.PARAM_INVALID);
        List<Product> products = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .eq(Product::getPublishStatus, PublishStatus.ON_SHELF)
                .gt(Product::getId, afterId)
                .orderByAsc(Product::getId)
                .last("LIMIT " + size));
        if (products.isEmpty()) {
            return new ProductSearchExportDTO(List.of(), afterId, false);
        }
        Related related = loadRelated(products.stream().map(Product::getId).toList());
        List<ProductSearchDocDTO> docs = products.stream().map(product -> toDoc(product, related)).toList();
        long next = products.get(products.size() - 1).getId();
        return new ProductSearchExportDTO(docs, next, products.size() == size);
    }

    public List<Long> ids(ProductIdQuery query) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (query.brandIds() != null && !query.brandIds().isEmpty()) {
            productMapper.selectList(Wrappers.<Product>lambdaQuery()
                            .select(Product::getId)
                            .in(Product::getBrandId, query.brandIds()))
                    .forEach(product -> ids.add(product.getId()));
        }
        if (query.categoryIds() != null && !query.categoryIds().isEmpty()) {
            productMapper.selectList(Wrappers.<Product>lambdaQuery()
                            .select(Product::getId)
                            .in(Product::getCategoryId, query.categoryIds()))
                    .forEach(product -> ids.add(product.getId()));
        }
        return List.copyOf(ids);
    }

    private ProductSearchDocDTO toDoc(Product product, Related related) {
        List<ProductAttrValue> values = related.values().getOrDefault(product.getId(), List.of());
        List<Sku> skus = related.skus().getOrDefault(product.getId(), List.of());
        return new ProductSearchDocDTO(
                product.getId(),
                true,
                version(product.getUpdateTime(), skus, values),
                product.getName(),
                product.getSubTitle(),
                product.getKeywords(),
                product.getBrandId(),
                product.getCategoryId(),
                product.getBrandName(),
                product.getCategoryName(),
                product.getPrice() == null ? BigDecimal.ZERO : product.getPrice(),
                product.getSale() == null ? 0 : product.getSale(),
                picture(product),
                epoch(product.getCreateTime()),
                attrs(values, related.attrs()));
    }

    private ProductSearchDocDTO tombstone(RawProduct raw, Related related) {
        List<ProductAttrValue> values = related.values().getOrDefault(raw.id(), List.of());
        List<Sku> skus = related.skus().getOrDefault(raw.id(), List.of());
        long version = version(raw.updateTime(), skus, values);
        return new ProductSearchDocDTO(raw.id(), false, version, null, null, null, null, null, null, null, null, null,
                null, null, List.of());
    }

    private List<ProductSearchDocDTO.AttrDTO> attrs(List<ProductAttrValue> values, Map<Long, Attr> attrs) {
        return values.stream()
                .filter(value -> attrs.containsKey(value.getAttrId()))
                .map(value -> new ProductSearchDocDTO.AttrDTO(attrs.get(value.getAttrId()).getName(), value.getValue()))
                .toList();
    }

    private Related loadRelated(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return new Related(Map.of(), Map.of(), Map.of());
        }
        List<Sku> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery().in(Sku::getProductId, productIds));
        List<ProductAttrValue> values = attrValueMapper.selectList(Wrappers.<ProductAttrValue>lambdaQuery()
                .in(ProductAttrValue::getProductId, productIds));
        Map<Long, Attr> attrs = attrService.mapByIds(values.stream().map(ProductAttrValue::getAttrId).distinct().toList());
        return new Related(
                skus.stream().collect(Collectors.groupingBy(Sku::getProductId)),
                values.stream().collect(Collectors.groupingBy(ProductAttrValue::getProductId)),
                attrs);
    }

    /**
     * 逻辑删除的行也要能读到，否则索引里会留下已删除的商品。JdbcTemplate 不走 MyBatis 的逻辑删除插件。
     */
    private List<RawProduct> selectRaw(List<Long> ids) {
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return jdbcTemplate.query(
                "SELECT id, publish_status, deleted, update_time FROM product WHERE id IN (" + placeholders + ")",
                (rs, row) -> mapRaw(rs),
                ids.toArray());
    }

    private static RawProduct mapRaw(ResultSet rs) throws SQLException {
        int deleted = rs.getInt("deleted");
        int publish = rs.getInt("publish_status");
        return new RawProduct(rs.getLong("id"), deleted == 0 && publish == PublishStatus.ON_SHELF.getCode(),
                rs.getObject("update_time", LocalDateTime.class));
    }

    private static long version(LocalDateTime productTime, List<Sku> skus, List<ProductAttrValue> values) {
        long version = epoch(productTime);
        for (Sku sku : skus) {
            version = Math.max(version, epoch(sku.getUpdateTime()));
        }
        for (ProductAttrValue value : values) {
            version = Math.max(version, epoch(value.getUpdateTime()));
        }
        return version;
    }

    static long epoch(LocalDateTime time) {
        if (time == null) {
            return 1L;
        }
        return Math.max(1L, time.atZone(ZONE).toInstant().toEpochMilli());
    }

    private static String picture(Product product) {
        if (StringUtils.hasText(product.getPic())) {
            return product.getPic();
        }
        if (!StringUtils.hasText(product.getAlbumPics())) {
            return null;
        }
        int comma = product.getAlbumPics().indexOf(',');
        return comma < 0 ? product.getAlbumPics() : product.getAlbumPics().substring(0, comma);
    }

    private record RawProduct(long id, boolean onShelf, LocalDateTime updateTime) {
    }

    private record Related(Map<Long, List<Sku>> skus, Map<Long, List<ProductAttrValue>> values, Map<Long, Attr> attrs) {
    }
}
