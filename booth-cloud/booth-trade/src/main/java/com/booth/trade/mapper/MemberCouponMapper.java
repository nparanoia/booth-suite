package com.booth.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booth.trade.entity.MemberCoupon;
import org.apache.ibatis.annotations.Update;

public interface MemberCouponMapper extends BaseMapper<MemberCoupon> {

    @Update("""
            UPDATE member_coupon mc
                JOIN coupon c ON c.id = mc.coupon_id
            SET mc.status = 3
            WHERE mc.status = 0
              AND c.use_end_time < NOW(3)
            """)
    int expireUnused();
}
