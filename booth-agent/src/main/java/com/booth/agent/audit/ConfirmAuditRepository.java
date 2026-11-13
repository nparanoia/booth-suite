package com.booth.agent.audit;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * 确认写操作审计持久化到 PostgreSQL。Redis 里仍保留 7 天热数据，PG 便于对账与面试演示。
 */
@Repository
public class ConfirmAuditRepository {

    private final JdbcTemplate jdbc;

    public ConfirmAuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbc.execute(
                """
                        CREATE TABLE IF NOT EXISTS confirm_audit (
                            ticket_id text PRIMARY KEY,
                            member_hash text NOT NULL,
                            action text NOT NULL,
                            order_sn text,
                            order_item_id bigint,
                            conversation_id text,
                            success boolean NOT NULL,
                            code int NOT NULL,
                            result_ref text,
                            message text,
                            created_at timestamptz NOT NULL DEFAULT now()
                        )
                        """);
        jdbc.execute(
                "CREATE INDEX IF NOT EXISTS confirm_audit_member_idx ON confirm_audit (member_hash, created_at DESC)");
    }

    public void upsert(String ticketId, String memberHash, String action, String orderSn, Long orderItemId,
                       String conversationId, boolean success, int code, String resultRef, String message,
                       Instant at) {
        Instant when = at == null ? Instant.now() : at;
        jdbc.update(
                """
                        INSERT INTO confirm_audit (
                            ticket_id, member_hash, action, order_sn, order_item_id, conversation_id,
                            success, code, result_ref, message, created_at
                        ) VALUES (?,?,?,?,?,?,?,?,?,?,?)
                        ON CONFLICT (ticket_id) DO UPDATE SET
                            member_hash = EXCLUDED.member_hash,
                            action = EXCLUDED.action,
                            order_sn = EXCLUDED.order_sn,
                            order_item_id = EXCLUDED.order_item_id,
                            conversation_id = EXCLUDED.conversation_id,
                            success = CASE
                                WHEN confirm_audit.success THEN confirm_audit.success
                                ELSE EXCLUDED.success
                            END,
                            code = CASE
                                WHEN confirm_audit.success THEN confirm_audit.code
                                ELSE EXCLUDED.code
                            END,
                            result_ref = CASE
                                WHEN confirm_audit.success THEN confirm_audit.result_ref
                                ELSE EXCLUDED.result_ref
                            END,
                            message = CASE
                                WHEN confirm_audit.success THEN confirm_audit.message
                                ELSE EXCLUDED.message
                            END
                        """,
                ticketId, memberHash, action, orderSn, orderItemId, conversationId,
                success, code, resultRef, message, Timestamp.from(when));
    }

    public Optional<Row> findByTicketId(String ticketId) {
        var list = jdbc.query(
                """
                        SELECT ticket_id, member_hash, action, order_sn, order_item_id, conversation_id,
                               success, code, result_ref, message, created_at
                        FROM confirm_audit WHERE ticket_id = ?
                        """,
                (rs, i) -> new Row(
                        rs.getString("ticket_id"),
                        rs.getString("member_hash"),
                        rs.getString("action"),
                        rs.getString("order_sn"),
                        rs.getObject("order_item_id") == null ? null : rs.getLong("order_item_id"),
                        rs.getString("conversation_id"),
                        rs.getBoolean("success"),
                        rs.getInt("code"),
                        rs.getString("result_ref"),
                        rs.getString("message"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                ticketId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public record Row(String ticketId, String memberHash, String action, String orderSn, Long orderItemId,
                      String conversationId, boolean success, int code, String resultRef, String message,
                      Instant createdAt) {
    }
}
