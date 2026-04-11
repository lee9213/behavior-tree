package com.lee9213.behavior.spring.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lee9213.behavior.engine.FlowInstanceSnapshot;
import com.lee9213.behavior.engine.store.ProcessInstanceStore;
import com.lee9213.behavior.engine.store.StoreException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RedisProcessInstanceStore implements ProcessInstanceStore {

    private static final String KEY_PREFIX = "flow:instance:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisProcessInstanceStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private static String key(String instanceId) {
        return KEY_PREFIX + instanceId;
    }

    @Override
    public Optional<FlowInstanceSnapshot> load(String instanceId) throws StoreException {
        try {
            String json = redisTemplate.opsForValue().get(key(instanceId));
            if (json == null || json.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(deserialize(json));
        } catch (Exception e) {
            throw new StoreException("Failed to load flow instance snapshot: " + instanceId, e);
        }
    }

    @Override
    public void save(String instanceId, FlowInstanceSnapshot snapshot) throws StoreException {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(key(instanceId), json);
        } catch (JsonProcessingException e) {
            throw new StoreException("Failed to serialize flow instance snapshot: " + instanceId, e);
        } catch (Exception e) {
            throw new StoreException("Failed to save flow instance snapshot: " + instanceId, e);
        }
    }

    @Override
    public void delete(String instanceId) throws StoreException {
        try {
            redisTemplate.delete(key(instanceId));
        } catch (Exception e) {
            throw new StoreException("Failed to delete flow instance snapshot: " + instanceId, e);
        }
    }

    private FlowInstanceSnapshot deserialize(String json) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);
        Map<String, Integer> retries = objectMapper.convertValue(
                root.path("retryCountByStepPath"),
                new TypeReference<Map<String, Integer>>() {});
        if (retries == null) {
            retries = new HashMap<>();
        }
        return FlowInstanceSnapshot.builder()
                .definitionId(root.path("definitionId").asText(null))
                .definitionVersion(root.path("definitionVersion").asText(null))
                .status(root.path("status").asText(null))
                .retryCountByStepPath(retries)
                .build();
    }
}
