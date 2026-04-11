package com.lee9213.behavior.engine;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.BehaviorTree;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.impl.SuccessActionNodeImpl;
import com.lee9213.behavior.engine.store.ProcessInstanceStore;
import com.lee9213.behavior.engine.store.StoreException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowEngineStoreTest {

    @Test
    void run_withStore_loadsBeforeSave_and_savesRunningThenTerminal() throws StoreException {
        AtomicInteger loadCount = new AtomicInteger();
        AtomicInteger saveCount = new AtomicInteger();
        ProcessInstanceStore store = new ProcessInstanceStore() {
            @Override
            public Optional<FlowInstanceSnapshot> load(String instanceId) {
                loadCount.incrementAndGet();
                return Optional.empty();
            }

            @Override
            public void save(String instanceId, FlowInstanceSnapshot snapshot) {
                saveCount.incrementAndGet();
            }

            @Override
            public void delete(String instanceId) {
            }
        };

        FlowEngineConfig config = new FlowEngineConfig();
        config.setStore(store);
        FlowEngine engine = new FlowEngine(config);

        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root =
                new BehaviorNodeWrapper<>("leaf", new SuccessActionNodeImpl<>());
        BehaviorTree<NodeResult, FlowExecutionContext> tree = new BehaviorTree<>(root);
        FlowDefinition def = new FlowDefinition("flow-a", "1.0.0", tree);

        engine.run("inst-1", def, new FlowExecutionContext());

        assertEquals(1, loadCount.get());
        assertTrue(saveCount.get() >= 2, "expected RUNNING then SUCCESS saves");
        assertEquals(2, saveCount.get());
    }

    @Test
    void run_whenSnapshotExists_butDefinitionMismatch_throws() {
        ProcessInstanceStore store = new ProcessInstanceStore() {
            @Override
            public Optional<FlowInstanceSnapshot> load(String instanceId) {
                return Optional.of(FlowInstanceSnapshot.builder()
                        .definitionId("other")
                        .definitionVersion("1.0.0")
                        .status("RUNNING")
                        .retryCountByStepPath(Map.of())
                        .build());
            }

            @Override
            public void save(String instanceId, FlowInstanceSnapshot snapshot) {
            }

            @Override
            public void delete(String instanceId) {
            }
        };
        FlowEngineConfig config = new FlowEngineConfig();
        config.setStore(store);
        FlowEngine engine = new FlowEngine(config);
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root =
                new BehaviorNodeWrapper<>("leaf", new SuccessActionNodeImpl<>());
        BehaviorTree<NodeResult, FlowExecutionContext> tree = new BehaviorTree<>(root);
        FlowDefinition def = new FlowDefinition("flow-a", "1.0.0", tree);

        assertThrows(StoreException.class, () -> engine.run("inst-1", def, new FlowExecutionContext()));
    }

    @Test
    void run_hydratesRetryMapFromSnapshot() throws StoreException {
        Map<String, Integer> retries = new HashMap<>();
        retries.put("0_1", 2);
        ProcessInstanceStore store = new ProcessInstanceStore() {
            @Override
            public Optional<FlowInstanceSnapshot> load(String instanceId) {
                return Optional.of(FlowInstanceSnapshot.builder()
                        .definitionId("flow-a")
                        .definitionVersion("1.0.0")
                        .status("RUNNING")
                        .retryCountByStepPath(retries)
                        .build());
            }

            @Override
            public void save(String instanceId, FlowInstanceSnapshot snapshot) {
            }

            @Override
            public void delete(String instanceId) {
            }
        };
        FlowEngineConfig config = new FlowEngineConfig();
        config.setStore(store);
        FlowEngine engine = new FlowEngine(config);
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root =
                new BehaviorNodeWrapper<>("leaf", new SuccessActionNodeImpl<>());
        BehaviorTree<NodeResult, FlowExecutionContext> tree = new BehaviorTree<>(root);
        FlowDefinition def = new FlowDefinition("flow-a", "1.0.0", tree);
        FlowExecutionContext ctx = new FlowExecutionContext();
        engine.run("inst-1", def, ctx);
        assertEquals(2, ctx.getRetryCountByStepPath().get("0_1"));
    }
}
