package com.booth.agent.chat;

import com.booth.agent.config.AppConfig;
import com.booth.agent.governance.UserRateLimiter;
import com.booth.agent.trace.TraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@Import({AppConfig.class, RunRegistry.class})
class ChatStreamEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatOrchestrator chatOrchestrator;

    @MockitoBean
    private TraceService traceService;

    @MockitoBean
    private UserRateLimiter rateLimiter;

    @BeforeEach
    void allowRate() {
        when(rateLimiter.acquire(anyString())).thenReturn(UserRateLimiter.Lease.NOOP);
    }

    @Test
    void streamEmitsProgressBeforeBody() throws Exception {
        when(chatOrchestrator.chat(anyString(), any(ChatRequest.class), any(ChatRun.class), any(ChatListener.class)))
                .thenAnswer(invocation -> {
                    ChatListener listener = invocation.getArgument(3);
                    listener.progress("正在查订单");
                    listener.token("已完成");
                    return new ChatResponse("已完成", "ORDER_QUERY", "conv-sse", null, List.of(), Map.of());
                });

        MvcResult started = mockMvc.perform(post("/app/chat/stream")
                        .header("Authorization", "Bearer member-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查订单 202610301234567890\",\"conversationId\":\"conv-sse\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = new String(mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        int progress = body.indexOf("正在查订单");
        int token = body.indexOf("已完成");
        if (progress < 0 || token < progress) {
            throw new AssertionError(body);
        }
    }
}
