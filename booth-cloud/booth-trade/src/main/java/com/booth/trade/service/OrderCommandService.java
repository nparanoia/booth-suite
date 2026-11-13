package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.product.SkuInnerClient;
import com.booth.api.product.StockInnerClient;
import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.api.product.dto.StockItemDTO;
import com.booth.api.product.dto.StockLockRequest;
import com.booth.api.product.dto.StockReleaseRequest;
import com.booth.api.user.IntegrationInnerClient;
import com.booth.api.user.MemberInnerClient;
import com.booth.api.user.dto.IntegrationChangeRequest;
import com.booth.api.user.dto.MemberAddressDTO;
import com.booth.api.user.dto.MemberBriefDTO;
import com.booth.common.core.exception.BizException;
import com.booth.trade.dto.OrderPrepareRequest;
import com.booth.trade.dto.OrderSkuRequest;
import com.booth.trade.dto.OrderSubmitRequest;
import com.booth.trade.entity.CartItem;
import com.booth.trade.entity.Coupon;
import com.booth.trade.entity.MemberCoupon;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.entity.TradeOrderItem;
import com.booth.trade.enums.MemberCouponStatus;
import com.booth.trade.enums.OrderSource;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.CartItemMapper;
import com.booth.trade.support.OrderPricing;
import com.booth.trade.vo.CouponCandidateVO;
import com.booth.trade.vo.OrderConfirmVO;
import com.booth.trade.vo.OrderItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 下单顺序：防重复提交、算价、锁优惠券、扣积分、锁库存，最后本地事务落单。
 * 远程调用失败时按相反顺序补偿。金额以商品服务实时价格为准。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final CartItemMapper cartItemMapper;
    private final SkuInnerClient skuInnerClient;
    private final StockInnerClient stockInnerClient;
    private final MemberInnerClient memberInnerClient;
    private final IntegrationInnerClient integrationInnerClient;
    private final CouponService couponService;
    private final OrderStoreService orderStoreService;
    private final StringRedisTemplate redis;

    public OrderConfirmVO confirm(Long memberId, OrderPrepareRequest request) {
        MemberBriefDTO member = memberInnerClient.getBrief(memberId).dataOrThrow();
        List<Quote> quotes = loadQuotes(memberId, request.source(), request.items());
        Set<Long> categories = quotes.stream().map(quote -> quote.sku().categoryId()).collect(Collectors.toSet());
        Set<Long> products = quotes.stream().map(quote -> quote.sku().productId()).collect(Collectors.toSet());
        BigDecimal goods = quotes.stream().map(Quote::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal couponAmount = BigDecimal.ZERO;
        if (request.memberCouponId() != null) {
            couponAmount = previewCoupon(memberId, request.memberCouponId(), goods, categories, products);
        }
        int requested = request.useIntegration() == null ? 0 : request.useIntegration();
        OrderPricing.Priced priced = OrderPricing.price(toLines(quotes), couponAmount, requested, member.integration());
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(tokenKey(memberId, token), "1", TOKEN_TTL);
        List<CouponCandidateVO> coupons = couponService.usable(memberId, goods, categories, products).stream()
                .map(item -> toCandidate(item, goods, categories, products)).toList();
        return new OrderConfirmVO(token, toItemViews(quotes, priced), coupons, member.integration(),
                priced.maxUseIntegration(), priced.goodsAmount(), BigDecimal.ZERO, priced.couponAmount(),
                priced.integrationAmount(), priced.payAmount(), priced.useIntegration());
    }

    public String submit(Long memberId, OrderSubmitRequest request) {
        String removed = redis.opsForValue().getAndDelete(tokenKey(memberId, request.submitToken()));
        BizException.throwIf(removed == null, TradeErrorCode.ORDER_TOKEN_INVALID);

        MemberBriefDTO member = memberInnerClient.getBrief(memberId).dataOrThrow();
        MemberAddressDTO address = memberInnerClient.getAddress(memberId, request.addressId()).dataOrThrow();
        List<Quote> quotes = loadQuotes(memberId, request.source(), request.items());
        Set<Long> categories = quotes.stream().map(quote -> quote.sku().categoryId()).collect(Collectors.toSet());
        Set<Long> products = quotes.stream().map(quote -> quote.sku().productId()).collect(Collectors.toSet());
        long orderId = IdWorker.getId();
        String orderSn = LocalDate.now().format(DAY) + orderId;
        List<StockItemDTO> stockItems = quotes.stream()
                .map(quote -> new StockItemDTO(quote.sku().skuId(), quote.quantity())).toList();

        boolean couponLocked = false;
        boolean pointsAttempted = false;
        boolean stockAttempted = false;
        int usedPoints = 0;
        try {
            BigDecimal couponAmount = BigDecimal.ZERO;
            if (request.memberCouponId() != null) {
                couponAmount = couponService.lockForOrder(memberId, request.memberCouponId(), orderSn,
                        quotes.stream().map(Quote::amount).reduce(BigDecimal.ZERO, BigDecimal::add), categories, products);
                couponLocked = true;
            }
            int requested = request.useIntegration() == null ? 0 : request.useIntegration();
            OrderPricing.Priced priced = OrderPricing.price(toLines(quotes), couponAmount, requested, member.integration());
            usedPoints = priced.useIntegration();
            if (usedPoints > 0) {
                // 先标记再调用。调用超时不代表对方没记上，补偿时按流水唯一键退回。
                pointsAttempted = true;
                integrationInnerClient.deduct(memberId, new IntegrationChangeRequest(orderSn, usedPoints)).dataOrThrow();
            }
            stockAttempted = true;
            stockInnerClient.lock(new StockLockRequest(orderSn, stockItems)).dataOrThrow();
            orderStoreService.save(newOrder(orderId, orderSn, member, address, request, priced, quotes),
                    newItems(orderId, orderSn, quotes, priced),
                    request.source() == OrderSource.CART ? stockItems.stream().map(StockItemDTO::skuId).toList() : List.of());
            return orderSn;
        } catch (RuntimeException e) {
            compensate(orderSn, memberId, stockItems, stockAttempted, pointsAttempted, usedPoints, couponLocked);
            throw e;
        }
    }

    private void compensate(String orderSn, Long memberId, List<StockItemDTO> items, boolean stock, boolean points,
                            int pointAmount, boolean coupon) {
        if (stock) {
            try {
                stockInnerClient.release(new StockReleaseRequest(orderSn, items)).dataOrThrow();
            } catch (RuntimeException e) {
                log.error("补偿释放库存失败，orderSn={}", orderSn, e);
            }
        }
        if (points) {
            try {
                integrationInnerClient.refund(memberId, new IntegrationChangeRequest(orderSn, pointAmount)).dataOrThrow();
            } catch (RuntimeException e) {
                log.error("补偿返还积分失败，orderSn={}", orderSn, e);
            }
        }
        if (coupon) {
            try {
                couponService.unlock(orderSn);
            } catch (RuntimeException e) {
                log.error("补偿解锁优惠券失败，orderSn={}", orderSn, e);
            }
        }
    }

    private List<Quote> loadQuotes(Long memberId, OrderSource source, List<OrderSkuRequest> requested) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        if (source == OrderSource.CART) {
            List<CartItem> cart = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getMemberId, memberId)
                    .eq(CartItem::getChecked, true));
            BizException.throwIf(cart.isEmpty(), TradeErrorCode.ORDER_ITEM_EMPTY);
            cart.forEach(item -> quantities.put(item.getSkuId(), item.getQuantity()));
        } else {
            BizException.throwIf(requested == null || requested.isEmpty(), TradeErrorCode.ORDER_ITEM_EMPTY);
            for (OrderSkuRequest item : requested) {
                quantities.merge(item.skuId(), item.quantity(), Integer::sum);
            }
        }
        Map<Long, SkuInfoDTO> skus = skuInnerClient.listByIds(new ArrayList<>(quantities.keySet())).dataOrThrow()
                .stream().collect(Collectors.toMap(SkuInfoDTO::skuId, Function.identity()));
        List<Quote> quotes = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            SkuInfoDTO sku = skus.get(entry.getKey());
            BizException.throwIf(sku == null, TradeErrorCode.SKU_NOT_FOUND);
            BizException.throwIf(!sku.onSale(), TradeErrorCode.SKU_OFF_SHELF);
            BizException.throwIf(sku.availableStock() < entry.getValue(), TradeErrorCode.STOCK_NOT_ENOUGH);
            quotes.add(new Quote(sku, entry.getValue()));
        }
        return quotes;
    }

    private BigDecimal previewCoupon(Long memberId, Long memberCouponId, BigDecimal goods, Set<Long> categories, Set<Long> products) {
        MemberCoupon mine = couponService.listMine(memberId, null).stream()
                .filter(item -> item.getId().equals(memberCouponId)).findFirst().orElse(null);
        BizException.throwIf(mine == null || mine.getStatus() != MemberCouponStatus.UNUSED, TradeErrorCode.COUPON_NOT_FOUND);
        Coupon coupon = couponService.requireCoupon(mine.getCouponId());
        BizException.throwIf(!couponService.applicable(coupon, goods, categories, products, true), TradeErrorCode.COUPON_NOT_APPLICABLE);
        return coupon.getAmount();
    }

    private CouponCandidateVO toCandidate(MemberCoupon item, BigDecimal goods, Set<Long> categories, Set<Long> products) {
        Coupon coupon = couponService.requireCoupon(item.getCouponId());
        return new CouponCandidateVO(item.getId(), coupon.getId(), coupon.getName(), coupon.getAmount(), coupon.getMinPoint(),
                couponService.applicable(coupon, goods, categories, products, true));
    }

    private static List<OrderPricing.Line> toLines(List<Quote> quotes) {
        return quotes.stream().map(quote -> new OrderPricing.Line(quote.sku().skuId(), quote.sku().price(), quote.quantity())).toList();
    }

    private static List<OrderItemVO> toItemViews(List<Quote> quotes, OrderPricing.Priced priced) {
        Map<Long, OrderPricing.Share> shares = priced.shares().stream()
                .collect(Collectors.toMap(OrderPricing.Share::skuId, Function.identity()));
        return quotes.stream().map(quote -> {
            OrderPricing.Share share = shares.get(quote.sku().skuId());
            return new OrderItemVO(quote.sku().skuId(), quote.sku().productName(), quote.sku().pic(), quote.sku().specs(),
                    quote.sku().price(), quote.quantity(), share.realAmount(), null);
        }).toList();
    }

    private static TradeOrder newOrder(long orderId, String orderSn, MemberBriefDTO member, MemberAddressDTO address,
                                       OrderSubmitRequest request, OrderPricing.Priced priced, List<Quote> quotes) {
        TradeOrder order = new TradeOrder();
        order.setId(orderId);
        order.setOrderSn(orderSn);
        order.setMemberId(member.id());
        order.setMemberUsername(member.username());
        order.setOrderType(0);
        order.setStatus(OrderStatus.WAIT_PAY);
        order.setTotalAmount(priced.goodsAmount());
        order.setFreightAmount(BigDecimal.ZERO);
        order.setCouponAmount(priced.couponAmount());
        order.setIntegrationAmount(priced.integrationAmount());
        order.setPayAmount(priced.payAmount());
        order.setMemberCouponId(request.memberCouponId());
        order.setUseIntegration(priced.useIntegration());
        order.setRewardIntegration(quotes.stream().mapToInt(quote -> quote.sku().giftIntegration() * quote.quantity()).sum());
        order.setReceiverName(address.receiverName());
        order.setReceiverPhone(address.receiverPhone());
        order.setReceiverProvince(address.province());
        order.setReceiverCity(address.city());
        order.setReceiverRegion(address.region());
        order.setReceiverDetailAddress(address.detailAddress());
        order.setReceiverPostCode(address.postCode());
        order.setNote(request.note());
        order.setMemberDeleted(false);
        return order;
    }

    private static List<TradeOrderItem> newItems(long orderId, String orderSn, List<Quote> quotes, OrderPricing.Priced priced) {
        Map<Long, OrderPricing.Share> shares = priced.shares().stream()
                .collect(Collectors.toMap(OrderPricing.Share::skuId, Function.identity()));
        List<TradeOrderItem> items = new ArrayList<>();
        for (Quote quote : quotes) {
            OrderPricing.Share share = shares.get(quote.sku().skuId());
            TradeOrderItem item = new TradeOrderItem();
            item.setOrderId(orderId);
            item.setOrderSn(orderSn);
            item.setProductId(quote.sku().productId());
            item.setSkuId(quote.sku().skuId());
            item.setProductName(quote.sku().productName());
            item.setProductPic(quote.sku().pic());
            item.setSkuCode(quote.sku().skuCode());
            item.setSpecData(quote.sku().specs());
            item.setBrandName(quote.sku().brandName());
            item.setCategoryId(quote.sku().categoryId());
            item.setPrice(quote.sku().price());
            item.setQuantity(quote.quantity());
            item.setCouponAmount(share.couponAmount());
            item.setIntegrationAmount(share.integrationAmount());
            item.setRealAmount(share.realAmount());
            items.add(item);
        }
        return items;
    }

    private static String tokenKey(Long memberId, String token) {
        return "booth:order:token:" + memberId + ":" + token;
    }

    private record Quote(SkuInfoDTO sku, int quantity) {
        BigDecimal amount() {
            return sku.price().multiply(BigDecimal.valueOf(quantity));
        }
    }
}
