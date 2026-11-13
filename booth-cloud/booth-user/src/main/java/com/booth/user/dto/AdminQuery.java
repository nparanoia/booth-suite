package com.booth.user.dto;

import com.booth.common.mybatis.page.PageQuery;
import com.booth.user.enums.AccountStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminQuery extends PageQuery {

    /** 匹配用户名或昵称 */
    private String keyword;
    private AccountStatus status;
}
