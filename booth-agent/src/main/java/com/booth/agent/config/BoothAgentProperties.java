package com.booth.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "booth")
public record BoothAgentProperties(
        String gatewayBaseUrl,
        Agent agent
) {
    public boolean modelEnabled() {
        if (agent == null || agent.llm() == null || !agent.llm().enabled()) {
            return false;
        }
        String apiKey = agent.llm().apiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean embeddingModelEnabled() {
        if (agent == null || agent.llm() == null) {
            return false;
        }
        String apiKey = agent.llm().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        Agent.Embedding embedding = agent.embedding();
        if (embedding == null || embedding.provider() == null || embedding.provider().isBlank()) {
            return true;
        }
        String provider = embedding.provider().trim().toLowerCase();
        return "auto".equals(provider) || "openai".equals(provider);
    }

    public record Agent(
            Duration confirmTtl,
            int knowledgeMinScore,
            double vectorMinScore,
            int maxToolSteps,
            int timeoutRetries,
            Llm llm,
            Embedding embedding,
            Governance governance,
            Support support,
            Skills skills
    ) {
        public Agent {
            if (knowledgeMinScore < 1) {
                throw new IllegalArgumentException("knowledgeMinScore 至少为 1");
            }
            if (vectorMinScore <= 0 || vectorMinScore >= 1) {
                throw new IllegalArgumentException("vectorMinScore 必须在 0 和 1 之间");
            }
            if (maxToolSteps < 1 || maxToolSteps > 8) {
                throw new IllegalArgumentException("maxToolSteps 必须在 1 到 8 之间");
            }
            if (timeoutRetries < 0 || timeoutRetries > 2) {
                throw new IllegalArgumentException("timeoutRetries 必须在 0 到 2 之间");
            }
            if (llm == null) {
                throw new IllegalArgumentException("llm 配置不能为空");
            }
            if (embedding == null) {
                embedding = new Embedding("auto", "text-embedding-3-small", 256);
            }
            if (embedding.dimensions() != 256) {
                throw new IllegalArgumentException("embedding.dimensions 必须是 256，和知识库表一致");
            }
            if (governance == null) {
                governance = Governance.defaults();
            }
            if (support == null) {
                support = Support.defaults();
            }
            if (skills == null) {
                skills = Skills.defaults();
            }
        }

        public record Llm(boolean enabled, String baseUrl, String apiKey, String model) {
        }

        public record Embedding(String provider, String model, int dimensions) {
        }

        public record Governance(
                int maxConcurrentPerUser,
                int maxRequestsPerMinute,
                Duration toolTimeout,
                int requestDedupSeconds,
                boolean writeCircuitOpen
        ) {
            public static Governance defaults() {
                return new Governance(3, 30, Duration.ofSeconds(8), 2, false);
            }

            public Governance {
                if (maxConcurrentPerUser < 1 || maxConcurrentPerUser > 20) {
                    throw new IllegalArgumentException("maxConcurrentPerUser 必须在 1 到 20");
                }
                if (maxRequestsPerMinute < 1 || maxRequestsPerMinute > 600) {
                    throw new IllegalArgumentException("maxRequestsPerMinute 必须在 1 到 600");
                }
                if (toolTimeout == null || toolTimeout.isNegative() || toolTimeout.isZero()) {
                    toolTimeout = Duration.ofSeconds(8);
                }
                if (requestDedupSeconds < 0 || requestDedupSeconds > 60) {
                    requestDedupSeconds = 2;
                }
            }
        }

        public record Support(
                String humanPhone,
                String humanHours,
                int chatRetentionDays,
                Duration sessionIdle
        ) {
            public static Support defaults() {
                return new Support("400-800-8820", "工作日 10:00-18:00（非实时在线，请留言或稍后再拨）", 90, Duration.ofHours(24));
            }

            public Support {
                if (humanPhone == null || humanPhone.isBlank()) {
                    humanPhone = "400-800-8820";
                }
                if (humanHours == null || humanHours.isBlank()) {
                    humanHours = "工作日 10:00-18:00";
                }
                if (chatRetentionDays < 7) {
                    chatRetentionDays = 7;
                }
                if (sessionIdle == null || sessionIdle.isNegative() || sessionIdle.isZero()) {
                    sessionIdle = Duration.ofHours(24);
                }
            }
        }

        public record Skills(boolean changeAddressEnabled, boolean humanTransferEnabled) {
            public static Skills defaults() {
                return new Skills(true, true);
            }
        }
    }
}
