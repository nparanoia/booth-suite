package com.booth.product.service;

import com.booth.common.core.exception.BizException;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.product.convert.ProductConvert;
import com.booth.product.dto.ProductQuery;
import com.booth.product.entity.Product;
import com.booth.product.enums.PublishStatus;
import com.booth.product.error.ProductErrorCode;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.vo.ProductDetailVO;
import com.booth.product.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 前台浏览，只能看到已上架的商品。关键字搜索这里只做简单模糊匹配，正式搜索走搜索服务。
 */
@Service
@RequiredArgsConstructor
public class ProductBrowseService {

    private final ProductMapper productMapper;
    private final ProductAssembler assembler;
    private final ProductConvert productConvert;
    private final ProductViewCache viewCache;

    public PageResult<ProductListVO> page(ProductQuery query) {
        query.setPublishStatus(PublishStatus.ON_SHELF);
        return PageResults.of(productMapper.selectPage(query.toPage(), ProductService.buildQuery(query)),
                productConvert::toListVO);
    }

    public ProductDetailVO detail(Long id) {
        ProductDetailVO detail = viewCache.detail(id, () -> {
            Product product = productMapper.selectById(id);
            if (product == null || product.getPublishStatus() != PublishStatus.ON_SHELF) {
                return null;
            }
            return assembler.toDetail(product, true);
        });
        BizException.throwIf(detail == null, ProductErrorCode.PRODUCT_NOT_FOUND);
        return detail;
    }
}
