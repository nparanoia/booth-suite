package com.booth.trade.support;

import com.booth.trade.entity.SeckillActivity;
import com.booth.trade.entity.SeckillSession;
import com.booth.trade.enums.ReleaseStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class SeckillWindow {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private SeckillWindow() {
    }

    public static boolean covers(SeckillActivity activity, LocalDate day) {
        return !day.isBefore(activity.getStartDate()) && !day.isAfter(activity.getEndDate());
    }

    public static boolean openNow(SeckillSession session) {
        LocalTime now = LocalTime.now(ZONE);
        return !now.isBefore(session.getStartTime()) && !now.isAfter(session.getEndTime());
    }

    /** 开始前 10 分钟到活动最后一天的场次结束，这段时间可以预热。 */
    public static boolean warmupDue(SeckillActivity activity, SeckillSession session) {
        if (activity.getStatus() != ReleaseStatus.RELEASED || !Boolean.TRUE.equals(session.getStatus())) {
            return false;
        }
        LocalDate today = LocalDate.now(ZONE);
        if (!covers(activity, today)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime from = today.atTime(session.getStartTime()).minusMinutes(10);
        LocalDateTime to = today.atTime(session.getEndTime());
        return !now.isBefore(from) && !now.isAfter(to);
    }

    public static long startMillis(LocalTime time) {
        return LocalDate.now(ZONE).atTime(time).atZone(ZONE).toInstant().toEpochMilli();
    }

    public static Duration ttl(SeckillActivity activity, SeckillSession session) {
        LocalDateTime expireAt = activity.getEndDate().atTime(session.getEndTime()).plusDays(1);
        Duration ttl = Duration.between(LocalDateTime.now(ZONE), expireAt);
        if (ttl.isZero() || ttl.isNegative() || ttl.getSeconds() < 60) {
            return Duration.ofDays(1);
        }
        return ttl;
    }
}
