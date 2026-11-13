package com.booth.agent.write;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class WriteActionRepository {

    private static final RowMapper<WriteActionRecord> ROW = (rs, i) -> new WriteActionRecord(
            rs.getLong("id"),
            rs.getString("ticket_id"),
            rs.getString("member_hash"),
            rs.getString("action"),
            rs.getString("param_fingerprint"),
            rs.getString("order_sn"),
            rs.getObject("order_item_id") == null ? null : rs.getLong("order_item_id"),
            rs.getString("conversation_id"),
            WriteActionStatus.valueOf(rs.getString("status")),
            rs.getInt("attempts"),
            rs.getInt("max_attempts"),
            rs.getString("last_error"),
            rs.getString("result_ref"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    private final JdbcTemplate jdbc;

    public WriteActionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbc.execute(
                """
                        CREATE TABLE IF NOT EXISTS write_action_outbox (
                            id bigserial PRIMARY KEY,
                            ticket_id text NOT NULL UNIQUE,
                            member_hash text NOT NULL,
                            action text NOT NULL,
                            param_fingerprint text NOT NULL,
                            order_sn text NOT NULL,
                            order_item_id bigint,
                            conversation_id text,
                            status text NOT NULL,
                            attempts int NOT NULL DEFAULT 0,
                            max_attempts int NOT NULL DEFAULT 3,
                            last_error text,
                            result_ref text,
                            created_at timestamptz NOT NULL DEFAULT now(),
                            updated_at timestamptz NOT NULL DEFAULT now()
                        )
                        """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS write_action_outbox_status_idx ON write_action_outbox (status, updated_at)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS write_action_outbox_fp_idx ON write_action_outbox (member_hash, param_fingerprint)");
    }

    public Optional<WriteActionRecord> findByTicketId(String ticketId) {
        List<WriteActionRecord> list = jdbc.query(
                "SELECT * FROM write_action_outbox WHERE ticket_id = ?", ROW, ticketId);
        return list.stream().findFirst();
    }

    public Optional<WriteActionRecord> findSuccessByFingerprint(String memberHash, String fingerprint) {
        List<WriteActionRecord> list = jdbc.query(
                """
                        SELECT * FROM write_action_outbox
                        WHERE member_hash = ? AND param_fingerprint = ? AND status = 'SUCCESS'
                        ORDER BY id DESC LIMIT 1
                        """,
                ROW, memberHash, fingerprint);
        return list.stream().findFirst();
    }

    public WriteActionRecord insertPending(String ticketId, String memberHash, String action, String fingerprint,
                                           String orderSn, Long orderItemId, String conversationId, int maxAttempts) {
        Instant now = Instant.now();
        jdbc.update(
                """
                        INSERT INTO write_action_outbox
                        (ticket_id, member_hash, action, param_fingerprint, order_sn, order_item_id,
                         conversation_id, status, attempts, max_attempts, created_at, updated_at)
                        VALUES (?,?,?,?,?,?,?,?,0,?,?,?)
                        """,
                ticketId, memberHash, action, fingerprint, orderSn, orderItemId, conversationId,
                WriteActionStatus.PENDING.name(), maxAttempts, Timestamp.from(now), Timestamp.from(now));
        return findByTicketId(ticketId).orElseThrow(() -> new IllegalStateException("outbox insert missing: " + ticketId));
    }

    public void markSuccess(long id, String resultRef) {
        jdbc.update(
                """
                        UPDATE write_action_outbox
                        SET status = 'SUCCESS', result_ref = ?, last_error = NULL,
                            attempts = attempts + 1, updated_at = now()
                        WHERE id = ?
                        """,
                resultRef, id);
    }

    public void markFailed(long id, String error, boolean needReconcile) {
        WriteActionStatus status = needReconcile ? WriteActionStatus.NEED_RECONCILE : WriteActionStatus.FAILED;
        jdbc.update(
                """
                        UPDATE write_action_outbox
                        SET status = ?, last_error = ?, attempts = attempts + 1, updated_at = now()
                        WHERE id = ?
                        """,
                status.name(), error, id);
    }

    public List<WriteActionRecord> findRetryable(int limit) {
        return jdbc.query(
                """
                        SELECT * FROM write_action_outbox
                        WHERE status IN ('FAILED', 'NEED_RECONCILE', 'PENDING')
                          AND attempts < max_attempts
                        ORDER BY updated_at ASC
                        LIMIT ?
                        """,
                ROW, limit);
    }
}
