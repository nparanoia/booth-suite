package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.user.enums.AccountStatus;
import com.booth.user.enums.Gender;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@TableName("member")
public class Member extends BaseEntity {

    private String username;
    /** BCrypt 哈希，任何接口都不返回 */
    private String password;
    private String nickname;
    private String phone;
    private String avatar;
    private Gender gender;
    private LocalDate birthday;
    /** 只能和积分流水在同一事务里用条件 UPDATE 修改 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer integration;
    private AccountStatus status;
}
