package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.api.user.dto.MemberAddressDTO;
import com.booth.api.user.dto.MemberBriefDTO;
import com.booth.common.core.exception.BizException;
import com.booth.trade.entity.SeckillOrder;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.entity.TradeOrderItem;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.enums.SeckillRecordStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.SeckillSkuMapper;
import com.booth.trade.mapper.SeckillOrderMapper;
import com.booth.trade.support.SeckillOrderMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 秒杀记录、活动库存和订单放在同一个本地事务里。远程锁库存在这之前完成。
 * 重复键不能在这个方法里吞掉，否则事务会被标成只能回滚。
 */
@Service
@RequiredArgsConstructor
public class SeckillOrderStore {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillSkuMapper seckillSkuMapper;
    private final OrderStoreService orderStoreService;

    @Transactional(rollbackFor = Exception.class)
    public void save(SeckillOrderMessage message, MemberBriefDTO member, MemberAddressDTO address, SeckillSku seckill, SkuInfoDTO sku) {
        SeckillOrder record = new SeckillOrder();
        record.setSeckillSkuId(seckill.getId());
        record.setMemberId(message.memberId());
        record.setOrderSn(message.orderSn());
        record.setQuantity(message.quantity());
        record.setStatus(SeckillRecordStatus.ACTIVE);
        record.setActiveMember(message.memberId());
        seckillOrderMapper.insert(record);

        int decreased = seckillSkuMapper.update(null, Wrappers.<SeckillSku>lambdaUpdate()
                .setSql("seckill_stock = seckill_stock - " + message.quantity())
                .eq(SeckillSku::getId, seckill.getId())
                .apply("seckill_stock >= {0}", message.quantity()));
        BizException.throwIf(decreased == 0, TradeErrorCode.SECKILL_SOLD_OUT);

        BigDecimal price = seckill.getSeckillPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal pay = price.multiply(BigDecimal.valueOf(message.quantity())).setScale(2, RoundingMode.HALF_UP);
        BizException.throwIf(pay.compareTo(new BigDecimal("0.01")) < 0, TradeErrorCode.ORDER_PAY_TOO_SMALL);

        TradeOrder order = new TradeOrder();
        order.setId(message.orderId());
        order.setOrderSn(message.orderSn());
        order.setMemberId(member.id());
        order.setMemberUsername(member.username());
        order.setOrderType(1);
        order.setStatus(OrderStatus.WAIT_PAY);
        order.setTotalAmount(pay);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setCouponAmount(BigDecimal.ZERO);
        order.setIntegrationAmount(BigDecimal.ZERO);
        order.setPayAmount(pay);
        order.setUseIntegration(0);
        order.setRewardIntegration(sku.giftIntegration() * message.quantity());
        order.setReceiverName(address.receiverName());
        order.setReceiverPhone(address.receiverPhone());
        order.setReceiverProvince(address.province());
        order.setReceiverCity(address.city());
        order.setReceiverRegion(address.region());
        order.setReceiverDetailAddress(address.detailAddress());
        order.setReceiverPostCode(address.postCode());
        order.setMemberDeleted(false);

        TradeOrderItem item = new TradeOrderItem();
        item.setOrderId(message.orderId());
        item.setOrderSn(message.orderSn());
        item.setProductId(sku.productId());
        item.setSkuId(sku.skuId());
        item.setProductName(seckill.getProductName());
        item.setProductPic(seckill.getProductPic());
        item.setSkuCode(sku.skuCode());
        item.setSpecData(sku.specs());
        item.setBrandName(sku.brandName());
        item.setCategoryId(sku.categoryId());
        item.setPrice(price);
        item.setQuantity(message.quantity());
        item.setCouponAmount(BigDecimal.ZERO);
        item.setIntegrationAmount(BigDecimal.ZERO);
        item.setRealAmount(pay);
        orderStoreService.save(order, List.of(item), List.of());
    }
}
