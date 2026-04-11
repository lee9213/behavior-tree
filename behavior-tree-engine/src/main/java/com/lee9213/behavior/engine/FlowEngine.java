package com.lee9213.behavior.engine;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.engine.store.ProcessInstanceStore;
import com.lee9213.behavior.engine.store.StoreException;
import com.lee9213.behavior.flow.FlowExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Synchronous flow runner. Per design §3: loads an existing snapshot when a store is configured, then writes
 * RUNNING and terminal states at persistence boundaries.
 */
public final class FlowEngine {

    private final FlowEngineConfig config;

    public FlowEngine(FlowEngineConfig config) {
        this.config = config;
    }

    public NodeResult run(String instanceId, FlowDefinition definition, FlowExecutionContext context) throws StoreException {
        FlowDefinitionValidator.validate(definition);
        context.setFlowInstanceId(instanceId);
        ProcessInstanceStore store = config.getStore();
        if (store != null) {
            Optional<FlowInstanceSnapshot> existing = store.load(instanceId);
            if (existing.isPresent()) {
                FlowInstanceSnapshot prev = existing.get();
                if (!definition.getId().equals(prev.getDefinitionId())
                        || !definition.getVersion().equals(prev.getDefinitionVersion())) {
                    throw new StoreException(
                            "Existing snapshot does not match FlowDefinition for instanceId=" + instanceId);
                }
                Map<String, Integer> retries = prev.getRetryCountByStepPath();
                if (retries != null && !retries.isEmpty()) {
                    context.setRetryCountByStepPath(new HashMap<>(retries));
                }
            }
            FlowInstanceSnapshot initial = FlowInstanceSnapshot.builder()
                    .definitionId(definition.getId())
                    .definitionVersion(definition.getVersion())
                    .status("RUNNING")
                    .retryCountByStepPath(new HashMap<>(context.getRetryCountByStepPath()))
                    .build();
            store.save(instanceId, initial);
        }
        NodeResult result = definition.getBehaviorTree().execute(context);
        if (store != null) {
            store.save(instanceId, FlowInstanceSnapshot.builder()
                    .definitionId(definition.getId())
                    .definitionVersion(definition.getVersion())
                    .status(result.isSuccess() ? "SUCCESS" : "FAILURE")
                    .retryCountByStepPath(new HashMap<>(context.getRetryCountByStepPath()))
                    .build());
        }
        return result;
    }
}
