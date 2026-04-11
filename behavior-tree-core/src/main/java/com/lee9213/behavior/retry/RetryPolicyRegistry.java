package com.lee9213.behavior.retry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RetryPolicyRegistry {

    private final Map<String, RetryPolicy> byTag = new ConcurrentHashMap<>();
    private RetryPolicy defaultPolicy = new RetryPolicy(3, 50L, 2.0, 0.2);

    public void registerDefault(RetryPolicy policy) {
        this.defaultPolicy = policy;
    }

    public void registerForTag(String tag, RetryPolicy policy) {
        byTag.put(tag, policy);
    }

    public RetryPolicy resolve(String stepTag) {
        if (stepTag != null && byTag.containsKey(stepTag)) {
            return byTag.get(stepTag);
        }
        return defaultPolicy;
    }
}
