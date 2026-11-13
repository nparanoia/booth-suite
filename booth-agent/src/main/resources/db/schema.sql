CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id bigserial PRIMARY KEY,
    source text NOT NULL,
    heading text NOT NULL,
    content text NOT NULL,
    embedding vector(256) NOT NULL,
    valid_from timestamptz,
    valid_to timestamptz
);

CREATE INDEX IF NOT EXISTS knowledge_chunk_embedding_hnsw
    ON knowledge_chunk USING hnsw (embedding vector_cosine_ops);

-- 写操作本地事务表：确认后先落库再调商城，失败可对账重试
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
);

CREATE INDEX IF NOT EXISTS write_action_outbox_status_idx
    ON write_action_outbox (status, updated_at);

CREATE INDEX IF NOT EXISTS write_action_outbox_fp_idx
    ON write_action_outbox (member_hash, param_fingerprint);

-- 确认写操作审计（成功记录不会被后来的失败覆盖）
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
);

CREATE INDEX IF NOT EXISTS confirm_audit_member_idx
    ON confirm_audit (member_hash, created_at DESC);

-- 对话取证（投诉/纠纷可按会话或单号查阅）
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
);

CREATE INDEX IF NOT EXISTS chat_transcript_conv_idx
    ON chat_transcript (conversation_id, created_at);

CREATE INDEX IF NOT EXISTS chat_transcript_member_idx
    ON chat_transcript (member_hash, created_at DESC);

CREATE INDEX IF NOT EXISTS chat_transcript_order_idx
    ON chat_transcript (order_sn, created_at DESC);
