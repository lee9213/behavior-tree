package com.lee9213.behavior.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lee9213.behavior.engine.FlowEngine;
import com.lee9213.behavior.engine.FlowEngineConfig;
import com.lee9213.behavior.spring.redis.RedisProcessInstanceStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(FlowEngineProperties.class)
public class BehaviorFlowAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    RedisProcessInstanceStore redisProcessInstanceStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            FlowEngineProperties properties) {
        return new RedisProcessInstanceStore(redisTemplate, objectMapper, properties.getRedisEntryTtl());
    }

    @Bean
    FlowEngineConfig flowEngineConfig(FlowEngineProperties properties, ObjectProvider<RedisProcessInstanceStore> redisStore) {
        FlowEngineConfig config = new FlowEngineConfig();
        config.setRetryEnabled(properties.isRetryEnabled());
        redisStore.ifAvailable(config::setStore);
        return config;
    }

    @Bean
    FlowEngine flowEngine(FlowEngineConfig flowEngineConfig) {
        return new FlowEngine(flowEngineConfig);
    }
}
