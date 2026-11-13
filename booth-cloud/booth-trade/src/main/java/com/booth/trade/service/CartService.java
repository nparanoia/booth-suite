package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.product.SkuInnerClient;
import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.common.core.exception.BizException;
import com.booth.trade.dto.CartAddRequest;
import com.booth.trade.dto.CartCheckRequest;
import com.booth.trade.entity.CartItem;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.CartItemMapper;
import com.booth.trade.vo.CartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车只存 skuId 和数量，商品名、价格、库存每次从商品服务批量获取。
 * 所有写操作都带 member_id 条件，只能改自己的购物车。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QUANTITY = 99;
    private static final int MAX_ITEMS = 100;

    private final CartItemMapper cartItemMapper;
    private final SkuInnerClient skuInnerClient;

    public void add(Long memberId, CartAddRequest request) {
        SkuInfoDTO sku = skuInnerClient.listByIds(List.of(request.skuId())).dataOrThrow()
                .stream().findFirst()
                .orElseThrow(() -> new BizException(TradeErrorCode.SKU_NOT_FOUND));
        BizException.throwIf(!sku.onSale(), TradeErrorCode.SKU_OFF_SHELF);
        BizException.throwIf(sku.availableStock() < request.quantity(), TradeErrorCode.STOCK_NOT_ENOUGH);

        boolean exists = cartItemMapper.exists(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getMemberId, memberId)
                .eq(CartItem::getSkuId, request.skuId()));
        if (!exists) {
            long count = cartItemMapper.selectCount(Wrappers.<CartItem>lambdaQuery().eq(CartItem::getMemberId, memberId));
            BizException.throwIf(count >= MAX_ITEMS, TradeErrorCode.CART_FULL);
        }
        cartItemMapper.upsert(memberId, sku.productId(), sku.skuId(), request.quantity(), MAX_QUANTITY);
    }

    public CartVO list(Long memberId) {
        List<CartItem> items = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getMemberId, memberId)
                .orderByDesc(CartItem::getUpdateTime));
        if (items.isEmpty()) {
            return new CartVO(List.of(), 0, BigDecimal.ZERO);
        }
        Map<Long, SkuInfoDTO> skus = skuInnerClient.listByIds(items.stream().map(CartItem::getSkuId).toList())
                .dataOrThrow().stream()
                .collect(Collectors.toMap(SkuInfoDTO::skuId, Function.identity()));

        List<CartVO.Item> voItems = items.stream().map(item -> toItem(item, skus.get(item.getSkuId()))).toList();
        List<CartVO.Item> payable = voItems.stream().filter(i -> i.checked() && i.available()).toList();
        BigDecimal amount = payable.stream()
                .map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartVO(voItems, payable.size(), amount);
    }

    public void updateQuantity(Long memberId, Long itemId, int quantity) {
        int rows = cartItemMapper.update(Wrappers.<CartItem>lambdaUpdate()
                .set(CartItem::getQuantity, quantity)
                .eq(CartItem::getId, itemId)
                .eq(CartItem::getMemberId, memberId));
        BizException.throwIf(rows == 0, TradeErrorCode.CART_ITEM_NOT_FOUND);
    }

    public void updateChecked(Long memberId, CartCheckRequest request) {
        cartItemMapper.update(Wrappers.<CartItem>lambdaUpdate()
                .set(CartItem::getChecked, request.checked())
                .eq(CartItem::getMemberId, memberId)
                .in(!CollectionUtils.isEmpty(request.itemIds()), CartItem::getId, request.itemIds()));
    }

    public void delete(Long memberId, List<Long> itemIds) {
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getMemberId, memberId)
                .in(CartItem::getId, itemIds));
    }

    private CartVO.Item toItem(CartItem item, SkuInfoDTO sku) {
        if (sku == null) {
            return new CartVO.Item(item.getId(), item.getSkuId(), item.getProductId(), null, null, List.of(),
                    BigDecimal.ZERO, item.getQuantity(), item.getChecked(), false, 0);
        }
        return new CartVO.Item(item.getId(), sku.skuId(), sku.productId(), sku.productName(), sku.pic(), sku.specs(),
                sku.price(), item.getQuantity(), item.getChecked(), sku.onSale(), sku.availableStock());
    }
}
