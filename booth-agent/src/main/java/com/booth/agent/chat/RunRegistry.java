package com.booth.agent.chat;

import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RunRegistry {

    private final ConcurrentHashMap<String, ChatRun> runs = new ConcurrentHashMap<>();

    public ChatRun start(String memberToken, String conversationId) {
        ChatRun run = ChatRun.start(memberToken, conversationId);
        runs.put(run.id(), run);
        return run;
    }

    public boolean cancel(String memberToken, String runId) {
        ChatRun run = runs.get(runId);
        if (run == null) {
            return false;
        }
        if (!run.memberHash().equals(Tokens.fingerprint(memberToken))) {
            throw new ApiException(403, 40301, "不能取消别人的对话");
        }
        return run.cancel();
    }

    public void finish(String runId) {
        if (runId != null) {
            runs.remove(runId);
        }
    }
}
