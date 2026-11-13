package com.booth.user.controller.app;

import com.booth.common.core.constant.AuthHeaders;
import com.booth.common.core.result.Result;
import com.booth.common.satoken.StpKit;
import com.booth.user.dto.AddressSaveRequest;
import com.booth.user.dto.MemberProfileUpdateRequest;
import com.booth.user.service.AddressService;
import com.booth.user.service.MemberService;
import com.booth.user.vo.AddressVO;
import com.booth.user.vo.MemberProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前会员 ID 来自网关写入的身份头，请求体和路径里都不接收 memberId。
 */
@Tag(name = "前台-会员资料与收货地址")
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class MemberAppController {

    private final MemberService memberService;
    private final AddressService addressService;

    @Operation(summary = "我的资料")
    @GetMapping("/members/me")
    public Result<MemberProfileVO> me(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId) {
        return Result.ok(memberService.getProfile(memberId));
    }

    @Operation(summary = "退出登录", description = "只注销当前 Token，其他设备上的登录不受影响")
    @PostMapping("/members/logout")
    public Result<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        memberService.logout(StpKit.bearerToken(authorization));
        return Result.ok();
    }

    @Operation(summary = "修改我的资料")
    @PutMapping("/members/me")
    public Result<Void> updateMe(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                 @Valid @RequestBody MemberProfileUpdateRequest request) {
        memberService.updateProfile(memberId, request);
        return Result.ok();
    }

    @Operation(summary = "我的收货地址", description = "默认地址排在最前")
    @GetMapping("/addresses")
    public Result<List<AddressVO>> listAddresses(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId) {
        return Result.ok(addressService.list(memberId));
    }

    @Operation(summary = "新增收货地址", description = "第一个地址自动设为默认")
    @PostMapping("/addresses")
    public Result<Long> createAddress(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                      @Valid @RequestBody AddressSaveRequest request) {
        return Result.ok(addressService.create(memberId, request));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping("/addresses/{id}")
    public Result<Void> updateAddress(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                      @PathVariable Long id, @Valid @RequestBody AddressSaveRequest request) {
        addressService.update(memberId, id, request);
        return Result.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/addresses/{id}/default")
    public Result<Void> setDefault(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                   @PathVariable Long id) {
        addressService.setDefault(memberId, id);
        return Result.ok();
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@RequestHeader(AuthHeaders.MEMBER_ID) Long memberId,
                                      @PathVariable Long id) {
        addressService.delete(memberId, id);
        return Result.ok();
    }
}
