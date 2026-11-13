package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.api.user.dto.MemberBriefDTO;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.common.satoken.StpKit;
import com.booth.user.config.PasswordConfig.CredentialChecker;
import com.booth.user.convert.MemberConvert;
import com.booth.user.dto.LoginRequest;
import com.booth.user.dto.MemberProfileUpdateRequest;
import com.booth.user.dto.MemberQuery;
import com.booth.user.dto.MemberRegisterRequest;
import com.booth.user.entity.Member;
import com.booth.user.enums.AccountStatus;
import com.booth.user.enums.Gender;
import com.booth.user.error.UserErrorCode;
import com.booth.user.mapper.MemberMapper;
import com.booth.user.vo.MemberAdminVO;
import com.booth.user.vo.MemberProfileVO;
import com.booth.user.vo.TokenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final MemberConvert memberConvert;
    private final PasswordEncoder passwordEncoder;
    private final CredentialChecker credentialChecker;
    private final LoginRateLimiter loginRateLimiter;

    /**
     * 先查重给出明确提示；并发注册时查重可能都通过，由唯一索引兜底。
     */
    public Long register(MemberRegisterRequest request) {
        BizException.throwIf(memberMapper.exists(Wrappers.<Member>lambdaQuery()
                .eq(Member::getUsername, request.username())), UserErrorCode.USERNAME_EXISTS);
        BizException.throwIf(memberMapper.exists(Wrappers.<Member>lambdaQuery()
                .eq(Member::getPhone, request.phone())), UserErrorCode.PHONE_EXISTS);

        Member member = new Member();
        member.setUsername(request.username());
        member.setPassword(passwordEncoder.encode(request.password()));
        member.setPhone(request.phone());
        member.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname() : request.username());
        member.setGender(Gender.UNKNOWN);
        member.setStatus(AccountStatus.ENABLED);
        try {
            memberMapper.insert(member);
        } catch (DuplicateKeyException e) {
            boolean phoneConflict = e.getMessage() != null && e.getMessage().contains("uk_phone");
            throw new BizException(phoneConflict ? UserErrorCode.PHONE_EXISTS : UserErrorCode.USERNAME_EXISTS);
        }
        return member.getId();
    }

    public TokenVO login(LoginRequest request, String clientIp) {
        loginRateLimiter.guard("member", clientIp);
        Member member = memberMapper.selectOne(Wrappers.<Member>lambdaQuery()
                .eq(Member::getUsername, request.username()));
        boolean passwordOk = credentialChecker.matches(request.password(), member == null ? null : member.getPassword());
        if (!passwordOk) {
            loginRateLimiter.recordFail("member", clientIp);
            throw new BizException(UserErrorCode.LOGIN_FAILED);
        }
        if (member.getStatus() != AccountStatus.ENABLED) {
            loginRateLimiter.recordFail("member", clientIp);
            throw new BizException(UserErrorCode.MEMBER_DISABLED);
        }
        String token = StpKit.MEMBER.createLoginSession(member.getId());
        loginRateLimiter.clear("member", clientIp);
        return new TokenVO(token, StpKit.MEMBER.getTokenTimeout(token));
    }

    public void logout(String token) {
        if (token != null) {
            StpKit.MEMBER.logoutByTokenValue(token);
        }
    }

    public MemberProfileVO getProfile(Long memberId) {
        return memberConvert.toProfileVO(require(memberId));
    }

    public void updateProfile(Long memberId, MemberProfileUpdateRequest request) {
        Member member = require(memberId);
        memberConvert.update(member, request);
        memberMapper.updateById(member);
    }

    public PageResult<MemberAdminVO> page(MemberQuery query) {
        String keyword = query.getKeyword();
        Page<Member> page = memberMapper.selectPage(query.toPage(), Wrappers.<Member>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(Member::getUsername, keyword)
                        .or().like(Member::getPhone, keyword))
                .eq(query.getStatus() != null, Member::getStatus, query.getStatus())
                .orderByDesc(Member::getId));
        return PageResults.of(page, memberConvert::toAdminVO);
    }

    /**
     * 禁用后立即踢下线：该会员所有已签发的 Token 马上失效。
     */
    public void updateStatus(Long memberId, AccountStatus status) {
        require(memberId);
        memberMapper.update(Wrappers.<Member>lambdaUpdate()
                .set(Member::getStatus, status)
                .eq(Member::getId, memberId));
        if (status == AccountStatus.DISABLED) {
            StpKit.MEMBER.logout(memberId);
        }
    }

    public MemberBriefDTO getBrief(Long memberId) {
        return memberConvert.toBriefDTO(require(memberId));
    }

    private Member require(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        BizException.throwIf(member == null, UserErrorCode.MEMBER_NOT_FOUND);
        return member;
    }
}
