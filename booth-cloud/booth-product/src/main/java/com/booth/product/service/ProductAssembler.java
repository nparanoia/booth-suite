package com.booth.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.product.convert.ProductConvert;
import com.booth.product.entity.Attr;
import com.booth.product.entity.Product;
import com.booth.product.entity.ProductAttrValue;
import com.booth.product.entity.Sku;
import com.booth.product.mapper.ProductAttrValueMapper;
import com.booth.product.mapper.SkuMapper;
import com.booth.product.vo.ProductDetailVO;
import com.booth.product.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 后台和前台的商品详情结构相同，区别只在 SKU 是否带库存明细。
 */
@Component
@RequiredArgsConstructor
class ProductAssembler {

    private final SkuMapper skuMapper;
    private final ProductAttrValueMapper attrValueMapper;
    private final AttrService attrService;
    private final ProductConvert productConvert;

    ProductDetailVO toDetail(Product product, boolean forOpen) {
        List<ProductAttrValue> values = attrValueMapper.selectList(Wrappers.<ProductAttrValue>lambdaQuery()
                .eq(ProductAttrValue::getProductId, product.getId()));
        Map<Long, Attr> attrs = attrService.mapByIds(values.stream().map(ProductAttrValue::getAttrId).toList());
        List<ProductDetailVO.ParamVO> params = values.stream()
                .filter(v -> attrs.containsKey(v.getAttrId()))
                .map(v -> new ProductDetailVO.ParamVO(v.getAttrId(), attrs.get(v.getAttrId()).getName(), v.getValue()))
                .toList();

        List<SkuVO> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                        .eq(Sku::getProductId, product.getId())
                        .orderByAsc(Sku::getId))
                .stream()
                .map(productConvert::toSkuVO)
                .map(vo -> forOpen ? vo.forOpen() : vo)
                .toList();
        return productConvert.toDetailVO(product, params, skus);
    }
}
