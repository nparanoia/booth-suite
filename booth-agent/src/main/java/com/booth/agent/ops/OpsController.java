package com.booth.agent.ops;

import com.booth.agent.audit.ChatTranscriptRepository;
import com.booth.agent.common.ApiException;
import com.booth.agent.common.Result;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.knowledge.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "运营面板")
@RestController
@RequestMapping("/app/ops")
public class OpsController {

    private final OpsMetrics metrics;
    private final OpsAuthService opsAuthService;
    private final ChatTranscriptRepository transcripts;
    private final KnowledgeService knowledgeService;
    private final BoothAgentProperties properties;

    public OpsController(OpsMetrics metrics, OpsAuthService opsAuthService,
                         ChatTranscriptRepository transcripts, KnowledgeService knowledgeService,
                         BoothAgentProperties properties) {
        this.metrics = metrics;
        this.opsAuthService = opsAuthService;
        this.transcripts = transcripts;
        this.knowledgeService = knowledgeService;
        this.properties = properties;
    }

    @Operation(summary = "今日客服摘要", description = "需要管理员 Authorization: Bearer {adminToken}，会向商城网关校验身份。")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        opsAuthService.requireAdmin(authorization);
        Map<String, Object> body = new LinkedHashMap<>(metrics.summary());
        body.put("writeCircuitOpen", properties.agent().governance().writeCircuitOpen());
        body.put("changeAddressEnabled", properties.agent().skills().changeAddressEnabled());
        body.put("humanTransferEnabled", properties.agent().skills().humanTransferEnabled());
        body.put("humanPhone", properties.agent().support().humanPhone());
        body.put("chatRetentionDays", properties.agent().support().chatRetentionDays());
        return Result.ok(body);
    }

    @Operation(summary = "对话取证查询")
    @GetMapping("/chats")
    public Result<List<Map<String, Object>>> chats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String memberHash,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String orderSn,
            @RequestParam(defaultValue = "100") int limit) {
        opsAuthService.requireAdmin(authorization);
        return Result.ok(transcripts.search(memberHash, conversationId, orderSn, limit));
    }

    @Operation(summary = "知识块列表")
    @GetMapping("/knowledge")
    public Result<List<Map<String, Object>>> knowledge(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(defaultValue = "50") int limit) {
        opsAuthService.requireAdmin(authorization);
        return Result.ok(knowledgeService.list(limit));
    }

    @Operation(summary = "新增知识/活动说明（可带时效）")
    @PostMapping("/knowledge")
    public Result<Map<String, Object>> addKnowledge(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody KnowledgeUpsertRequest body) {
        opsAuthService.requireAdmin(authorization);
        if (body == null || !StringUtils.hasText(body.source()) || !StringUtils.hasText(body.heading())
                || !StringUtils.hasText(body.content())) {
            throw new ApiException(400, 40000, "source、heading、content 必填");
        }
        Instant from = parseInstant(body.validFrom());
        Instant to = parseInstant(body.validTo());
        knowledgeService.add(body.source().trim(), body.heading().trim(), body.content().trim(), from, to);
        return Result.ok(Map.of("ok", true));
    }

    @Operation(summary = "按 source 删除知识")
    @DeleteMapping("/knowledge")
    public Result<Map<String, Object>> deleteKnowledge(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam String source) {
        opsAuthService.requireAdmin(authorization);
        if (!StringUtils.hasText(source)) {
            throw new ApiException(400, 40000, "source 必填");
        }
        knowledgeService.removeSource(source.trim());
        return Result.ok(Map.of("ok", true));
    }

    private static Instant parseInstant(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return Instant.parse(raw.trim());
    }

    public record KnowledgeUpsertRequest(
            String source,
            String heading,
            String content,
            String validFrom,
            String validTo
    ) {
    }
}
