package com.booth.user.convert;

import com.booth.api.user.dto.MemberAddressDTO;
import com.booth.api.user.dto.MemberBriefDTO;
import com.booth.user.dto.AddressSaveRequest;
import com.booth.user.dto.MemberProfileUpdateRequest;
import com.booth.user.entity.Member;
import com.booth.user.entity.MemberAddress;
import com.booth.user.enums.AccountStatus;
import com.booth.user.vo.AddressVO;
import com.booth.user.vo.MemberAdminVO;
import com.booth.user.vo.MemberProfileVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(imports = AccountStatus.class)
public interface MemberConvert {

    @Mapping(target = "phone", qualifiedByName = "maskPhone")
    MemberProfileVO toProfileVO(Member member);

    @Mapping(target = "phone", qualifiedByName = "maskPhone")
    MemberAdminVO toAdminVO(Member member);

    @Mapping(target = "enabled", expression = "java(member.getStatus() == AccountStatus.ENABLED)")
    MemberBriefDTO toBriefDTO(Member member);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Member member, MemberProfileUpdateRequest request);

    MemberAddress toEntity(AddressSaveRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "isDefault", ignore = true)
    void update(@MappingTarget MemberAddress address, AddressSaveRequest request);

    AddressVO toVO(MemberAddress address);

    MemberAddressDTO toDTO(MemberAddress address);

    /**
     * 必须用 @Named 显式引用：MapStruct 会把签名匹配的方法自动套用到所有同类型字段上。
     */
    @Named("maskPhone")
    default String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
