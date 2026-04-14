package com.lee9213.behavior.tree.retry;

public record RetryPolicy(
        int maxAttempts,
        long baseDelayMillis,
        double multiplier,
        double jitterRatio // 相对当前 delay，0..1
) {
    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts");
        }
        if (jitterRatio < 0 || jitterRatio > 1) {
            throw new IllegalArgumentException("jitterRatio");
        }
    }
}
