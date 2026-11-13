package com.booth.user.controller.admin;

import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.user.dto.MemberQuery;
import com.booth.user.dto.StatusUpdateRequest;
import com.booth.user.service.MemberService;
import com.booth.user.vo.MemberAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-会员管理")
@RequiresPermission("user:member:edit")
@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class MemberAdminController {

    private final MemberService memberService;

    @Operation(summary = "分页查询会员", description = "keyword 匹配用户名或手机号")
    @RequiresPermission("user:member:view")
    @GetMapping
    public Result<PageResult<MemberAdminVO>> page(@Valid MemberQuery query) {
        return Result.ok(memberService.page(query));
    }

    @Operation(summary = "启用或禁用会员")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        memberService.updateStatus(id, request.status());
        return Result.ok();
    }
}
