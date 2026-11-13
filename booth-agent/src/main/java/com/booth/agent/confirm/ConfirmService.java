package com.booth.agent.confirm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import com.booth.agent.config.BoothAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfirmService {

    private static final String PREFIX = "booth:agent:confirm:";
    private static final String FP_PREFIX = "booth:agent:confirm:fp:";

    private final ConfirmStore store;
    private final ObjectMapper objectMapper;
    private final BoothAgentProperties properties;

    public ConfirmTicket create(String memberToken, String action, String orderSn, Long orderItemId,
                                String reason, String description, String conversationId) {
        String memberHash = Tokens.fingerprint(memberToken);
        String fingerprint = ParamFingerprints.of(action, orderSn, orderItemId, reason, description);
        String indexKey = FP_PREFIX + memberHash + ":" + fingerprint;
        String existingId = store.get(indexKey);
        if (StringUtils.hasText(existingId)) {
            String json = store.get(PREFIX + existingId);
            if (json != null) {
                ConfirmTicket existing = fromJson(json);
                if (memberHash.equals(existing.memberTokenHash())) {
                    return existing;
                }
            }
        }
        String ticketId = UUID.randomUUID().toString().replace("-", "");
        Instant expireAt = Instant.now().plus(properties.agent().confirmTtl());
        ConfirmTicket ticket = new ConfirmTicket(ticketId, memberHash, action, orderSn, orderItemId,
                reason, description, expireAt, conversationId, fingerprint);
        store.save(PREFIX + ticketId, toJson(ticket), properties.agent().confirmTtl());
        store.save(indexKey, ticketId, properties.agent().confirmTtl());
        return ticket;
    }

    public ConfirmTicket require(String memberToken, String ticketId) {
        String json = store.get(PREFIX + ticketId);
        if (json == null) {
            throw new ApiException(400, 40001, "确认单已失效，请重新发起");
        }
        ConfirmTicket ticket = fromJson(json);
        if (!ticket.memberTokenHash().equals(Tokens.fingerprint(memberToken))) {
            throw new ApiException(403, 40301, "确认单与当前登录不一致");
        }
        return ticket;
    }

    /**
     * 先校验是不是本人的票，再原子删除。删除失败说明并发确认里另一边已经用过。
     * 只有删除成功的那一次才会继续提交商城。
     */
    public ConfirmTicket consume(String memberToken, String ticketId) {
        ConfirmTicket ticket = require(memberToken, ticketId);
        if (!store.delete(PREFIX + ticketId)) {
            throw new ApiException(400, 40002, "确认单已使用或已失效");
        }
        if (StringUtils.hasText(ticket.paramFingerprint())) {
            store.delete(FP_PREFIX + ticket.memberTokenHash() + ":" + ticket.paramFingerprint());
        }
        return ticket;
    }

    private String toJson(ConfirmTicket ticket) {
        try {
            return objectMapper.writeValueAsString(ticket);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private ConfirmTicket fromJson(String json) {
        try {
            return objectMapper.readValue(json, ConfirmTicket.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
