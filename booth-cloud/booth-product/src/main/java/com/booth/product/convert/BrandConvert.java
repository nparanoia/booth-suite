package com.booth.product.convert;

import com.booth.product.dto.BrandSaveRequest;
import com.booth.product.entity.Brand;
import com.booth.product.vo.BrandVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface BrandConvert {

    Brand toEntity(BrandSaveRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Brand brand, BrandSaveRequest request);

    BrandVO toVO(Brand brand);
}
