package com.booth.product.controller.admin;

import com.booth.common.core.result.PageResult;
import com.booth.common.core.result.Result;
import com.booth.common.web.security.RequiresPermission;
import com.booth.product.dto.ProductCreateRequest;
import com.booth.product.dto.ProductQuery;
import com.booth.product.dto.ProductSaveRequest;
import com.booth.product.dto.PublishRequest;
import com.booth.product.dto.SkuCreateRequest;
import com.booth.product.dto.SkuUpdateRequest;
import com.booth.product.dto.StockUpdateRequest;
import com.booth.product.service.ProductService;
import com.booth.product.vo.ProductDetailVO;
import com.booth.product.vo.ProductListVO;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台-商品与 SKU")
@RequiresPermission("product:product:edit")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;

    @Operation(summary = "分页查询商品")
    @RequiresPermission("product:product:view")
    @GetMapping("/products")
    public Result<PageResult<ProductListVO>> page(@Valid ProductQuery query) {
        return Result.ok(productService.page(query));
    }

    @Operation(summary = "商品详情（含库存明细）")
    @RequiresPermission("product:product:view")
    @GetMapping("/products/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @Operation(summary = "新增商品", description = "商品、SKU、参数在同一事务写入，新商品默认下架")
    @PostMapping("/products")
    public Result<Long> create(@Valid @RequestBody ProductCreateRequest request) {
        return Result.ok(productService.create(request));
    }

    @Operation(summary = "修改商品信息与参数", description = "不修改 SKU")
    @PutMapping("/products/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
        productService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "批量上下架")
    @PutMapping("/products/publish")
    public Result<Void> publish(@Valid @RequestBody PublishRequest request) {
        productService.publish(request);
        return Result.ok();
    }

    @Operation(summary = "删除商品", description = "逻辑删除，上架中的商品需先下架")
    @DeleteMapping("/products/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "新增 SKU")
    @PostMapping("/products/{productId}/skus")
    public Result<Long> addSku(@PathVariable Long productId, @Valid @RequestBody SkuCreateRequest request) {
        return Result.ok(productService.addSku(productId, request));
    }

    @Operation(summary = "修改 SKU 价格、图片、预警值")
    @PutMapping("/skus/{id}")
    public Result<Void> updateSku(@PathVariable Long id, @Valid @RequestBody SkuUpdateRequest request) {
        productService.updateSku(id, request);
        return Result.ok();
    }

    @Operation(summary = "设置 SKU 库存", description = "新库存不能小于已被订单锁定的数量")
    @RequiresPermission("product:stock:edit")
    @PutMapping("/skus/{id}/stock")
    public Result<Void> updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        productService.updateStock(id, request.stock());
        return Result.ok();
    }
}
