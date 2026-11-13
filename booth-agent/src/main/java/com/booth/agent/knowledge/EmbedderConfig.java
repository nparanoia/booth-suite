package com.booth.agent.knowledge;

import com.booth.agent.config.BoothAgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EmbedderConfig {

    @Bean
    TextEmbedder textEmbedder(BoothAgentProperties properties) {
        BoothAgentProperties.Agent.Embedding embedding = properties.agent().embedding();
        String provider = embedding == null || embedding.provider() == null
                ? "auto" : embedding.provider().trim().toLowerCase();
        if ("ngram".equals(provider)) {
            log.info("knowledge embedder=ngram");
            return new NgramEmbedder();
        }
        if ("openai".equals(provider) || ("auto".equals(provider) && properties.embeddingModelEnabled())) {
            if (!properties.embeddingModelEnabled()) {
                throw new IllegalStateException("embedding.provider=openai 但没有 OPENAI_API_KEY");
            }
            log.info("knowledge embedder={}", "openai");
            return new OpenAiEmbeddingClient(properties);
        }
        log.info("knowledge embedder=ngram");
        return new NgramEmbedder();
    }
}
