package com.booth.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.booth.product.entity.Product;
import com.booth.product.entity.Sku;
import com.booth.product.enums.PublishStatus;
import com.booth.product.mapper.ProductMapper;
import com.booth.product.mapper.SkuMapper;
import com.booth.product.vo.CategoryTreeVO;
import com.booth.product.vo.ProductDetailVO;
import com.booth.product.vo.SkuVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 前台商品详情和分类树的两级缓存。
 * 内存缓存 30 秒，Redis 10 分钟再加最多 2 分钟的随机时间。
 * 不存在的商品缓存空值 60 秒。同一个 key 同时失效时只有一个请求回源。
 * 可售库存和销量每次现查，不放进上面的长缓存。
 */
@Slf4j
@Service
public class ProductViewCache {

    public static final String CHANNEL = "booth:product:cache";
    private static final String DETAIL_PREFIX = "booth:product:detail:";
    private static final String TREE_KEY = "booth:product:category-tree";
    private static final String NIL = "nil";
    private static final Duration NIL_TTL = Duration.ofSeconds(60);
    private static final Duration LOCAL_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final Cache<Long, Optional<ProductDetailVO>> details = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(LOCAL_TTL)
            .build();
    private final Cache<String, List<CategoryTreeVO>> trees = Caffeine.newBuilder()
            .maximumSize(4)
            .expireAfterWrite(LOCAL_TTL)
            .build();

    public ProductViewCache(StringRedisTemplate redis, ObjectMapper objectMapper,
                            ProductMapper productMapper, SkuMapper skuMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
    }

    public ProductDetailVO detail(Long id, Supplier<ProductDetailVO> loader) {
        Optional<ProductDetailVO> cached = details.getIfPresent(id);
        if (cached == null) {
            cached = readDetail(id);
            if (cached != null) {
                details.put(id, cached);
            }
        }
        if (cached == null) {
            cached = loadDetail(id, loader);
        }
        return cached.isEmpty() ? null : overlay(cached.get());
    }

    public List<CategoryTreeVO> categoryTree(Supplier<List<CategoryTreeVO>> loader) {
        List<CategoryTreeVO> local = trees.getIfPresent(TREE_KEY);
        if (local != null) {
            return local;
        }
        List<CategoryTreeVO> cached = readTree();
        if (cached != null) {
            trees.put(TREE_KEY, cached);
            return cached;
        }
        return loadTree(loader);
    }

    public void evictProduct(Long id) {
        details.invalidate(id);
        try {
            redis.delete(DETAIL_PREFIX + id);
            redis.convertAndSend(CHANNEL, "p:" + id);
        } catch (RuntimeException e) {
            log.warn("删除商品详情缓存失败，id={}", id, e);
        }
    }

    public void evictCategoryTree() {
        trees.invalidateAll();
        try {
            redis.delete(TREE_KEY);
            redis.convertAndSend(CHANNEL, "c");
        } catch (RuntimeException e) {
            log.warn("删除分类树缓存失败", e);
        }
    }

    /**
     * 其他实例收到广播后只清自己的内存缓存。Redis 已经由发出广播的实例删过，这里再删会把刚写回去的新值清掉。
     */
    public void onRemote(String body) {
        if (body == null) {
            return;
        }
        if (body.startsWith("p:")) {
            details.invalidate(Long.valueOf(body.substring(2)));
        } else if ("c".equals(body)) {
            trees.invalidateAll();
        }
    }

    private Optional<ProductDetailVO> loadDetail(Long id, Supplier<ProductDetailVO> loader) {
        String lockKey = "booth:product:lock:detail:" + id;
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5)));
        if (!locked) {
            Optional<ProductDetailVO> waited = waitDetail(id);
            if (waited != null) {
                return waited;
            }
        }
        try {
            log.info("商品详情回源，id={}", id);
            ProductDetailVO loaded = loader.get();
            writeDetail(id, loaded);
            return loaded == null ? Optional.empty() : Optional.of(loaded);
        } finally {
            if (locked) {
                redis.delete(lockKey);
            }
        }
    }

    private List<CategoryTreeVO> loadTree(Supplier<List<CategoryTreeVO>> loader) {
        String lockKey = "booth:product:lock:tree";
        boolean locked = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5)));
        if (!locked) {
            List<CategoryTreeVO> waited = waitTree();
            if (waited != null) {
                return waited;
            }
        }
        try {
            List<CategoryTreeVO> loaded = loader.get();
            writeTree(loaded);
            return loaded;
        } finally {
            if (locked) {
                redis.delete(lockKey);
            }
        }
    }

    private Optional<ProductDetailVO> waitDetail(Long id) {
        for (int i = 0; i < 20; i++) {
            pause();
            Optional<ProductDetailVO> local = details.getIfPresent(id);
            if (local != null) {
                return local;
            }
            Optional<ProductDetailVO> cached = readDetail(id);
            if (cached != null) {
                details.put(id, cached);
                return cached;
            }
        }
        return null;
    }

    private List<CategoryTreeVO> waitTree() {
        for (int i = 0; i < 20; i++) {
            pause();
            List<CategoryTreeVO> local = trees.getIfPresent(TREE_KEY);
            if (local != null) {
                return local;
            }
            List<CategoryTreeVO> cached = readTree();
            if (cached != null) {
                trees.put(TREE_KEY, cached);
                return cached;
            }
        }
        return null;
    }

    private void writeDetail(Long id, ProductDetailVO loaded) {
        details.put(id, loaded == null ? Optional.empty() : Optional.of(loaded));
        Duration ttl = loaded == null ? NIL_TTL : Duration.ofMinutes(10).plusSeconds(ThreadLocalRandom.current().nextInt(121));
        redis.opsForValue().set(DETAIL_PREFIX + id, loaded == null ? NIL : toJson(loaded), ttl);
    }

    private void writeTree(List<CategoryTreeVO> loaded) {
        trees.put(TREE_KEY, loaded);
        redis.opsForValue().set(TREE_KEY, toJson(loaded),
                Duration.ofMinutes(10).plusSeconds(ThreadLocalRandom.current().nextInt(121)));
    }

    /**
     * 缺失返回 null，缓存的空值返回 Optional.empty()，两者不能混。
     */
    private Optional<ProductDetailVO> readDetail(Long id) {
        String json = redis.opsForValue().get(DETAIL_PREFIX + id);
        if (json == null) {
            return null;
        }
        if (NIL.equals(json)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ProductDetailVO.class));
        } catch (JsonProcessingException e) {
            redis.delete(DETAIL_PREFIX + id);
            return null;
        }
    }

    private List<CategoryTreeVO> readTree() {
        String json = redis.opsForValue().get(TREE_KEY);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CategoryTreeVO>>() {
            });
        } catch (JsonProcessingException e) {
            redis.delete(TREE_KEY);
            return null;
        }
    }

    /**
     * 详情正文走缓存，库存和销量看当前数据库。商品已经下架时立刻当作不存在，并清掉缓存。
     */
    private ProductDetailVO overlay(ProductDetailVO cached) {
        Product product = productMapper.selectOne(Wrappers.<Product>lambdaQuery()
                .select(Product::getId, Product::getSale, Product::getPublishStatus)
                .eq(Product::getId, cached.id()));
        if (product == null || product.getPublishStatus() != PublishStatus.ON_SHELF) {
            evictProduct(cached.id());
            return null;
        }
        Map<Long, Sku> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                        .select(Sku::getId, Sku::getStock, Sku::getLockStock, Sku::getSale)
                        .eq(Sku::getProductId, cached.id()))
                .stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        List<SkuVO> freshSkus = cached.skus() == null ? List.of() : cached.skus().stream().map(vo -> {
            Sku sku = skus.get(vo.id());
            if (sku == null || sku.getStock() == null || sku.getLockStock() == null) {
                return vo.withStock(0, vo.sale() == null ? 0 : vo.sale());
            }
            return vo.withStock(sku.availableStock(), sku.getSale() == null ? 0 : sku.getSale());
        }).toList();
        return cached.fresh(product.getSale(), freshSkus);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }

    private static void pause() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
