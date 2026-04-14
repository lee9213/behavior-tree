package com.lee9213.behavior.tree.engine;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.engine.store.ProcessInstanceStore;
import com.lee9213.behavior.tree.engine.store.StoreException;

import java.util.Optional;

/**
 * 同步流程运行器。根据设计 §3：当配置了存储时加载现有的快照，然后在持久化边界写入 RUNNING 和终端状态。
 */
public final class FlowEngine {

    private final FlowEngineConfig config;

    public FlowEngine(FlowEngineConfig config) {
        this.config = config;
    }

    public <C extends BaseContext> NodeResult run(String instanceId, FlowDefinition<C> definition, C context) throws StoreException {
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
            }
            FlowInstanceSnapshot initial = FlowInstanceSnapshot.builder()
                    .definitionId(definition.getId())
                    .definitionVersion(definition.getVersion())
                    .status("RUNNING")
                    .build();
            store.save(instanceId, initial);
        }
        NodeResult result = definition.getBehaviorTree().execute(context);
        if (store != null) {
            store.save(instanceId, FlowInstanceSnapshot.builder()
                    .definitionId(definition.getId())
                    .definitionVersion(definition.getVersion())
                    .status(result.isSuccess() ? "SUCCESS" : "FAILURE")
                    .build());
        }
        return result;
    }
}
