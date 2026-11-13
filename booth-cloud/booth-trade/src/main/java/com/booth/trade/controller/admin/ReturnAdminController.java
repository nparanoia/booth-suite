package com.booth.trade.controller.admin;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.trade.dto.HandleNoteRequest;
import com.booth.trade.enums.ReturnStatus;
import com.booth.trade.service.ReturnService;
import com.booth.trade.vo.ReturnApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-售后")
@RequiresPermission("trade:order:edit")
@RestController
@RequestMapping("/admin/returns")
@RequiredArgsConstructor
public class ReturnAdminController {

    private final ReturnService returnService;

    @Operation(summary = "售后列表")
    @RequiresPermission("trade:order:view")
    @GetMapping
    public Result<List<ReturnApplyVO>> list(@RequestParam(required = false) ReturnStatus status) {
        return Result.ok(returnService.adminList(status));
    }

    @Operation(summary = "同意退货")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@RequestHeader(AuthHeaders.ADMIN_ID) Long adminId, @PathVariable Long id,
                                @Valid @RequestBody(required = false) HandleNoteRequest request) {
        returnService.approve(id, adminId, request);
        return Result.ok();
    }

    @Operation(summary = "拒绝")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@RequestHeader(AuthHeaders.ADMIN_ID) Long adminId, @PathVariable Long id,
                               @Valid @RequestBody(required = false) HandleNoteRequest request) {
        returnService.reject(id, adminId, request);
        return Result.ok();
    }

    @Operation(summary = "确认收到退货")
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@RequestHeader(AuthHeaders.ADMIN_ID) Long adminId, @PathVariable Long id,
                                @Valid @RequestBody(required = false) HandleNoteRequest request) {
        returnService.receive(id, adminId, request);
        return Result.ok();
    }
}
