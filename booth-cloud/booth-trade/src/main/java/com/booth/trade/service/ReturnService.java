package com.booth.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.trade.dto.HandleNoteRequest;
import com.booth.trade.dto.ReturnApplyRequest;
import com.booth.trade.entity.OrderReturnApply;
import com.booth.trade.entity.TradeOrder;
import com.booth.trade.entity.TradeOrderItem;
import com.booth.trade.enums.OrderStatus;
import com.booth.trade.enums.ReturnStatus;
import com.booth.trade.error.TradeErrorCode;
import com.booth.trade.mapper.OrderReturnApplyMapper;
import com.booth.trade.vo.ReturnApplyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final OrderReturnApplyMapper returnApplyMapper;
    private final OrderLifecycleService lifecycleService;

    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long memberId, String orderSn, ReturnApplyRequest request) {
        TradeOrder order = lifecycleService.requireMember(memberId, orderSn);
        BizException.throwIf(order.getStatus() != OrderStatus.COMPLETED, TradeErrorCode.RETURN_ORDER_INVALID);
        TradeOrderItem item = lifecycleService.items(order.getId()).stream()
                .filter(row -> row.getId().equals(request.orderItemId())).findFirst().orElse(null);
        BizException.throwIf(item == null, TradeErrorCode.RETURN_ORDER_INVALID);
        OrderReturnApply apply = new OrderReturnApply();
        apply.setOrderId(order.getId());
        apply.setOrderItemId(item.getId());
        apply.setOrderSn(orderSn);
        apply.setMemberId(memberId);
        apply.setReturnQuantity(item.getQuantity());
        apply.setReturnAmount(item.getRealAmount());
        apply.setReason(request.reason());
        apply.setDescription(request.description());
        apply.setProofPics(request.proofPics());
        apply.setStatus(ReturnStatus.PENDING);
        try {
            returnApplyMapper.insert(apply);
        } catch (DuplicateKeyException e) {
            throw new BizException(TradeErrorCode.RETURN_EXISTS);
        }
        return apply.getId();
    }

    public List<ReturnApplyVO> mine(Long memberId) {
        return returnApplyMapper.selectList(Wrappers.<OrderReturnApply>lambdaQuery()
                        .eq(OrderReturnApply::getMemberId, memberId)
                        .orderByDesc(OrderReturnApply::getId))
                .stream().map(this::toVO).toList();
    }

    public List<ReturnApplyVO> adminList(ReturnStatus status) {
        return returnApplyMapper.selectList(Wrappers.<OrderReturnApply>lambdaQuery()
                        .eq(status != null, OrderReturnApply::getStatus, status)
                        .orderByDesc(OrderReturnApply::getId)
                        .last("LIMIT 100"))
                .stream().map(this::toVO).toList();
    }

    public void approve(Long id, Long adminId, HandleNoteRequest request) {
        transit(id, ReturnStatus.PENDING, ReturnStatus.RETURNING, adminId, request == null ? null : request.note(), true);
    }

    public void reject(Long id, Long adminId, HandleNoteRequest request) {
        transit(id, ReturnStatus.PENDING, ReturnStatus.REJECTED, adminId, request == null ? null : request.note(), true);
    }

    public void receive(Long id, Long adminId, HandleNoteRequest request) {
        transit(id, ReturnStatus.RETURNING, ReturnStatus.COMPLETED, adminId, request == null ? null : request.note(), false);
    }

    private void transit(Long id, ReturnStatus from, ReturnStatus to, Long adminId, String note, boolean handle) {
        var update = Wrappers.<OrderReturnApply>lambdaUpdate()
                .set(OrderReturnApply::getStatus, to)
                .eq(OrderReturnApply::getId, id)
                .eq(OrderReturnApply::getStatus, from);
        if (handle) {
            update.set(OrderReturnApply::getHandleAdmin, String.valueOf(adminId))
                    .set(OrderReturnApply::getHandleNote, note)
                    .set(OrderReturnApply::getHandleTime, LocalDateTime.now());
        } else {
            update.set(OrderReturnApply::getReceiveAdmin, String.valueOf(adminId))
                    .set(OrderReturnApply::getReceiveNote, note)
                    .set(OrderReturnApply::getReceiveTime, LocalDateTime.now());
        }
        int changed = returnApplyMapper.update(null, update);
        BizException.throwIf(changed == 0, TradeErrorCode.RETURN_STATUS_CHANGED);
    }

    private ReturnApplyVO toVO(OrderReturnApply apply) {
        return new ReturnApplyVO(apply.getId(), apply.getOrderSn(), apply.getOrderItemId(), apply.getReturnQuantity(),
                apply.getReturnAmount(), apply.getReason(), apply.getDescription(), apply.getStatus(),
                apply.getHandleNote(), apply.getCreateTime());
    }
}
