package com.booth.search.dto;

import com.booth.search.enums.SearchSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductSearchQuery {

    private String keyword;
    private Long categoryId;
    private Long brandId;
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    /** 形式为「属性名:属性值」，可以重复传 */
    private List<String> attrs;
    private SearchSort sort = SearchSort.DEFAULT;

    @Min(value = 1, message = "页码最小为 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private long pageSize = 10;
}
