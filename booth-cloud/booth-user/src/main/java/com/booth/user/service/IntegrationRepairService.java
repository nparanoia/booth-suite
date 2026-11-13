package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.trade.OrderInnerClient;
import com.booth.api.trade.dto.OrderStatusDTO;
import com.booth.user.entity.MemberIntegrationLog;
import com.booth.user.mapper.MemberIntegrationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 扣减已经记账、退回却没写上的流水。订单不存在或已关闭时补退。
 * 只查还没有退回、也没有禁止扣减标记的流水，避免反复处理同一批旧数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationRepairService {

    private static final int CLOSED = 4;

    private final MemberIntegrationLogMapper logMapper;
    private final IntegrationService integrationService;
    private final OrderInnerClient orderInnerClient;

    public void refundClosedOrMissing() {
        List<MemberIntegrationLog> deducts = logMapper.selectList(Wrappers.<MemberIntegrationLog>lambdaQuery()
                .eq(MemberIntegrationLog::getBizType, IntegrationService.ORDER_DEDUCT)
                .lt(MemberIntegrationLog::getCreateTime, LocalDateTime.now().minusMinutes(2))
                .apply("NOT EXISTS (SELECT 1 FROM member_integration_log r WHERE r.biz_no = member_integration_log.biz_no AND r.biz_type = {0})",
                        IntegrationService.ORDER_REFUND)
                .apply("NOT EXISTS (SELECT 1 FROM member_integration_log a WHERE a.biz_no = member_integration_log.biz_no AND a.biz_type = {0})",
                        IntegrationService.ORDER_ABORT)
                .orderByAsc(MemberIntegrationLog::getId)
                .last("LIMIT 50"));
        for (MemberIntegrationLog deduct : deducts) {
            try {
                OrderStatusDTO status = orderInnerClient.status(deduct.getBizNo()).dataOrThrow();
                boolean closed = status.exists() && status.status() != null && status.status() == CLOSED;
                if (status.exists() && !closed) {
                    continue;
                }
                integrationService.refund(deduct.getMemberId(), deduct.getBizNo(), -deduct.getChangeAmount());
                log.info("对账退回积分，bizNo={}", deduct.getBizNo());
            } catch (RuntimeException e) {
                log.warn("对账退回积分失败，bizNo={}", deduct.getBizNo(), e);
            }
        }
    }
}
