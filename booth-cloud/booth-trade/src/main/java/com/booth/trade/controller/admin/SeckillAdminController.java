package com.booth.trade.controller.admin;

import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.trade.dto.SeckillActivityRequest;
import com.booth.trade.dto.SeckillSessionRequest;
import com.booth.trade.dto.SeckillSkuRequest;
import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillSession;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.service.SeckillAdminService;
import com.booth.trade.service.SeckillReconcileService;
import com.booth.trade.vo.SeckillReconcileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台-秒杀")
@RequiresPermission("trade:seckill:edit")
@RestController
@RequestMapping("/admin/seckill")
@RequiredArgsConstructor
public class SeckillAdminController {

    private final SeckillAdminService seckillAdminService;
    private final SeckillReconcileService seckillReconcileService;

    @Operation(summary = "秒杀活动分页")
    @RequiresPermission("trade:seckill:view")
    @GetMapping("/activities")
    public Result<PageResult<SeckillActivity>> activities(@RequestParam(defaultValue = "1") long pageNum,
                                                          @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(seckillAdminService.pageActivities(pageNum, pageSize));
    }

    @Operation(summary = "新增秒杀活动")
    @PostMapping("/activities")
    public Result<Long> createActivity(@Valid @RequestBody SeckillActivityRequest request) {
        return Result.ok(seckillAdminService.createActivity(request));
    }

    @Operation(summary = "修改秒杀活动")
    @PutMapping("/activities/{id}")
    public Result<Void> updateActivity(@PathVariable Long id, @Valid @RequestBody SeckillActivityRequest request) {
        seckillAdminService.updateActivity(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除草稿活动")
    @DeleteMapping("/activities/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        seckillAdminService.deleteActivity(id);
        return Result.ok();
    }

    @Operation(summary = "发布活动")
    @PostMapping("/activities/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        seckillAdminService.publish(id);
        return Result.ok();
    }

    @Operation(summary = "下线活动")
    @PostMapping("/activities/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        seckillAdminService.offline(id);
        return Result.ok();
    }

    @Operation(summary = "秒杀场次")
    @RequiresPermission("trade:seckill:view")
    @GetMapping("/sessions")
    public Result<List<SeckillSession>> sessions() {
        return Result.ok(seckillAdminService.sessions());
    }

    @Operation(summary = "新增场次")
    @PostMapping("/sessions")
    public Result<Long> createSession(@Valid @RequestBody SeckillSessionRequest request) {
        return Result.ok(seckillAdminService.createSession(request));
    }

    @Operation(summary = "修改场次")
    @PutMapping("/sessions/{id}")
    public Result<Void> updateSession(@PathVariable Long id, @Valid @RequestBody SeckillSessionRequest request) {
        seckillAdminService.updateSession(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除场次")
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        seckillAdminService.deleteSession(id);
        return Result.ok();
    }

    @Operation(summary = "秒杀商品")
    @RequiresPermission("trade:seckill:view")
    @GetMapping("/skus")
    public Result<List<SeckillSku>> skus(@RequestParam(required = false) Long activityId) {
        return Result.ok(seckillAdminService.skus(activityId));
    }

    @Operation(summary = "新增秒杀商品")
    @PostMapping("/skus")
    public Result<Long> createSku(@Valid @RequestBody SeckillSkuRequest request) {
        return Result.ok(seckillAdminService.createSku(request));
    }

    @Operation(summary = "修改秒杀商品")
    @PutMapping("/skus/{id}")
    public Result<Void> updateSku(@PathVariable Long id, @Valid @RequestBody SeckillSkuRequest request) {
        seckillAdminService.updateSku(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除草稿中的秒杀商品")
    @DeleteMapping("/skus/{id}")
    public Result<Void> deleteSku(@PathVariable Long id) {
        seckillAdminService.deleteSku(id);
        return Result.ok();
    }

    @Operation(summary = "对账")
    @RequiresPermission("trade:seckill:view")
    @PostMapping("/skus/{id}/reconcile")
    public Result<SeckillReconcileVO> reconcile(@PathVariable Long id) {
        return Result.ok(seckillReconcileService.reconcile(id));
    }
}
