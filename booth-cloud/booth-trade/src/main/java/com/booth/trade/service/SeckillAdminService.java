package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.api.product.SkuInnerClient;
import com.booth.api.product.dto.SkuInfoDTO;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.trade.dto.SeckillActivityRequest;
import com.booth.trade.dto.SeckillSessionRequest;
import com.booth.trade.dto.SeckillSkuRequest;
import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillOrder;
import com.booth.trade.entity.SeckillSession;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.enums.SeckillRecordStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.SeckillActivityMapper;
import com.booth.trade.mapper.SeckillOrderMapper;
import com.booth.trade.mapper.SeckillSessionMapper;
import com.booth.trade.mapper.SeckillSkuMapper;
import com.booth.trade.support.SeckillKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillAdminService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillSessionMapper sessionMapper;
    private final SeckillSkuMapper skuMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SkuInnerClient skuInnerClient;
    private final StringRedisTemplate redis;

    public PageResult<SeckillActivity> pageActivities(long pageNum, long pageSize) {
        return PageResults.of(activityMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<SeckillActivity>lambdaQuery().orderByDesc(SeckillActivity::getId)));
    }

    public Long createActivity(SeckillActivityRequest request) {
        checkDates(request);
        SeckillActivity activity = new SeckillActivity();
        activity.setTitle(request.title());
        activity.setStartDate(request.startDate());
        activity.setEndDate(request.endDate());
        activity.setStatus(ReleaseStatus.DRAFT);
        activityMapper.insert(activity);
        return activity.getId();
    }

    public void updateActivity(Long id, SeckillActivityRequest request) {
        checkDates(request);
        SeckillActivity activity = requireActivity(id);
        boolean datesChanged = !activity.getStartDate().equals(request.startDate())
                || !activity.getEndDate().equals(request.endDate());
        BizException.throwIf(activity.getStatus() != ReleaseStatus.DRAFT && datesChanged, TradeErrorCode.SECKILL_IMMUTABLE);
        activity.setTitle(request.title());
        activity.setStartDate(request.startDate());
        activity.setEndDate(request.endDate());
        activityMapper.updateById(activity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long id) {
        SeckillActivity activity = requireActivity(id);
        BizException.throwIf(activity.getStatus() != ReleaseStatus.DRAFT, TradeErrorCode.SECKILL_IMMUTABLE, "只有草稿活动可以删除");
        skuMapper.delete(Wrappers.<SeckillSku>lambdaQuery().eq(SeckillSku::getActivityId, id));
        activityMapper.deleteById(id);
    }

    public void publish(Long id) {
        int updated = activityMapper.update(null, Wrappers.<SeckillActivity>lambdaUpdate()
                .set(SeckillActivity::getStatus, ReleaseStatus.RELEASED)
                .eq(SeckillActivity::getId, id)
                .eq(SeckillActivity::getStatus, ReleaseStatus.DRAFT));
        BizException.throwIf(updated == 0, TradeErrorCode.SECKILL_IMMUTABLE);
    }

    public void offline(Long id) {
        int updated = activityMapper.update(null, Wrappers.<SeckillActivity>lambdaUpdate()
                .set(SeckillActivity::getStatus, ReleaseStatus.OFFLINE)
                .eq(SeckillActivity::getId, id)
                .eq(SeckillActivity::getStatus, ReleaseStatus.RELEASED));
        BizException.throwIf(updated == 0, TradeErrorCode.SECKILL_IMMUTABLE);
    }

    public List<SeckillSession> sessions() {
        return sessionMapper.selectList(Wrappers.<SeckillSession>lambdaQuery().orderByAsc(SeckillSession::getStartTime));
    }

    public Long createSession(SeckillSessionRequest request) {
        checkSession(request);
        SeckillSession session = new SeckillSession();
        fillSession(session, request);
        sessionMapper.insert(session);
        return session.getId();
    }

    public void updateSession(Long id, SeckillSessionRequest request) {
        checkSession(request);
        SeckillSession session = requireSession(id);
        fillSession(session, request);
        sessionMapper.updateById(session);
    }

    public void deleteSession(Long id) {
        requireSession(id);
        long used = skuMapper.selectCount(Wrappers.<SeckillSku>lambdaQuery().eq(SeckillSku::getSessionId, id));
        BizException.throwIf(used > 0, TradeErrorCode.SECKILL_IMMUTABLE, "场次下还有秒杀商品，不能删除");
        sessionMapper.deleteById(id);
    }

    public List<SeckillSku> skus(Long activityId) {
        return skuMapper.selectList(Wrappers.<SeckillSku>lambdaQuery()
                .eq(activityId != null, SeckillSku::getActivityId, activityId)
                .orderByAsc(SeckillSku::getSort)
                .orderByAsc(SeckillSku::getId));
    }

    public Long createSku(SeckillSkuRequest request) {
        SeckillActivity activity = requireActivity(request.activityId());
        BizException.throwIf(activity.getStatus() == ReleaseStatus.OFFLINE, TradeErrorCode.SECKILL_IMMUTABLE);
        requireSession(request.sessionId());
        SkuInfoDTO sku = requireRealSku(request);
        SeckillSku row = new SeckillSku();
        row.setActivityId(request.activityId());
        row.setSessionId(request.sessionId());
        row.setProductId(sku.productId());
        row.setSkuId(request.skuId());
        row.setProductName(sku.productName());
        row.setProductPic(sku.pic());
        row.setSeckillPrice(request.seckillPrice());
        row.setSeckillStock(request.seckillStock());
        row.setSeckillStockInitial(request.seckillStock());
        row.setPerLimit(request.perLimit());
        row.setSort(request.sort() == null ? 0 : request.sort());
        skuMapper.insert(row);
        return row.getId();
    }

    public void updateSku(Long id, SeckillSkuRequest request) {
        SeckillSku row = requireSku(id);
        BizException.throwIf(!row.getActivityId().equals(request.activityId())
                || !row.getSessionId().equals(request.sessionId())
                || !row.getSkuId().equals(request.skuId()), TradeErrorCode.SECKILL_IMMUTABLE, "不能改秒杀商品关联的活动和 SKU");
        boolean stockChanged = !row.getSeckillStock().equals(request.seckillStock());
        BizException.throwIf(stockChanged && Boolean.TRUE.equals(redis.hasKey(SeckillKeys.stock(id))),
                TradeErrorCode.SECKILL_IMMUTABLE, "已经预热的秒杀库存不能修改，避免覆盖 Redis 里剩余的数量");
        SkuInfoDTO sku = requireRealSku(request);
        row.setProductName(sku.productName());
        row.setProductPic(sku.pic());
        row.setSeckillPrice(request.seckillPrice());
        row.setPerLimit(request.perLimit());
        row.setSort(request.sort() == null ? 0 : request.sort());
        skuMapper.updateById(row);
        if (stockChanged) {
            skuMapper.update(null, Wrappers.<SeckillSku>lambdaUpdate()
                    .set(SeckillSku::getSeckillStock, request.seckillStock())
                    .set(SeckillSku::getSeckillStockInitial, request.seckillStock())
                    .eq(SeckillSku::getId, id));
        }
        if (Boolean.TRUE.equals(redis.hasKey(SeckillKeys.info(id)))) {
            redis.opsForHash().put(SeckillKeys.info(id), "price", request.seckillPrice().toPlainString());
            redis.opsForHash().put(SeckillKeys.info(id), "limit", String.valueOf(request.perLimit()));
        }
    }

    public void deleteSku(Long id) {
        SeckillSku row = requireSku(id);
        SeckillActivity activity = requireActivity(row.getActivityId());
        BizException.throwIf(activity.getStatus() != ReleaseStatus.DRAFT, TradeErrorCode.SECKILL_IMMUTABLE, "只有草稿活动里的商品可以删除");
        long orders = seckillOrderMapper.selectCount(Wrappers.<SeckillOrder>lambdaQuery()
                .eq(SeckillOrder::getSeckillSkuId, id)
                .eq(SeckillOrder::getStatus, SeckillRecordStatus.ACTIVE));
        BizException.throwIf(orders > 0, TradeErrorCode.SECKILL_IMMUTABLE);
        skuMapper.deleteById(id);
    }

    public SeckillActivity requireActivity(Long id) {
        SeckillActivity activity = activityMapper.selectById(id);
        BizException.throwIf(activity == null, TradeErrorCode.SECKILL_NOT_FOUND);
        return activity;
    }

    public SeckillSession requireSession(Long id) {
        SeckillSession session = sessionMapper.selectById(id);
        BizException.throwIf(session == null, TradeErrorCode.SECKILL_NOT_FOUND);
        return session;
    }

    public SeckillSku requireSku(Long id) {
        SeckillSku sku = skuMapper.selectById(id);
        BizException.throwIf(sku == null, TradeErrorCode.SECKILL_NOT_FOUND);
        return sku;
    }

    private SkuInfoDTO requireRealSku(SeckillSkuRequest request) {
        List<SkuInfoDTO> found = skuInnerClient.listByIds(List.of(request.skuId())).dataOrThrow();
        BizException.throwIf(found.isEmpty(), TradeErrorCode.SKU_NOT_FOUND);
        SkuInfoDTO sku = found.get(0);
        BizException.throwIf(!sku.onSale(), TradeErrorCode.SKU_OFF_SHELF);
        BizException.throwIf(request.seckillStock() > sku.availableStock(), TradeErrorCode.SECKILL_STOCK_INVALID);
        return sku;
    }

    private static void checkDates(SeckillActivityRequest request) {
        BizException.throwIf(request.endDate().isBefore(request.startDate()), TradeErrorCode.SECKILL_IMMUTABLE, "结束日期不能早于开始日期");
    }

    private static void checkSession(SeckillSessionRequest request) {
        BizException.throwIf(!request.endTime().isAfter(request.startTime()), TradeErrorCode.SECKILL_IMMUTABLE, "场次结束时间必须晚于开始时间");
    }

    private static void fillSession(SeckillSession session, SeckillSessionRequest request) {
        session.setName(request.name());
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setStatus(request.status() == null || request.status());
    }
}
