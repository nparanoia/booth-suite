package com.booth.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booth.user.entity.AdminRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AdminRoleMapper extends BaseMapper<AdminRole> {

    /**
     * 串行化同一角色的并发权限分配（先删后插，并发时会互相覆盖或撞唯一键）。必须在事务内调用。
     */
    @Select("SELECT id FROM admin_role WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);
}
