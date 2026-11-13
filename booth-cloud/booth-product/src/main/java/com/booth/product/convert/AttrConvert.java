package com.booth.product.convert;

import com.booth.product.dto.AttrSaveRequest;
import com.booth.product.entity.Attr;
import com.booth.product.entity.AttrGroup;
import com.booth.product.vo.AttrGroupVO;
import com.booth.product.vo.AttrVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface AttrConvert {

    AttrGroupVO toGroupVO(AttrGroup group);

    Attr toEntity(AttrSaveRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "groupId", ignore = true)
    void update(@MappingTarget Attr attr, AttrSaveRequest request);

    AttrVO toVO(Attr attr);
}
