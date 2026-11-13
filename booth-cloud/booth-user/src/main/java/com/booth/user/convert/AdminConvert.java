package com.booth.user.convert;

import com.booth.user.dto.RoleCreateRequest;
import com.booth.user.dto.RoleUpdateRequest;
import com.booth.user.entity.AdminRole;
import com.booth.user.entity.AdminUser;
import com.booth.user.vo.AdminUserVO;
import com.booth.user.vo.RoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper
public interface AdminConvert {

    AdminUserVO toVO(AdminUser admin, List<Long> roleIds);

    RoleVO toVO(AdminRole role);

    AdminRole toEntity(RoleCreateRequest request);

    void update(@MappingTarget AdminRole role, RoleUpdateRequest request);
}
