package com.booth.search.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Canal 扁平消息。一行变更只拿主键，商品内容以数据库当前值为准。
 */
public final class CanalMessageParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CanalMessageParser() {
    }

    public static List<CanalChange> parse(String body) {
        JsonNode root;
        try {
            root = MAPPER.readTree(body);
        } catch (Exception e) {
            throw new IllegalArgumentException("商品变更消息不是 JSON", e);
        }
        List<CanalChange> changes = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(node -> add(changes, node));
        } else {
            add(changes, root);
        }
        return changes;
    }

    private static void add(List<CanalChange> changes, JsonNode node) {
        if (node.path("isDdl").asBoolean(false)) {
            return;
        }
        String table = node.path("table").asText("");
        JsonNode rows = node.path("data");
        if (!rows.isArray() || rows.isEmpty()) {
            return;
        }
        List<Long> productIds = new ArrayList<>();
        List<Long> brandIds = new ArrayList<>();
        List<Long> categoryIds = new ArrayList<>();
        for (JsonNode row : rows) {
            switch (table) {
                case "product" -> productIds.add(longValue(row, "id"));
                case "sku", "product_attr_value" -> productIds.add(longValue(row, "product_id"));
                case "brand" -> brandIds.add(longValue(row, "id"));
                case "category" -> categoryIds.add(longValue(row, "id"));
                default -> {
                    return;
                }
            }
        }
        changes.add(new CanalChange(table, List.copyOf(productIds), List.copyOf(brandIds), List.copyOf(categoryIds)));
    }

    private static long longValue(JsonNode row, String field) {
        JsonNode value = row.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("商品变更消息缺少 " + field);
        }
        return Long.parseLong(value.asText());
    }

    public record CanalChange(String table, List<Long> productIds, List<Long> brandIds, List<Long> categoryIds) {
    }
}
