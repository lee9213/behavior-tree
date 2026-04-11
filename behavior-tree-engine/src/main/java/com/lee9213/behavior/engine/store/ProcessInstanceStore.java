package com.lee9213.behavior.engine.store;

import com.lee9213.behavior.engine.FlowInstanceSnapshot;

import java.util.Optional;

public interface ProcessInstanceStore {

    Optional<FlowInstanceSnapshot> load(String instanceId) throws StoreException;

    void save(String instanceId, FlowInstanceSnapshot snapshot) throws StoreException;

    void delete(String instanceId) throws StoreException;
}
