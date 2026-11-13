package com.booth.trade.vo;

import java.time.LocalTime;
import java.util.List;

public record SeckillSessionVO(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        boolean ongoing,
        List<SeckillSkuVO> skus) {
}
