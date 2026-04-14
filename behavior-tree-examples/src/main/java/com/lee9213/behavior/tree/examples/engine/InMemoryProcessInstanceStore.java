package com.lee9213.behavior.tree.examples.engine;

import com.lee9213.behavior.tree.engine.FlowInstanceSnapshot;
import com.lee9213.behavior.tree.engine.store.ProcessInstanceStore;
import com.lee9213.behavior.tree.engine.store.StoreException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 内存存储实现，用于演示流程引擎的持久化功能
 */
public class InMemoryProcessInstanceStore implements ProcessInstanceStore {

    private final Map<String, FlowInstanceSnapshot> store = new HashMap<>();

    @Override
    public Optional<FlowInstanceSnapshot> load(String instanceId) throws StoreException {
        return Optional.ofNullable(store.get(instanceId));
    }

    @Override
    public void save(String instanceId, FlowInstanceSnapshot snapshot) throws StoreException {
        store.put(instanceId, snapshot);
        System.out.println("保存流程实例: " + instanceId + "，状态: " + snapshot.getStatus());
    }

    @Override
    public void delete(String instanceId) throws StoreException {
        store.remove(instanceId);
    }
}
