package com.booth.api.user;

import com.booth.api.user.dto.IntegrationChangeRequest;
import com.booth.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 幂等键是 (bizType, bizNo)，bizNo 使用订单号。重复调用不会重复加减积分。
 */
@FeignClient(name = "booth-user", contextId = "integrationInnerClient", path = "/inner/members")
public interface IntegrationInnerClient {

    @PostMapping("/{memberId}/integration/deduct")
    Result<Void> deduct(@PathVariable("memberId") Long memberId, @RequestBody IntegrationChangeRequest request);

    @PostMapping("/{memberId}/integration/refund")
    Result<Void> refund(@PathVariable("memberId") Long memberId, @RequestBody IntegrationChangeRequest request);

    @PostMapping("/{memberId}/integration/reward")
    Result<Void> reward(@PathVariable("memberId") Long memberId, @RequestBody IntegrationChangeRequest request);
}
