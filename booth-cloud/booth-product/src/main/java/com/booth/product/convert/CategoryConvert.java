package com.booth.product.convert;

import com.booth.product.dto.CategorySaveRequest;
import com.booth.product.entity.Category;
import com.booth.product.vo.CategoryTreeVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface CategoryConvert {

    Category toEntity(CategorySaveRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "level", ignore = true)
    void update(@MappingTarget Category category, CategorySaveRequest request);

    @Mapping(target = "children", ignore = true)
    CategoryTreeVO toTreeVO(Category category);
}
