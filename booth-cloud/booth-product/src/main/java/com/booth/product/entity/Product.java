package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.product.enums.PublishStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("product")
public class Product extends BaseEntity {

    private Long categoryId;
    private Long brandId;
    private Long attrGroupId;
    private String productSn;
    private String name;
    private String subTitle;
    private String pic;
    /** 逗号分隔的图片地址 */
    private String albumPics;
    private String keywords;
    private String detailHtml;
    /** 展示价，始终等于 SKU 最低价，由 SKU 变更时重新计算 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String unit;
    private BigDecimal weight;
    /** 下单并发累加，不参与 updateById */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer sale;
    private Integer sort;
    /** 只能通过上下架接口修改 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private PublishStatus publishStatus;
    private Boolean newStatus;
    private Boolean recommendStatus;
    private Integer giftIntegration;
    private String brandName;
    private String categoryName;
    @TableLogic
    private Integer deleted;
}
