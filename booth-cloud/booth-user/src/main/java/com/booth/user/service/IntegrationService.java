package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.user.entity.Member;
import com.booth.user.entity.MemberIntegrationLog;
import com.booth.user.error.UserErrorCode;
import com.booth.user.mapper.MemberIntegrationLogMapper;
import com.booth.user.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分只和流水一起改。流水的 (biz_type, biz_no) 唯一，重复请求直接返回。
 * 先锁会员行再记账，保证 balance_after 和实际余额一致。
 * <p>
 * 扣减和退回必须在同一把会员行锁里判断。退回时如果扣减还没提交，先写 ORDER_ABORT；
 * 晚到的扣减看到这个标记就直接返回，不再改余额。
 */
@Service
@RequiredArgsConstructor
public class IntegrationService {

    public static final String ORDER_DEDUCT = "ORDER_DEDUCT";
    public static final String ORDER_REFUND = "ORDER_REFUND";
    public static final String ORDER_REWARD = "ORDER_REWARD";
    /** 订单没有扣成，用来拦住超时之后才到达的扣减 */
    public static final String ORDER_ABORT = "ORDER_ABORT";

    private final MemberMapper memberMapper;
    private final MemberIntegrationLogMapper logMapper;

    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long memberId, String bizNo, int amount) {
        lock(memberId);
        if (exists(ORDER_ABORT, bizNo) || exists(ORDER_DEDUCT, bizNo)) {
            return;
        }
        change(memberId, ORDER_DEDUCT, bizNo, -amount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void refund(Long memberId, String bizNo, int amount) {
        lock(memberId);
        if (exists(ORDER_REFUND, bizNo)) {
            return;
        }
        if (!exists(ORDER_DEDUCT, bizNo)) {
            insertAbort(memberId, bizNo);
            return;
        }
        change(memberId, ORDER_REFUND, bizNo, amount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reward(Long memberId, String bizNo, int amount) {
        lock(memberId);
        if (exists(ORDER_REWARD, bizNo)) {
            return;
        }
        change(memberId, ORDER_REWARD, bizNo, amount);
    }

    private void lock(Long memberId) {
        BizException.throwIf(memberMapper.lockById(memberId) == null, UserErrorCode.MEMBER_NOT_FOUND);
    }

    private void change(Long memberId, String bizType, String bizNo, int delta) {
        Member member = memberMapper.selectById(memberId);
        int after = member.getIntegration() + delta;
        BizException.throwIf(after < 0, UserErrorCode.INTEGRATION_NOT_ENOUGH);
        if (!insertLog(memberId, bizType, bizNo, delta, after, null)) {
            return;
        }
        int updated = memberMapper.update(null, Wrappers.<Member>lambdaUpdate()
                .setSql("integration = integration + (" + delta + ")")
                .eq(Member::getId, memberId)
                .apply(delta < 0, "integration >= {0}", -delta));
        BizException.throwIf(updated == 0, UserErrorCode.INTEGRATION_NOT_ENOUGH);
    }

    private void insertAbort(Long memberId, String bizNo) {
        if (exists(ORDER_ABORT, bizNo)) {
            return;
        }
        Member member = memberMapper.selectById(memberId);
        insertLog(memberId, ORDER_ABORT, bizNo, 0, member.getIntegration(), "下单未完成，禁止再扣这次积分");
    }

    /** @return false 表示唯一键冲突，调用方不要再改余额 */
    private boolean insertLog(Long memberId, String bizType, String bizNo, int delta, int balanceAfter, String note) {
        MemberIntegrationLog row = new MemberIntegrationLog();
        row.setMemberId(memberId);
        row.setChangeAmount(delta);
        row.setBalanceAfter(balanceAfter);
        row.setBizType(bizType);
        row.setBizNo(bizNo);
        row.setNote(note);
        try {
            logMapper.insert(row);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private boolean exists(String bizType, String bizNo) {
        return logMapper.exists(Wrappers.<MemberIntegrationLog>lambdaQuery()
                .eq(MemberIntegrationLog::getBizType, bizType)
                .eq(MemberIntegrationLog::getBizNo, bizNo));
    }
}
