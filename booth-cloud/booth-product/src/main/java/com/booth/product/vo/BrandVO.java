package com.booth.product.vo;

public record BrandVO(
        Long id,
        String name,
        String firstLetter,
        String logo,
        String story,
        Integer sort,
        Boolean showStatus) {
}
