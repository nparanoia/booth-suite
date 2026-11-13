package com.booth.agent.audit;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ChatTranscriptBootstrap {

    private final ChatTranscriptRepository repository;

    public ChatTranscriptBootstrap(ChatTranscriptRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        repository.ensureSchema();
    }
}
