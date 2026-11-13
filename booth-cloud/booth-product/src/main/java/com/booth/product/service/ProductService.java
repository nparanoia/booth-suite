package com.booth.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.exception.CommonErrorCode;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.product.convert.ProductConvert;
import com.booth.product.dto.ProductCreateRequest;
import com.booth.product.dto.ProductQuery;
import com.booth.product.dto.ProductSaveRequest;
import com.booth.product.dto.PublishRequest;
import com.booth.product.dto.SkuCreateRequest;
import com.booth.product.dto.SkuUpdateRequest;
import com.booth.product.entity.Brand;
import com.booth.product.entity.Category;
import com.booth.product.entity.Product;
import com.booth.product.entity.ProductAttrValue;
import com.booth.product.entity.Sku;
import com.booth.product.enums.PublishStatus;
import com.booth.product.error.ProductErrorCode;
import com.booth.product.mapper.ProductAttrValueMapper;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.mapper.SkuMapper;
import com.booth.product.support.AfterCommit;
import com.booth.product.vo.ProductDetailVO;
import com.booth.product.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 后台商品维护。所有写操作都在事务内完成，商品、SKU、参数要么一起成功，要么一起回滚。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final ProductAttrValueMapper attrValueMapper;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final AttrService attrService;
    private final ProductAssembler assembler;
    private final ProductConvert productConvert;
    private final ProductViewCache viewCache;

    public PageResult<ProductListVO> page(ProductQuery query) {
        return PageResults.of(productMapper.selectPage(query.toPage(), buildQuery(query)), productConvert::toListVO);
    }

    public ProductDetailVO detail(Long id) {
        return assembler.toDetail(require(id), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductCreateRequest request) {
        List<SkuCreateRequest> skuRequests = request.skus();
        Set<String> codes = new HashSet<>();
        skuRequests.forEach(s -> BizException.throwIf(!codes.add(s.skuCode()), ProductErrorCode.SKU_CODE_EXISTS,
                "SKU 编码重复：" + s.skuCode()));

        Product product = productConvert.toEntity(request.product());
        fillRelations(product, request.product());
        product.setPrice(skuRequests.stream().map(SkuCreateRequest::price).min(Comparator.naturalOrder()).orElseThrow());
        product.setPublishStatus(PublishStatus.OFF_SHELF);
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException e) {
            throw new BizException(ProductErrorCode.PRODUCT_SN_EXISTS);
        }

        List<Sku> skus = skuRequests.stream().map(s -> {
            Sku sku = productConvert.toEntity(s);
            sku.setProductId(product.getId());
            return sku;
        }).toList();
        try {
            skuMapper.insert(skus);
        } catch (DuplicateKeyException e) {
            throw new BizException(ProductErrorCode.SKU_CODE_EXISTS);
        }
        saveParams(product.getId(), request.product());
        return product.getId();
    }

    /**
     * 只改商品信息和参数，不动 SKU，已锁定的库存不受影响。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductSaveRequest request) {
        Product product = require(id);
        productConvert.update(product, request);
        fillRelations(product, request);
        try {
            productMapper.updateById(product);
        } catch (DuplicateKeyException e) {
            throw new BizException(ProductErrorCode.PRODUCT_SN_EXISTS);
        }
        attrValueMapper.delete(Wrappers.<ProductAttrValue>lambdaQuery().eq(ProductAttrValue::getProductId, id));
        saveParams(id, request);
        AfterCommit.run(() -> viewCache.evictProduct(id));
    }

    public void publish(PublishRequest request) {
        productMapper.update(Wrappers.<Product>lambdaUpdate()
                .set(Product::getPublishStatus, request.status())
                .in(Product::getId, request.ids()));
        request.ids().forEach(viewCache::evictProduct);
    }

    /**
     * 逻辑删除。上架中的商品可能正在被下单，必须先下架。
     */
    public void delete(Long id) {
        Product product = require(id);
        BizException.throwIf(product.getPublishStatus() == PublishStatus.ON_SHELF, ProductErrorCode.PRODUCT_ON_SHELF);
        productMapper.deleteById(id);
        viewCache.evictProduct(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addSku(Long productId, SkuCreateRequest request) {
        require(productId);
        Sku sku = productConvert.toEntity(request);
        sku.setProductId(productId);
        try {
            skuMapper.insert(sku);
        } catch (DuplicateKeyException e) {
            throw new BizException(ProductErrorCode.SKU_CODE_EXISTS);
        }
        refreshPrice(productId);
        AfterCommit.run(() -> viewCache.evictProduct(productId));
        return sku.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSku(Long skuId, SkuUpdateRequest request) {
        Sku sku = requireSku(skuId);
        productConvert.update(sku, request);
        skuMapper.updateById(sku);
        refreshPrice(sku.getProductId());
        Long productId = sku.getProductId();
        AfterCommit.run(() -> viewCache.evictProduct(productId));
    }

    /**
     * 直接 SET stock = ? 会和并发下单的锁库存互相覆盖，所以用条件更新：
     * 新库存必须不小于当前锁定量，判断和写入在一条 SQL 里完成。
     */
    public void updateStock(Long skuId, int stock) {
        int rows = skuMapper.update(Wrappers.<Sku>lambdaUpdate()
                .set(Sku::getStock, stock)
                .eq(Sku::getId, skuId)
                .le(Sku::getLockStock, stock));
        if (rows == 0) {
            requireSku(skuId);
            throw new BizException(ProductErrorCode.STOCK_BELOW_LOCKED);
        }
    }

    private void fillRelations(Product product, ProductSaveRequest request) {
        Brand brand = brandService.require(request.brandId());
        Category category = categoryService.requireLeaf(request.categoryId());
        product.setBrandName(brand.getName());
        product.setCategoryName(category.getName());
    }

    private void saveParams(Long productId, ProductSaveRequest request) {
        List<ProductSaveRequest.ParamValue> params = request.params() == null ? List.of() : request.params();
        Set<Long> attrIds = new HashSet<>();
        params.forEach(p -> BizException.throwIf(!attrIds.add(p.attrId()), CommonErrorCode.PARAM_INVALID,
                "商品参数重复：" + p.attrId()));
        attrService.checkParams(request.attrGroupId(), attrIds);
        if (params.isEmpty()) {
            return;
        }
        attrValueMapper.insert(params.stream().map(p -> {
            ProductAttrValue value = new ProductAttrValue();
            value.setProductId(productId);
            value.setAttrId(p.attrId());
            value.setValue(p.value());
            return value;
        }).toList());
    }

    private void refreshPrice(Long productId) {
        BigDecimal minPrice = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                        .select(Sku::getPrice)
                        .eq(Sku::getProductId, productId))
                .stream().map(Sku::getPrice).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        productMapper.update(Wrappers.<Product>lambdaUpdate()
                .set(Product::getPrice, minPrice)
                .eq(Product::getId, productId));
    }

    static LambdaQueryWrapper<Product> buildQuery(ProductQuery query) {
        return Wrappers.<Product>lambdaQuery()
                .like(StringUtils.hasText(query.getKeyword()), Product::getName, query.getKeyword())
                .eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId())
                .eq(query.getBrandId() != null, Product::getBrandId, query.getBrandId())
                .eq(query.getPublishStatus() != null, Product::getPublishStatus, query.getPublishStatus())
                .orderByDesc(Product::getSort)
                .orderByDesc(Product::getId);
    }

    private Product require(Long id) {
        Product product = productMapper.selectById(id);
        BizException.throwIf(product == null, ProductErrorCode.PRODUCT_NOT_FOUND);
        return product;
    }

    private Sku requireSku(Long id) {
        Sku sku = skuMapper.selectById(id);
        BizException.throwIf(sku == null, ProductErrorCode.SKU_NOT_FOUND);
        return sku;
    }
}
