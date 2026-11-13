package com.booth.api.user;

import com.booth.api.user.dto.MemberAddressDTO;
import com.booth.api.user.dto.MemberBriefDTO;
import com.booth.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booth-user", contextId = "memberInnerClient", path = "/inner/members")
public interface MemberInnerClient {

    @GetMapping("/{memberId}")
    Result<MemberBriefDTO> getBrief(@PathVariable("memberId") Long memberId);

    /**
     * 地址必须属于该会员，否则返回 NOT_FOUND，防止用别人的地址下单。
     */
    @GetMapping("/{memberId}/addresses/{addressId}")
    Result<MemberAddressDTO> getAddress(@PathVariable("memberId") Long memberId,
                                        @PathVariable("addressId") Long addressId);
}
