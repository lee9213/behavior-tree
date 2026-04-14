package com.lee9213.behavior.tree.engine.store;

import com.lee9213.behavior.tree.engine.FlowInstanceSnapshot;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProcessInstanceStore implements ProcessInstanceStore {

    private final ConcurrentHashMap<String, FlowInstanceSnapshot> map = new ConcurrentHashMap<>();

    @Override
    public Optional<FlowInstanceSnapshot> load(String instanceId) throws StoreException {
        return Optional.ofNullable(map.get(instanceId));
    }

    @Override
    public void save(String instanceId, FlowInstanceSnapshot snapshot) throws StoreException {
        map.put(instanceId, snapshot);
    }

    @Override
    public void delete(String instanceId) throws StoreException {
        map.remove(instanceId);
    }
}
