package com.lee9213.behavior.engine;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.BehaviorTree;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.impl.SuccessActionNodeImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlowDefinitionValidatorTest {

    @Test
    void validate_blankId_fails() {
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root =
                new BehaviorNodeWrapper<>("leaf", new SuccessActionNodeImpl<>());
        BehaviorTree<NodeResult, FlowExecutionContext> tree = new BehaviorTree<>(root);
        FlowDefinition def = new FlowDefinition("   ", "1.0", tree);
        assertThrows(InvalidFlowDefinitionException.class, () -> FlowDefinitionValidator.validate(def));
    }

    @Test
    void validate_minimalSuccessLeaf_passes() {
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root =
                new BehaviorNodeWrapper<>("leaf", new SuccessActionNodeImpl<>());
        BehaviorTree<NodeResult, FlowExecutionContext> tree = new BehaviorTree<>(root);
        FlowDefinition def = new FlowDefinition("flow-a", "1.0.0", tree);
        assertDoesNotThrow(() -> FlowDefinitionValidator.validate(def));
    }
}
