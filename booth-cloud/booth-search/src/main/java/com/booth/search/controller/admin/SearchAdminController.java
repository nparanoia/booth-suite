package com.booth.search.controller.admin;

import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.search.service.IndexRebuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-搜索索引")
@RestController
@RequestMapping("/admin/search")
@RequiredArgsConstructor
public class SearchAdminController {

    private final IndexRebuildService indexRebuildService;

    @Operation(summary = "全量重建商品索引并切换别名")
    @RequiresPermission("search:index:edit")
    @PostMapping("/rebuild")
    public Result<Void> rebuild() {
        indexRebuildService.rebuild();
        return Result.ok();
    }
}
