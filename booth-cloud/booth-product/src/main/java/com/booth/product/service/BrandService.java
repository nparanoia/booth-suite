package com.booth.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.product.convert.BrandConvert;
import com.booth.product.dto.BrandQuery;
import com.booth.product.dto.BrandSaveRequest;
import com.booth.product.entity.Brand;
import com.booth.product.entity.Product;
import com.booth.product.error.ProductErrorCode;
import com.booth.product.mapper.BrandMapper;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.support.AfterCommit;
import com.booth.product.vo.BrandVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandMapper brandMapper;
    private final ProductMapper productMapper;
    private final BrandConvert brandConvert;
    private final ProductViewCache viewCache;

    public PageResult<BrandVO> page(BrandQuery query) {
        Page<Brand> page = brandMapper.selectPage(query.toPage(), Wrappers.<Brand>lambdaQuery()
                .like(StringUtils.hasText(query.getKeyword()), Brand::getName, query.getKeyword())
                .orderByDesc(Brand::getSort)
                .orderByDesc(Brand::getId));
        return PageResults.of(page, brandConvert::toVO);
    }

    public BrandVO get(Long id) {
        return brandConvert.toVO(require(id));
    }

    public Long create(BrandSaveRequest request) {
        Brand brand = brandConvert.toEntity(request);
        try {
            brandMapper.insert(brand);
        } catch (DuplicateKeyException e) {
            throw new BizException(ProductErrorCode.BRAND_NAME_EXISTS);
        }
        return brand.getId();
    }

    /**
     * 商品表冗余了品牌名，改名时在同一事务里同步，避免列表和搜索显示旧名字。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BrandSaveRequest request) {
        Brand brand = require(id);
        String oldName = brand.getName();
        brandConvert.update(brand, request);
        try {
            brandMapper.updateById(brand);
        } catch (DuplicateKeyException e) {
            throw new BizException(ProductErrorCode.BRAND_NAME_EXISTS);
        }
        if (!Objects.equals(oldName, brand.getName())) {
            productMapper.update(Wrappers.<Product>lambdaUpdate()
                    .set(Product::getBrandName, brand.getName())
                    .eq(Product::getBrandId, id));
            List<Long> productIds = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                            .select(Product::getId)
                            .eq(Product::getBrandId, id))
                    .stream().map(Product::getId).toList();
            AfterCommit.run(() -> productIds.forEach(viewCache::evictProduct));
        }
    }

    public void delete(Long id) {
        require(id);
        boolean inUse = productMapper.exists(Wrappers.<Product>lambdaQuery().eq(Product::getBrandId, id));
        BizException.throwIf(inUse, ProductErrorCode.BRAND_IN_USE);
        brandMapper.deleteById(id);
    }

    Brand require(Long id) {
        Brand brand = brandMapper.selectById(id);
        BizException.throwIf(brand == null, ProductErrorCode.BRAND_NOT_FOUND);
        return brand;
    }
}
