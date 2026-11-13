package com.booth.agent.confirm;

import java.time.Duration;

public interface ConfirmStore {

    void save(String key, String json, Duration ttl);

    String get(String key);

    /** @return 这一次确实删掉了已存在的值 */
    boolean delete(String key);
}
