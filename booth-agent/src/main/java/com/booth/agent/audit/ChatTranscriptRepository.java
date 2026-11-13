package com.booth.agent.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 对话取证：用户/助手原文落 PG，供店主按会话或会员查阅。 */
@Repository
public class ChatTranscriptRepository {

    private final JdbcTemplate jdbc;

    public ChatTranscriptRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureSchema() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS chat_transcript (
                    id bigserial PRIMARY KEY,
                    conversation_id text NOT NULL,
                    member_hash text NOT NULL,
                    role text NOT NULL,
                    content text NOT NULL,
                    intent text,
                    order_sn text,
                    tools text,
                    created_at timestamptz NOT NULL DEFAULT now()
                )
                """);
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS chat_transcript_conv_idx
                    ON chat_transcript (conversation_id, created_at)
                """);
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS chat_transcript_member_idx
                    ON chat_transcript (member_hash, created_at DESC)
                """);
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS chat_transcript_order_idx
                    ON chat_transcript (order_sn, created_at DESC)
                """);
    }

    public void append(String conversationId, String memberHash, String role, String content,
                       String intent, String orderSn, String tools, Instant at) {
        jdbc.update("""
                INSERT INTO chat_transcript
                    (conversation_id, member_hash, role, content, intent, order_sn, tools, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, conversationId, memberHash, role, clip(content), intent, orderSn, tools,
                Timestamp.from(at == null ? Instant.now() : at));
    }

    public List<Map<String, Object>> search(String memberHash, String conversationId, String orderSn, int limit) {
        int cap = Math.min(Math.max(limit, 1), 200);
        if (conversationId != null && !conversationId.isBlank()) {
            return jdbc.queryForList("""
                    SELECT conversation_id, member_hash, role, content, intent, order_sn, tools, created_at
                    FROM chat_transcript
                    WHERE conversation_id = ?
                    ORDER BY created_at ASC
                    LIMIT ?
                    """, conversationId.trim(), cap);
        }
        if (orderSn != null && !orderSn.isBlank()) {
            return jdbc.queryForList("""
                    SELECT conversation_id, member_hash, role, content, intent, order_sn, tools, created_at
                    FROM chat_transcript
                    WHERE order_sn = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """, orderSn.trim(), cap);
        }
        if (memberHash != null && !memberHash.isBlank()) {
            return jdbc.queryForList("""
                    SELECT conversation_id, member_hash, role, content, intent, order_sn, tools, created_at
                    FROM chat_transcript
                    WHERE member_hash = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                    """, memberHash.trim(), cap);
        }
        return jdbc.queryForList("""
                SELECT conversation_id, member_hash, role, content, intent, order_sn, tools, created_at
                FROM chat_transcript
                ORDER BY created_at DESC
                LIMIT ?
                """, cap);
    }

    public int purgeOlderThanDays(int days) {
        if (days < 1) {
            return 0;
        }
        return jdbc.update("DELETE FROM chat_transcript WHERE created_at < now() - (? || ' days')::interval", days);
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 2000 ? trimmed : trimmed.substring(0, 2000);
    }
}
