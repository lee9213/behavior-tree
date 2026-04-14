package com.lee9213.behavior.definition.resolve;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.TestContext;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.enums.NodeType;
import com.lee9213.behavior.tree.node.IActionNode;
import com.lee9213.behavior.tree.node.impl.SuccessActionNodeImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReflectionActionNodeResolverTest {

    @Test
    void resolvesFqcn() {
        ReflectionActionNodeResolver resolver = new ReflectionActionNodeResolver();
        BehaviorDefinitionNode node = new BehaviorDefinitionNode(
                "n",
                NodeType.Action,
                SuccessActionNodeImpl.class.getName(),
                null,
                java.util.List.of(),
                null,
                java.util.Map.of()
        );
        IActionNode<NodeResult, TestContext> action =
                resolver.resolveAction(node, NodeResult.class);
        assertNotNull(action);
    }
}
