package com.booth.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.product.convert.AttrConvert;
import com.booth.product.dto.AttrGroupSaveRequest;
import com.booth.product.dto.AttrSaveRequest;
import com.booth.product.entity.Attr;
import com.booth.product.entity.AttrGroup;
import com.booth.product.entity.ProductAttrValue;
import com.booth.product.enums.AttrType;
import com.booth.product.error.ProductErrorCode;
import com.booth.product.mapper.AttrGroupMapper;
import com.booth.product.mapper.AttrMapper;
import com.booth.product.mapper.ProductAttrValueMapper;
import com.booth.product.vo.AttrGroupVO;
import com.booth.product.vo.AttrVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttrService {

    private final AttrGroupMapper groupMapper;
    private final AttrMapper attrMapper;
    private final ProductAttrValueMapper attrValueMapper;
    private final AttrConvert attrConvert;

    public List<AttrGroupVO> listGroups() {
        return groupMapper.selectList(Wrappers.<AttrGroup>lambdaQuery().orderByAsc(AttrGroup::getId))
                .stream().map(attrConvert::toGroupVO).toList();
    }

    public Long createGroup(AttrGroupSaveRequest request) {
        AttrGroup group = new AttrGroup();
        group.setName(request.name());
        groupMapper.insert(group);
        return group.getId();
    }

    public void updateGroup(Long id, AttrGroupSaveRequest request) {
        AttrGroup group = requireGroup(id);
        group.setName(request.name());
        groupMapper.updateById(group);
    }

    public void deleteGroup(Long id) {
        requireGroup(id);
        boolean hasAttrs = attrMapper.exists(Wrappers.<Attr>lambdaQuery().eq(Attr::getGroupId, id));
        BizException.throwIf(hasAttrs, ProductErrorCode.ATTR_GROUP_IN_USE);
        groupMapper.deleteById(id);
    }

    public List<AttrVO> listAttrs(Long groupId, AttrType type) {
        return attrMapper.selectList(Wrappers.<Attr>lambdaQuery()
                        .eq(Attr::getGroupId, groupId)
                        .eq(type != null, Attr::getType, type)
                        .orderByDesc(Attr::getSort)
                        .orderByAsc(Attr::getId))
                .stream().map(attrConvert::toVO).toList();
    }

    public Long createAttr(AttrSaveRequest request) {
        BizException.throwIf(request.groupId() == null, ProductErrorCode.ATTR_GROUP_NOT_FOUND);
        requireGroup(request.groupId());
        Attr attr = attrConvert.toEntity(request);
        attrMapper.insert(attr);
        return attr.getId();
    }

    public void updateAttr(Long id, AttrSaveRequest request) {
        Attr attr = requireAttr(id);
        attrConvert.update(attr, request);
        attrMapper.updateById(attr);
    }

    public void deleteAttr(Long id) {
        requireAttr(id);
        boolean inUse = attrValueMapper.exists(Wrappers.<ProductAttrValue>lambdaQuery().eq(ProductAttrValue::getAttrId, id));
        BizException.throwIf(inUse, ProductErrorCode.ATTR_IN_USE);
        attrMapper.deleteById(id);
    }

    /**
     * 商品参数只能引用所选分组下的"展示参数"，防止把别的类型或别的分组的属性挂到商品上。
     */
    void checkParams(Long groupId, Collection<Long> attrIds) {
        if (attrIds.isEmpty()) {
            return;
        }
        BizException.throwIf(groupId == null, ProductErrorCode.PRODUCT_PARAM_INVALID);
        long valid = attrMapper.selectCount(Wrappers.<Attr>lambdaQuery()
                .eq(Attr::getGroupId, groupId)
                .eq(Attr::getType, AttrType.PARAM)
                .in(Attr::getId, attrIds));
        BizException.throwIf(valid != attrIds.size(), ProductErrorCode.PRODUCT_PARAM_INVALID);
    }

    Map<Long, Attr> mapByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return attrMapper.selectByIds(ids).stream().collect(Collectors.toMap(Attr::getId, Function.identity()));
    }

    private AttrGroup requireGroup(Long id) {
        AttrGroup group = groupMapper.selectById(id);
        BizException.throwIf(group == null, ProductErrorCode.ATTR_GROUP_NOT_FOUND);
        return group;
    }

    private Attr requireAttr(Long id) {
        Attr attr = attrMapper.selectById(id);
        BizException.throwIf(attr == null, ProductErrorCode.ATTR_NOT_FOUND);
        return attr;
    }
}
