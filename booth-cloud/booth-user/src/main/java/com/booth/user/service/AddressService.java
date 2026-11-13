package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.user.dto.MemberAddressDTO;
import com.booth.common.core.exception.BizException;
import com.booth.user.convert.MemberConvert;
import com.booth.user.dto.AddressSaveRequest;
import com.booth.user.entity.MemberAddress;
import com.booth.user.error.UserErrorCode;
import com.booth.user.mapper.MemberAddressMapper;
import com.booth.user.mapper.MemberMapper;
import com.booth.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 所有读写都带 member_id 条件，别人的地址 ID 查不到也改不了，从数据访问层杜绝越权。
 * 涉及"默认地址"的写操作先锁会员行，保证同一会员同时只有一个默认地址。
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESS_COUNT = 20;

    private final MemberAddressMapper addressMapper;
    private final MemberMapper memberMapper;
    private final MemberConvert memberConvert;

    public List<AddressVO> list(Long memberId) {
        return addressMapper.selectList(Wrappers.<MemberAddress>lambdaQuery()
                        .eq(MemberAddress::getMemberId, memberId)
                        .orderByDesc(MemberAddress::getIsDefault)
                        .orderByDesc(MemberAddress::getId))
                .stream().map(memberConvert::toVO).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long memberId, AddressSaveRequest request) {
        lockMember(memberId);
        long count = addressMapper.selectCount(Wrappers.<MemberAddress>lambdaQuery()
                .eq(MemberAddress::getMemberId, memberId));
        BizException.throwIf(count >= MAX_ADDRESS_COUNT, UserErrorCode.ADDRESS_LIMIT);

        boolean makeDefault = count == 0 || Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearDefault(memberId);
        }
        MemberAddress address = memberConvert.toEntity(request);
        address.setMemberId(memberId);
        address.setIsDefault(makeDefault);
        addressMapper.insert(address);
        return address.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long memberId, Long addressId, AddressSaveRequest request) {
        lockMember(memberId);
        MemberAddress address = require(memberId, addressId);
        memberConvert.update(address, request);
        addressMapper.updateById(address);
        if (Boolean.TRUE.equals(request.isDefault())) {
            setDefault(memberId, addressId);
        }
    }

    /**
     * 删掉的是默认地址时，把最新的一条地址顶上来做默认。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, Long addressId) {
        lockMember(memberId);
        MemberAddress address = require(memberId, addressId);
        addressMapper.deleteById(addressId);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressMapper.selectList(Wrappers.<MemberAddress>lambdaQuery()
                            .eq(MemberAddress::getMemberId, memberId)
                            .orderByDesc(MemberAddress::getId)
                            .last("LIMIT 1"))
                    .stream().findFirst()
                    .ifPresent(next -> markDefault(memberId, next.getId()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long memberId, Long addressId) {
        lockMember(memberId);
        require(memberId, addressId);
        clearDefault(memberId);
        markDefault(memberId, addressId);
    }

    public MemberAddressDTO getForOrder(Long memberId, Long addressId) {
        return memberConvert.toDTO(require(memberId, addressId));
    }

    private MemberAddress require(Long memberId, Long addressId) {
        MemberAddress address = addressMapper.selectOne(Wrappers.<MemberAddress>lambdaQuery()
                .eq(MemberAddress::getId, addressId)
                .eq(MemberAddress::getMemberId, memberId));
        BizException.throwIf(address == null, UserErrorCode.ADDRESS_NOT_FOUND);
        return address;
    }

    private void lockMember(Long memberId) {
        BizException.throwIf(memberMapper.lockById(memberId) == null, UserErrorCode.MEMBER_NOT_FOUND);
    }

    private void clearDefault(Long memberId) {
        addressMapper.update(Wrappers.<MemberAddress>lambdaUpdate()
                .set(MemberAddress::getIsDefault, false)
                .eq(MemberAddress::getMemberId, memberId)
                .eq(MemberAddress::getIsDefault, true));
    }

    private void markDefault(Long memberId, Long addressId) {
        addressMapper.update(Wrappers.<MemberAddress>lambdaUpdate()
                .set(MemberAddress::getIsDefault, true)
                .eq(MemberAddress::getId, addressId)
                .eq(MemberAddress::getMemberId, memberId));
    }
}
