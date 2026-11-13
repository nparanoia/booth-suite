package com.booth.user.controller.inner;

import com.booth.api.user.dto.IntegrationChangeRequest;
import com.booth.api.user.dto.MemberAddressDTO;
import com.booth.api.user.dto.MemberBriefDTO;
import com.booth.common.core.result.Result;
import com.booth.user.service.AddressService;
import com.booth.user.service.IntegrationService;
import com.booth.user.service.MemberService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实现 {@link com.booth.api.user.MemberInnerClient}，路径和参数必须与它保持一致。
 */
@Hidden
@RestController
@RequestMapping("/inner/members")
@RequiredArgsConstructor
public class MemberInnerController {

    private final MemberService memberService;
    private final AddressService addressService;
    private final IntegrationService integrationService;

    @GetMapping("/{memberId}")
    public Result<MemberBriefDTO> getBrief(@PathVariable Long memberId) {
        return Result.ok(memberService.getBrief(memberId));
    }

    @GetMapping("/{memberId}/addresses/{addressId}")
    public Result<MemberAddressDTO> getAddress(@PathVariable Long memberId, @PathVariable Long addressId) {
        return Result.ok(addressService.getForOrder(memberId, addressId));
    }

    @PostMapping("/{memberId}/integration/deduct")
    public Result<Void> deduct(@PathVariable Long memberId, @Valid @RequestBody IntegrationChangeRequest request) {
        integrationService.deduct(memberId, request.bizNo(), request.amount());
        return Result.ok();
    }

    @PostMapping("/{memberId}/integration/refund")
    public Result<Void> refund(@PathVariable Long memberId, @Valid @RequestBody IntegrationChangeRequest request) {
        integrationService.refund(memberId, request.bizNo(), request.amount());
        return Result.ok();
    }

    @PostMapping("/{memberId}/integration/reward")
    public Result<Void> reward(@PathVariable Long memberId, @Valid @RequestBody IntegrationChangeRequest request) {
        integrationService.reward(memberId, request.bizNo(), request.amount());
        return Result.ok();
    }
}
