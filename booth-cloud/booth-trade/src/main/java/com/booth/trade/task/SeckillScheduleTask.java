package com.booth.trade.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillSession;
import com.booth.trade.entity.SeckillSku;
import com.booth.trade.enums.ReleaseStatus;
import com.booth.trade.mapper.SeckillActivityMapper;
import com.booth.trade.mapper.SeckillSessionMapper;
import com.booth.trade.mapper.SeckillSkuMapper;
import com.booth.trade.service.SeckillReconcileService;
import com.booth.trade.service.SeckillRepairService;
import com.booth.trade.service.SeckillWarmupService;
import com.booth.trade.support.SeckillWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SeckillScheduleTask {

    private final SeckillWarmupService warmupService;
    private final SeckillRepairService repairService;
    private final SeckillReconcileService reconcileService;
    private final SeckillActivityMapper activityMapper;
    private final SeckillSessionMapper sessionMapper;
    private final SeckillSkuMapper skuMapper;

    @Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
    public void warmup() {
        warmupService.warmupDue();
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void repair() {
        repairService.repairUndelivered();
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 120_000)
    public void reconcileEnded() {
        LocalDate today = LocalDate.now(SeckillWindow.ZONE);
        LocalTime now = LocalTime.now(SeckillWindow.ZONE);
        List<SeckillActivity> activities = activityMapper.selectList(Wrappers.<SeckillActivity>lambdaQuery()
                .eq(SeckillActivity::getStatus, ReleaseStatus.RELEASED)
                .le(SeckillActivity::getStartDate, today)
                .ge(SeckillActivity::getEndDate, today));
        for (SeckillActivity activity : activities) {
            List<SeckillSku> skus = skuMapper.selectList(Wrappers.<SeckillSku>lambdaQuery()
                    .eq(SeckillSku::getActivityId, activity.getId()));
            for (SeckillSku sku : skus) {
                SeckillSession session = sessionMapper.selectById(sku.getSessionId());
                if (session != null && now.isAfter(session.getEndTime())) {
                    reconcileService.reconcile(sku.getId());
                }
            }
        }
    }
}
