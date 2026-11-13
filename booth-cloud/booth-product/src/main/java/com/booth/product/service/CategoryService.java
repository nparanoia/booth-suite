package com.booth.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.product.convert.CategoryConvert;
import com.booth.product.dto.CategorySaveRequest;
import com.booth.product.entity.Category;
import com.booth.product.entity.Product;
import com.booth.product.error.ProductErrorCode;
import com.booth.product.mapper.CategoryMapper;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.support.AfterCommit;
import com.booth.product.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CategoryConvert categoryConvert;
    private final ProductViewCache viewCache;

    /**
     * 分类只有两级且总量很小，一次查出全部再在内存里组装，避免逐层递归查询。
     */
    public List<CategoryTreeVO> tree(boolean onlyVisible) {
        if (onlyVisible) {
            return viewCache.categoryTree(() -> assemble(true));
        }
        return assemble(false);
    }

    private List<CategoryTreeVO> assemble(boolean onlyVisible) {
        List<Category> all = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(onlyVisible, Category::getShowStatus, true)
                .orderByDesc(Category::getSort)
                .orderByAsc(Category::getId));
        Map<Long, CategoryTreeVO> roots = new LinkedHashMap<>();
        all.stream()
                .filter(c -> c.getParentId() == Category.ROOT_ID)
                .forEach(c -> roots.put(c.getId(), categoryConvert.toTreeVO(c)));
        all.stream()
                .filter(c -> c.getParentId() != Category.ROOT_ID)
                .forEach(c -> {
                    CategoryTreeVO parent = roots.get(c.getParentId());
                    // 父分类被隐藏时，前台不展示其子分类
                    if (parent != null) {
                        parent.children().add(categoryConvert.toTreeVO(c));
                    }
                });
        return new ArrayList<>(roots.values());
    }

    public Long create(CategorySaveRequest request) {
        Category category = categoryConvert.toEntity(request);
        long parentId = request.parentId() == null ? Category.ROOT_ID : request.parentId();
        if (parentId == Category.ROOT_ID) {
            category.setLevel(1);
        } else {
            Category parent = require(parentId);
            BizException.throwIf(parent.getLevel() != 1, ProductErrorCode.CATEGORY_LEVEL_INVALID);
            category.setLevel(Category.MAX_LEVEL);
        }
        category.setParentId(parentId);
        categoryMapper.insert(category);
        viewCache.evictCategoryTree();
        return category.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategorySaveRequest request) {
        Category category = require(id);
        String oldName = category.getName();
        categoryConvert.update(category, request);
        categoryMapper.updateById(category);
        boolean rename = category.getLevel() == Category.MAX_LEVEL && !Objects.equals(oldName, category.getName());
        if (rename) {
            productMapper.update(Wrappers.<Product>lambdaUpdate()
                    .set(Product::getCategoryName, category.getName())
                    .eq(Product::getCategoryId, id));
        }
        List<Long> productIds = rename
                ? productMapper.selectList(Wrappers.<Product>lambdaQuery()
                        .select(Product::getId)
                        .eq(Product::getCategoryId, id))
                .stream().map(Product::getId).toList()
                : List.of();
        AfterCommit.run(() -> {
            viewCache.evictCategoryTree();
            productIds.forEach(viewCache::evictProduct);
        });
    }

    public void delete(Long id) {
        require(id);
        boolean hasChildren = categoryMapper.exists(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        BizException.throwIf(hasChildren, ProductErrorCode.CATEGORY_HAS_CHILDREN);
        boolean inUse = productMapper.exists(Wrappers.<Product>lambdaQuery().eq(Product::getCategoryId, id));
        BizException.throwIf(inUse, ProductErrorCode.CATEGORY_IN_USE);
        categoryMapper.deleteById(id);
        viewCache.evictCategoryTree();
    }

    Category requireLeaf(Long id) {
        Category category = require(id);
        BizException.throwIf(category.getLevel() != Category.MAX_LEVEL, ProductErrorCode.CATEGORY_NOT_LEAF);
        return category;
    }

    private Category require(Long id) {
        Category category = categoryMapper.selectById(id);
        BizException.throwIf(category == null, ProductErrorCode.CATEGORY_NOT_FOUND);
        return category;
    }
}
