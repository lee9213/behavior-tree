package com.lee9213.behavior.tree.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "behavior.flow")
public class FlowEngineProperties {

    private boolean retryEnabled = true;

    /**
     * Redis 中流程实例快照的过期时间；未设置或非正数表示永不过期（不设 TTL）。
     * 示例：{@code behavior.flow.redis-entry-ttl=7d}
     */
    private Duration redisEntryTtl;

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public Duration getRedisEntryTtl() {
        return redisEntryTtl;
    }

    public void setRedisEntryTtl(Duration redisEntryTtl) {
        this.redisEntryTtl = redisEntryTtl;
    }
}
