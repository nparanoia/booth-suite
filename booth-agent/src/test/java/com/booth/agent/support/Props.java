package com.booth.agent.support;

import com.booth.agent.config.BoothAgentProperties;

import java.time.Duration;

public final class Props {

    private Props() {
    }

    public static BoothAgentProperties rules() {
        return of(4, 2, false, "");
    }

    public static BoothAgentProperties llm() {
        return of(4, 2, true, "sk-test");
    }

    public static BoothAgentProperties live(String apiKey, String baseUrl, String model) {
        String url = baseUrl == null || baseUrl.isBlank() ? "https://api.deepseek.com" : baseUrl.trim();
        String chatModel = model == null || model.isBlank() ? "deepseek-chat" : model.trim();
        return new BoothAgentProperties(
                "http://127.0.0.1:9",
                new BoothAgentProperties.Agent(
                        Duration.ofMinutes(10),
                        1,
                        0.72,
                        6,
                        2,
                        new BoothAgentProperties.Agent.Llm(true, url, apiKey, chatModel),
                        new BoothAgentProperties.Agent.Embedding("ngram", "text-embedding-3-small", 256),
                        BoothAgentProperties.Agent.Governance.defaults(),
                        BoothAgentProperties.Agent.Support.defaults(),
                        BoothAgentProperties.Agent.Skills.defaults()
                )
        );
    }

    public static BoothAgentProperties of(int maxSteps, int timeoutRetries, boolean llmEnabled, String apiKey) {
        return new BoothAgentProperties(
                "http://127.0.0.1:9",
                new BoothAgentProperties.Agent(
                        Duration.ofMinutes(10),
                        1,
                        0.72,
                        maxSteps,
                        timeoutRetries,
                        new BoothAgentProperties.Agent.Llm(llmEnabled, "http://127.0.0.1:9", apiKey, "gpt-4o-mini"),
                        new BoothAgentProperties.Agent.Embedding("ngram", "text-embedding-3-small", 256),
                        BoothAgentProperties.Agent.Governance.defaults(),
                        BoothAgentProperties.Agent.Support.defaults(),
                        BoothAgentProperties.Agent.Skills.defaults()
                )
        );
    }
}
