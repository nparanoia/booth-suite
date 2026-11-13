package com.booth.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.api.product.dto.StockItemDTO;
import com.booth.api.product.dto.StockLockRequest;
import com.booth.api.product.dto.StockReleaseRequest;
import com.booth.common.core.exception.BizException;
import com.booth.product.entity.Product;
import com.booth.product.entity.Sku;
import com.booth.product.entity.StockLockRecord;
import com.booth.product.enums.PublishStatus;
import com.booth.product.enums.StockLockStatus;
import com.booth.product.error.ProductErrorCode;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.mapper.SkuMapper;
import com.booth.product.mapper.StockLockRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存三阶段：锁定（下单）、扣减（支付）、释放（取消）。幂等键是 orderSn + skuId。
 * 同一订单的 SKU 按 id 升序加锁，避免两个订单交叉锁定时死锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockInnerService {

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final StockLockRecordMapper lockRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public void lock(StockLockRequest request) {
        Map<Long, Integer> wanted = merge(request.items());
        List<StockLockRecord> existing = listByOrder(request.orderSn());
        if (!existing.isEmpty()) {
            if (existing.stream().anyMatch(record -> record.getStatus() == StockLockStatus.RELEASED)) {
                throw new BizException(ProductErrorCode.STOCK_LOCK_CLOSED);
            }
            Map<Long, Integer> locked = existing.stream()
                    .filter(record -> record.getStatus() == StockLockStatus.LOCKED)
                    .collect(Collectors.toMap(StockLockRecord::getSkuId, StockLockRecord::getQuantity));
            if (locked.equals(wanted) && locked.size() == existing.size()) {
                return;
            }
            throw new BizException(ProductErrorCode.STOCK_LOCK_CONFLICT);
        }

        Map<Long, Sku> skus = skuMapper.selectByIds(wanted.keySet()).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Map<Long, Product> products = skus.isEmpty() ? Map.of()
                : productMapper.selectByIds(skus.values().stream().map(Sku::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Sku sku = skus.get(entry.getKey());
            Product product = sku == null ? null : products.get(sku.getProductId());
            BizException.throwIf(sku == null, ProductErrorCode.SKU_NOT_FOUND);
            BizException.throwIf(product == null || product.getPublishStatus() != PublishStatus.ON_SHELF,
                    ProductErrorCode.PRODUCT_NOT_FOUND);
            insert(request.orderSn(), entry.getKey(), entry.getValue(), StockLockStatus.LOCKED);
            int updated = skuMapper.update(null, Wrappers.<Sku>lambdaUpdate()
                    .setSql("lock_stock = lock_stock + " + entry.getValue())
                    .eq(Sku::getId, entry.getKey())
                    .apply("stock - lock_stock >= {0}", entry.getValue()));
            if (updated == 0) {
                throw new BizException(ProductErrorCode.STOCK_NOT_ENOUGH, "库存不足，skuId=" + entry.getKey());
            }
        }
    }

    /**
     * 只有本次从 LOCKED 抢到 DEDUCTED 的记录才扣实物库存，重复调用不会重复扣。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String orderSn) {
        List<StockLockRecord> records = listByOrder(orderSn);
        if (records.isEmpty()) {
            log.warn("扣减库存时没有锁定记录，orderSn={}", orderSn);
            return;
        }
        for (StockLockRecord record : records) {
            if (record.getStatus() != StockLockStatus.LOCKED) {
                continue;
            }
            int changed = cas(record.getId(), StockLockStatus.LOCKED, StockLockStatus.DEDUCTED);
            if (changed == 0) {
                continue;
            }
            int quantity = record.getQuantity();
            int updated = skuMapper.update(null, Wrappers.<Sku>lambdaUpdate()
                    .setSql("stock = stock - " + quantity + ", lock_stock = lock_stock - " + quantity + ", sale = sale + " + quantity)
                    .eq(Sku::getId, record.getSkuId())
                    .ge(Sku::getLockStock, quantity));
            BizException.throwIf(updated == 0, ProductErrorCode.STOCK_LOCK_CONFLICT);
            Sku sku = skuMapper.selectById(record.getSkuId());
            productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                    .setSql("sale = sale + " + quantity)
                    .eq(Product::getId, sku.getProductId()));
        }
    }

    /**
     * 没有锁定记录时，按请求写入 RELEASED 占位。晚到的 lock 会撞上唯一键，从而拒绝悬挂的锁定。
     */
    @Transactional(rollbackFor = Exception.class)
    public void release(StockReleaseRequest request) {
        List<StockLockRecord> records = listByOrder(request.orderSn());
        if (records.isEmpty()) {
            Map<Long, Integer> wanted = merge(request.items());
            for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
                insert(request.orderSn(), entry.getKey(), entry.getValue(), StockLockStatus.RELEASED);
            }
            return;
        }
        for (StockLockRecord record : records) {
            if (record.getStatus() != StockLockStatus.LOCKED) {
                continue;
            }
            int changed = cas(record.getId(), StockLockStatus.LOCKED, StockLockStatus.RELEASED);
            if (changed == 0) {
                continue;
            }
            int quantity = record.getQuantity();
            int updated = skuMapper.update(null, Wrappers.<Sku>lambdaUpdate()
                    .setSql("lock_stock = lock_stock - " + quantity)
                    .eq(Sku::getId, record.getSkuId())
                    .ge(Sku::getLockStock, quantity));
            BizException.throwIf(updated == 0, ProductErrorCode.STOCK_LOCK_CONFLICT);
        }
    }

    private void insert(String orderSn, Long skuId, int quantity, StockLockStatus status) {
        StockLockRecord record = new StockLockRecord();
        record.setOrderSn(orderSn);
        record.setSkuId(skuId);
        record.setQuantity(quantity);
        record.setStatus(status);
        lockRecordMapper.insert(record);
    }

    private int cas(Long id, StockLockStatus from, StockLockStatus to) {
        return lockRecordMapper.update(null, Wrappers.<StockLockRecord>lambdaUpdate()
                .set(StockLockRecord::getStatus, to)
                .eq(StockLockRecord::getId, id)
                .eq(StockLockRecord::getStatus, from));
    }

    private List<StockLockRecord> listByOrder(String orderSn) {
        return lockRecordMapper.selectList(Wrappers.<StockLockRecord>lambdaQuery()
                .eq(StockLockRecord::getOrderSn, orderSn)
                .orderByAsc(StockLockRecord::getSkuId));
    }

    private static Map<Long, Integer> merge(List<StockItemDTO> items) {
        Map<Long, Integer> merged = new TreeMap<>();
        if (items == null) {
            return merged;
        }
        for (StockItemDTO item : items) {
            merged.merge(item.skuId(), item.quantity(), Integer::sum);
        }
        return merged;
    }
}
