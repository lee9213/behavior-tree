package com.lee9213.behavior.definition.assemble;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.TestContext;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.definition.resolve.ReflectionActionNodeResolver;
import com.lee9213.behavior.tree.enums.NodeType;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.impl.SuccessActionNodeImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefinitionAssemblerTest {

    @Test
    void singleActionSuccess() {
        BehaviorDefinitionNode def = new BehaviorDefinitionNode(
                "leaf",
                NodeType.Action,
                SuccessActionNodeImpl.class.getName(),
                null,
                List.of(),
                null,
                Map.of()
        );
        INode<NodeResult, BaseContext> root =
                DefinitionAssembler.assemble(def, NodeResult.class, new ReflectionActionNodeResolver());
        TestContext ctx = new TestContext();
        assertEquals(NodeResult.SUCCESS, root.execute(ctx));
    }
}
