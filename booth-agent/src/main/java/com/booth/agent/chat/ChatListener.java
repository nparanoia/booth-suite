package com.booth.agent.chat;

import com.booth.agent.tool.ToolEvents;

public interface ChatListener extends ToolEvents {

    boolean enabled();

    void token(String text);

    void cancelled();

    static ChatListener none() {
        return new ChatListener() {
            @Override
            public boolean enabled() {
                return false;
            }

            @Override
            public void progress(String text) {
            }

            @Override
            public void token(String text) {
            }

            @Override
            public void cancelled() {
            }
        };
    }
}
