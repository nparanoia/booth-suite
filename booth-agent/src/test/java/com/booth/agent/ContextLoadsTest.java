package com.booth.agent;

import com.booth.agent.knowledge.LocalPg;
import com.booth.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.data.redis.timeout=500ms",
        "spring.data.redis.connect-timeout=500ms",
        "booth.agent.embedding.provider=ngram"
})
class ContextLoadsTest {

    static {
        LocalPg.ensureStarted();
    }

    @Autowired
    private ToolRegistry registry;

    @Test
    void registersExactlyTheAllowlist() {
        assertEquals(List.of(
                        "list_returns",
                        "prepare_cancel",
                        "prepare_change_address",
                        "prepare_return",
                        "query_order",
                        "search_policy",
                        "transfer_human"),
                registry.names());
    }
}
