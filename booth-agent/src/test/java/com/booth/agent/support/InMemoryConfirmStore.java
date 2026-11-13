package com.booth.agent.support;

import com.booth.agent.confirm.ConfirmStore;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryConfirmStore implements ConfirmStore {

    private final ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();

    @Override
    public void save(String key, String json, Duration ttl) {
        data.put(key, json);
    }

    @Override
    public String get(String key) {
        return data.get(key);
    }

    @Override
    public boolean delete(String key) {
        return data.remove(key) != null;
    }
}
