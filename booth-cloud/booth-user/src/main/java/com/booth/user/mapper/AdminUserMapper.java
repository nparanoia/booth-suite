package com.booth.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booth.user.entity.AdminUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AdminUserMapper extends BaseMapper<AdminUser> {

    /**
     * 串行化同一管理员的并发角色分配。必须在事务内调用。
     */
    @Select("SELECT id FROM admin_user WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Long lockById(@Param("id") Long id);
}
