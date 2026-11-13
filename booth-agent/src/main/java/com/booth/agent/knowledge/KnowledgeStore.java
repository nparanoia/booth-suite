package com.booth.agent.knowledge;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class KnowledgeStore {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public KnowledgeStore(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    public void ensureSchema() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_chunk (
                    id bigserial PRIMARY KEY,
                    source text NOT NULL,
                    heading text NOT NULL,
                    content text NOT NULL,
                    embedding vector(256) NOT NULL,
                    valid_from timestamptz,
                    valid_to timestamptz
                )
                """);
        try {
            jdbc.execute("ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS valid_from timestamptz");
            jdbc.execute("ALTER TABLE knowledge_chunk ADD COLUMN IF NOT EXISTS valid_to timestamptz");
        } catch (RuntimeException ignored) {
            // 旧库兼容：部分驱动对 IF NOT EXISTS 支持不一，忽略重复列
        }
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS knowledge_chunk_embedding_hnsw
                    ON knowledge_chunk USING hnsw (embedding vector_cosine_ops)
                """);
    }

    public void replaceAll(List<Row> rows) {
        transaction.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM knowledge_chunk");
            insertAll(rows);
        });
    }

    public void insert(Row row) {
        insertAll(List.of(row));
    }

    public void deleteSource(String source) {
        jdbc.update("DELETE FROM knowledge_chunk WHERE source = ?", source);
    }

    public List<Row> nearest(String vectorLiteral, int limit) {
        return jdbc.query("""
                SELECT source, heading, content,
                       1 - (embedding <=> CAST(? AS vector)) AS cosine,
                       valid_from, valid_to
                FROM knowledge_chunk
                WHERE (valid_from IS NULL OR valid_from <= now())
                  AND (valid_to IS NULL OR valid_to >= now())
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """, (rs, rowNum) -> new Row(
                rs.getString("source"),
                rs.getString("heading"),
                rs.getString("content"),
                rs.getDouble("cosine"),
                null,
                toInstant(rs.getTimestamp("valid_from")),
                toInstant(rs.getTimestamp("valid_to"))
        ), vectorLiteral, vectorLiteral, limit);
    }

    public List<Row> containing(String phrase) {
        String pattern = likePattern(phrase);
        return jdbc.query("""
                SELECT source, heading, content, 0 AS cosine, valid_from, valid_to
                FROM knowledge_chunk
                WHERE (heading LIKE ? ESCAPE '!' OR content LIKE ? ESCAPE '!')
                  AND (valid_from IS NULL OR valid_from <= now())
                  AND (valid_to IS NULL OR valid_to >= now())
                """, (rs, rowNum) -> new Row(
                rs.getString("source"),
                rs.getString("heading"),
                rs.getString("content"),
                0,
                null,
                toInstant(rs.getTimestamp("valid_from")),
                toInstant(rs.getTimestamp("valid_to"))
        ), pattern, pattern);
    }

    public List<Map<String, Object>> listAll(int limit) {
        return jdbc.queryForList("""
                SELECT id, source, heading, left(content, 200) AS content_preview,
                       valid_from, valid_to
                FROM knowledge_chunk
                ORDER BY id DESC
                LIMIT ?
                """, Math.min(Math.max(limit, 1), 200));
    }

    private void insertAll(List<Row> rows) {
        for (Row row : rows) {
            jdbc.update("""
                    INSERT INTO knowledge_chunk (source, heading, content, embedding, valid_from, valid_to)
                    VALUES (?, ?, ?, CAST(? AS vector), ?, ?)
                    """, row.source(), row.heading(), row.content(), row.vectorLiteral(),
                    row.validFrom() == null ? null : Timestamp.from(row.validFrom()),
                    row.validTo() == null ? null : Timestamp.from(row.validTo()));
        }
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String likePattern(String phrase) {
        String escaped = phrase.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    public record Row(String source, String heading, String content, double cosine, String vectorLiteral,
                      Instant validFrom, Instant validTo) {
        public Row(String source, String heading, String content, double cosine) {
            this(source, heading, content, cosine, null, null, null);
        }

        public Row(String source, String heading, String content, double cosine, String vectorLiteral) {
            this(source, heading, content, cosine, vectorLiteral, null, null);
        }
    }
}
