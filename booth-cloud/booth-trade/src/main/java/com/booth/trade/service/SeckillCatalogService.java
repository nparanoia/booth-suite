package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillSession;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.mapper.SeckillActivityMapper;
import com.booth.trade.mapper.SeckillSessionMapper;
import com.booth.trade.mapper.SeckillSkuMapper;
import com.booth.trade.support.SeckillKeys;
import com.booth.trade.support.SeckillWindow;
import com.booth.trade.vo.SeckillSessionVO;
import com.booth.trade.vo.SeckillSkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillCatalogService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillSessionMapper sessionMapper;
    private final SeckillSkuMapper skuMapper;
    private final StringRedisTemplate redis;

    public List<SeckillSessionVO> current() {
        LocalDate today = LocalDate.now(SeckillWindow.ZONE);
        List<SeckillActivity> activities = activityMapper.selectList(Wrappers.<SeckillActivity>lambdaQuery()
                .eq(SeckillActivity::getStatus, ReleaseStatus.RELEASED)
                .le(SeckillActivity::getStartDate, today)
                .ge(SeckillActivity::getEndDate, today));
        if (activities.isEmpty()) {
            return List.of();
        }
        List<Long> activityIds = activities.stream().map(SeckillActivity::getId).toList();
        List<SeckillSku> skus = skuMapper.selectList(Wrappers.<SeckillSku>lambdaQuery()
                .in(SeckillSku::getActivityId, activityIds)
                .orderByAsc(SeckillSku::getSort));
        List<SeckillSessionVO> views = new ArrayList<>();
        for (SeckillSession session : sessionMapper.selectList(Wrappers.<SeckillSession>lambdaQuery()
                .eq(SeckillSession::getStatus, true)
                .orderByAsc(SeckillSession::getStartTime))) {
            List<SeckillSkuVO> items = skus.stream()
                    .filter(sku -> sku.getSessionId().equals(session.getId()))
                    .map(this::toView)
                    .toList();
            if (!items.isEmpty()) {
                views.add(new SeckillSessionVO(session.getId(), session.getName(), session.getStartTime(),
                        session.getEndTime(), SeckillWindow.openNow(session), items));
            }
        }
        return views;
    }

    public SeckillSkuVO detail(Long id) {
        SeckillSku sku = skuMapper.selectById(id);
        if (sku == null) {
            return null;
        }
        return toView(sku);
    }

    public Integer remain(Long seckillSkuId) {
        String value = redis.opsForValue().get(SeckillKeys.stock(seckillSkuId));
        return value == null ? null : Integer.valueOf(value);
    }

    private SeckillSkuVO toView(SeckillSku sku) {
        return new SeckillSkuVO(sku.getId(), sku.getActivityId(), sku.getSessionId(), sku.getSkuId(),
                sku.getProductName(), sku.getProductPic(), sku.getSeckillPrice(), sku.getSeckillStock(),
                remain(sku.getId()), sku.getPerLimit(), sku.getSort());
    }
}
