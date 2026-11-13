package com.booth.agent.ops;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.confirm.ConfirmStore;
import com.booth.agent.trace.TurnTrace;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 按自然日汇总轨迹计数，供运营面板。存在 ConfirmStore（测试内存 / 运行时 Redis）。
 */
@Service
public class OpsMetrics {

    private static final String KEY_PREFIX = "booth:agent:ops:";
    private static final Duration TTL = Duration.ofDays(14);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ConfirmStore store;
    private final ObjectMapper objectMapper;

    public OpsMetrics(ConfirmStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public synchronized void observe(TurnTrace trace) {
        if (trace == null) {
            return;
        }
        String day = LocalDate.now(ZONE).toString();
        DayBucket bucket = load(day);
        // recordConfirm 的合成轨迹用 apply_return/cancel_order；对话内工具名是 prepare_*，仍计 chats
        boolean syntheticWrite = isSyntheticWriteTrace(trace);
        if (!syntheticWrite) {
            bucket.chats = bucket.chats + 1;
        }
        if ("POLICY".equals(trace.intent())) {
            if (trace.retrievalHit()) {
                bucket.policyHits = bucket.policyHits + 1;
            } else {
                bucket.policyMisses = bucket.policyMisses + 1;
            }
        }
        if (trace.tools() != null) {
            for (String tool : trace.tools()) {
                if (tool == null || tool.isBlank()) {
                    continue;
                }
                bucket.tools.merge(tool, 1, Integer::sum);
                if (isReturnTool(tool)) {
                    if (trace.errorCode() == null) {
                        bucket.writeReturns = bucket.writeReturns + 1;
                    } else {
                        bucket.writeFails = bucket.writeFails + 1;
                    }
                }
                if (isCancelTool(tool)) {
                    if (trace.errorCode() == null) {
                        bucket.writeCancels = bucket.writeCancels + 1;
                    } else {
                        bucket.writeFails = bucket.writeFails + 1;
                    }
                }
            }
        }
        if (trace.errorCode() != null && !syntheticWrite) {
            bucket.toolErrors = bucket.toolErrors + 1;
        }
        save(day, bucket);
    }

    public DayBucket today() {
        return load(LocalDate.now(ZONE).toString());
    }

    public Map<String, Object> summary() {
        DayBucket bucket = today();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("day", bucket.day);
        map.put("chats", bucket.chats);
        map.put("policyHits", bucket.policyHits);
        map.put("policyMisses", bucket.policyMisses);
        map.put("writeReturns", bucket.writeReturns);
        map.put("writeCancels", bucket.writeCancels);
        map.put("writeFails", bucket.writeFails);
        map.put("toolErrors", bucket.toolErrors);
        map.put("tools", Map.copyOf(bucket.tools));
        return map;
    }

    private DayBucket load(String day) {
        String json = store.get(KEY_PREFIX + day);
        if (json == null) {
            return DayBucket.empty(day);
        }
        try {
            DayBucket bucket = objectMapper.readValue(json, DayBucket.class);
            if (bucket == null) {
                return DayBucket.empty(day);
            }
            if (bucket.tools == null) {
                bucket.tools = new LinkedHashMap<>();
            }
            bucket.day = day;
            return bucket;
        } catch (JsonProcessingException exception) {
            return DayBucket.empty(day);
        }
    }

    private void save(String day, DayBucket bucket) {
        try {
            store.save(KEY_PREFIX + day, objectMapper.writeValueAsString(bucket), TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean isSyntheticWriteTrace(TurnTrace trace) {
        if (trace.tools() == null) {
            return false;
        }
        return trace.tools().contains("apply_return")
                || trace.tools().contains("cancel_order")
                || trace.tools().contains("change_address");
    }

    /** 仅统计 recordConfirm 合成名，避免与对话里的 prepare_* 重复加计。 */
    private static boolean isReturnTool(String tool) {
        return "apply_return".equals(tool);
    }

    private static boolean isCancelTool(String tool) {
        return "cancel_order".equals(tool);
    }

    public static final class DayBucket {
        public String day;
        public int chats;
        public int policyHits;
        public int policyMisses;
        @JsonAlias("confirmReturns")
        public int writeReturns;
        @JsonAlias("confirmCancels")
        public int writeCancels;
        @JsonAlias("confirmFails")
        public int writeFails;
        public int toolErrors;
        public Map<String, Integer> tools = new LinkedHashMap<>();

        static DayBucket empty(String day) {
            DayBucket bucket = new DayBucket();
            bucket.day = day;
            return bucket;
        }
    }
}
