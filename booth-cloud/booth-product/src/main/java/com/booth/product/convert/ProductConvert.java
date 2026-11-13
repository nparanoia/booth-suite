package com.booth.product.convert;

import com.booth.product.dto.ProductSaveRequest;
import com.booth.product.dto.SkuCreateRequest;
import com.booth.product.dto.SkuUpdateRequest;
import com.booth.product.entity.Product;
import com.booth.product.entity.Sku;
import com.booth.product.vo.ProductDetailVO;
import com.booth.product.vo.ProductListVO;
import com.booth.product.vo.SkuVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Arrays;
import java.util.List;

@Mapper
public interface ProductConvert {

    String PIC_SEPARATOR = ",";

    Product toEntity(ProductSaveRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Product product, ProductSaveRequest request);

    ProductListVO toListVO(Product product);

    ProductDetailVO toDetailVO(Product product, List<ProductDetailVO.ParamVO> params, List<SkuVO> skus);

    @Mapping(target = "specData", source = "specs")
    Sku toEntity(SkuCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Sku sku, SkuUpdateRequest request);

    @Mapping(target = "specs", source = "specData")
    @Mapping(target = "availableStock", expression = "java(sku.availableStock())")
    SkuVO toSkuVO(Sku sku);

    default String joinPics(List<String> pics) {
        return pics == null ? null : String.join(PIC_SEPARATOR, pics);
    }

    default List<String> splitPics(String pics) {
        return pics == null || pics.isBlank() ? List.of() : Arrays.asList(pics.split(PIC_SEPARATOR));
    }
}
