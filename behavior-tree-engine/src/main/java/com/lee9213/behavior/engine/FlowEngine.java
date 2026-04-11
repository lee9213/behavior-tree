package com.lee9213.behavior.engine;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.engine.store.ProcessInstanceStore;
import com.lee9213.behavior.engine.store.StoreException;
import com.lee9213.behavior.flow.FlowExecutionContext;

import java.util.HashMap;

public final class FlowEngine {

    private final FlowEngineConfig config;

    public FlowEngine(FlowEngineConfig config) {
        this.config = config;
    }

    public NodeResult run(String instanceId, FlowDefinition definition, FlowExecutionContext context) throws StoreException {
        FlowDefinitionValidator.validate(definition);
        context.setFlowInstanceId(instanceId);
        ProcessInstanceStore store = config.getStore();
        FlowInstanceSnapshot initial = FlowInstanceSnapshot.builder()
                .definitionId(definition.getId())
                .definitionVersion(definition.getVersion())
                .status("RUNNING")
                .retryCountByStepPath(new HashMap<>())
                .build();
        if (store != null) {
            store.save(instanceId, initial);
        }
        NodeResult result = definition.getBehaviorTree().execute(context);
        if (store != null) {
            store.save(instanceId, FlowInstanceSnapshot.builder()
                    .definitionId(definition.getId())
                    .definitionVersion(definition.getVersion())
                    .status(result.isSuccess() ? "SUCCESS" : "FAILURE")
                    .retryCountByStepPath(new HashMap<>())
                    .build());
        }
        return result;
    }
}
