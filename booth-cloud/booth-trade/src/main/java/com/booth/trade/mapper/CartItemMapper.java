package com.booth.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booth.trade.entity.CartItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface CartItemMapper extends BaseMapper<CartItem> {

    /**
     * 先查再改在并发加购时会丢失数量，这里依赖 (member_id, sku_id) 唯一键一条 SQL 完成"插入或累加"，
     * 累加结果不超过 maxQuantity。重新加购的商品自动勾选。
     */
    @Insert("""
            INSERT INTO cart_item (member_id, product_id, sku_id, quantity, checked)
            VALUES (#{memberId}, #{productId}, #{skuId}, #{quantity}, 1) AS new
            ON DUPLICATE KEY UPDATE
                quantity = LEAST(cart_item.quantity + new.quantity, #{maxQuantity}),
                checked = 1
            """)
    int upsert(@Param("memberId") Long memberId,
               @Param("productId") Long productId,
               @Param("skuId") Long skuId,
               @Param("quantity") int quantity,
               @Param("maxQuantity") int maxQuantity);
}
