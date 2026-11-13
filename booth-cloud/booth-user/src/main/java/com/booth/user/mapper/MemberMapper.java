package com.booth.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booth.user.entity.Member;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 锁住会员行，用来串行化同一会员的并发写操作（如两个请求同时设置默认地址）。必须在事务内调用。
     */
    @Select("SELECT id FROM member WHERE id = #{id} FOR UPDATE")
    Long lockById(@Param("id") Long id);
}
