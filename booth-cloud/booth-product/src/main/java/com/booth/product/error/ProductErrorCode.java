package com.booth.product.error;

import com.booth.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    BRAND_NOT_FOUND(20001, "品牌不存在"),
    BRAND_NAME_EXISTS(20002, "品牌名称已存在"),
    BRAND_IN_USE(20003, "品牌下还有商品，不能删除"),

    CATEGORY_NOT_FOUND(20101, "分类不存在"),
    CATEGORY_LEVEL_INVALID(20102, "只支持两级分类，父分类必须是一级分类"),
    CATEGORY_NOT_LEAF(20103, "商品只能挂在二级分类下"),
    CATEGORY_HAS_CHILDREN(20104, "分类下还有子分类，不能删除"),
    CATEGORY_IN_USE(20105, "分类下还有商品，不能删除"),

    ATTR_GROUP_NOT_FOUND(20201, "属性分组不存在"),
    ATTR_GROUP_IN_USE(20202, "属性分组下还有属性，不能删除"),
    ATTR_NOT_FOUND(20203, "属性不存在"),
    ATTR_IN_USE(20204, "属性已被商品使用，不能删除"),
    PRODUCT_PARAM_INVALID(20205, "商品参数必须是所选属性分组下的展示参数"),

    PRODUCT_NOT_FOUND(20301, "商品不存在或已下架"),
    PRODUCT_SN_EXISTS(20302, "商品货号已存在"),
    PRODUCT_ON_SHELF(20303, "商品上架中，请先下架"),
    SKU_NOT_FOUND(20304, "SKU 不存在"),
    SKU_CODE_EXISTS(20305, "SKU 编码已存在"),
    STOCK_BELOW_LOCKED(20306, "库存不能小于已被订单锁定的数量"),

    STOCK_NOT_ENOUGH(20401, "库存不足"),
    STOCK_LOCK_CLOSED(20402, "订单已取消，不能再锁定"),
    STOCK_LOCK_CONFLICT(20403, "订单库存状态冲突");

    private final int code;
    private final String message;

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
