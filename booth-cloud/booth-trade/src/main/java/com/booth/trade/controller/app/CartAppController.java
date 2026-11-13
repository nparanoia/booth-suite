package com.booth.trade.controller.app;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.Result;
import com.booth.trade.dto.CartAddRequest;
import com.booth.trade.dto.CartCheckRequest;
import com.booth.trade.dto.CartQuantityRequest;
import com.booth.trade.service.CartService;
import com.booth.trade.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台-购物车")
@RestController
@RequestMapping("/app/cart")
@RequiredArgsConstructor
public class CartAppController {

    private final CartService cartService;

    @Operation(summary = "我的购物车", description = "价格和库存为实时数据，下架商品标记为不可购买")
    @GetMapping
    public Result<CartVO> list(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId) {
        return Result.ok(cartService.list(memberId));
    }

    @Operation(summary = "加入购物车", description = "同一 SKU 重复加购会累加数量，最多 99 件")
    @PostMapping("/items")
    public Result<Void> add(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                            @Valid @RequestBody CartAddRequest request) {
        cartService.add(memberId, request);
        return Result.ok();
    }

    @Operation(summary = "修改数量")
    @PutMapping("/items/{id}/quantity")
    public Result<Void> updateQuantity(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                       @PathVariable Long id, @Valid @RequestBody CartQuantityRequest request) {
        cartService.updateQuantity(memberId, id, request.quantity());
        return Result.ok();
    }

    @Operation(summary = "勾选或取消勾选", description = "itemIds 不传表示全部")
    @PutMapping("/items/checked")
    public Result<Void> updateChecked(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                      @Valid @RequestBody CartCheckRequest request) {
        cartService.updateChecked(memberId, request);
        return Result.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/items")
    public Result<Void> delete(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                               @RequestParam @NotEmpty @Size(max = 100) List<Long> ids) {
        cartService.delete(memberId, ids);
        return Result.ok();
    }
}
