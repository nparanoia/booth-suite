package com.booth.product.service;

import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.product.entity.Product;
import com.booth.product.entity.Sku;
import com.booth.product.enums.PublishStatus;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuInnerService {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;

    /**
     * 商品被逻辑删除后 selectByIds 查不到它，对应 SKU 按"未上架"返回，调用方据此判断不可购买。
     */
    public List<SkuInfoDTO> listByIds(Collection<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        List<Sku> skus = skuMapper.selectByIds(skuIds);
        if (skus.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> products = productMapper.selectByIds(skus.stream().map(Sku::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        return skus.stream().map(sku -> toDTO(sku, products.get(sku.getProductId()))).toList();
    }

    private SkuInfoDTO toDTO(Sku sku, Product product) {
        boolean onSale = product != null && product.getPublishStatus() == PublishStatus.ON_SHELF;
        String pic = StringUtils.hasText(sku.getPic()) || product == null ? sku.getPic() : product.getPic();
        return new SkuInfoDTO(
                sku.getId(),
                sku.getProductId(),
                sku.getSkuCode(),
                product == null ? null : product.getName(),
                pic,
                sku.getSpecData(),
                sku.getPrice(),
                sku.availableStock(),
                onSale,
                product == null ? null : product.getCategoryId(),
                product == null ? null : product.getBrandName(),
                product == null || product.getGiftIntegration() == null ? 0 : product.getGiftIntegration());
    }
}
