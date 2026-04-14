package com.lee9213.behavior.tree;

import com.lee9213.behavior.tree.node.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 行为树手动创建测试
 */
class BehaviorTreeManualCreationTest {

    @Test
    void testBasicSequence() {
        BehaviorTree<NodeResult, BaseContext> tree = BehaviorTree.<NodeResult, BaseContext>builder()
                .sequence("root")
                    .action("action1", ctx -> {
                        System.out.println("Action 1 executed");
                        return NodeResult.SUCCESS;
                    })
                    .action("action2", ctx -> {
                        System.out.println("Action 2 executed");
                        return NodeResult.SUCCESS;
                    })
                .end()
                .build();

        BaseContext ctx = new BaseContext();
        NodeResult result = tree.execute(ctx);
        assertEquals(NodeResult.SUCCESS, result);
    }

    @Test
    void testSelector() {
        BehaviorTree<NodeResult, BaseContext> tree = BehaviorTree.<NodeResult, BaseContext>builder()
                .selector("root")
                    .action("action1", ctx -> {
                        System.out.println("Action 1 executed (failure)");
                        return NodeResult.FAILURE;
                    })
                    .action("action2", ctx -> {
                        System.out.println("Action 2 executed (success)");
                        return NodeResult.SUCCESS;
                    })
                .end()
                .build();

        BaseContext ctx = new BaseContext();
        NodeResult result = tree.execute(ctx);
        assertEquals(NodeResult.SUCCESS, result);
    }

    @Test
    void testNestedNodes() {
        BehaviorTree<NodeResult, BaseContext> tree = BehaviorTree.<NodeResult, BaseContext>builder()
                .sequence("root")
                    .action("checkCondition", ctx -> {
                        System.out.println("Check condition executed");
                        return NodeResult.SUCCESS;
                    })
                    .selector("decision")
                        .sequence("option1")
                            .action("option1_1", ctx -> {
                                System.out.println("Option 1.1 executed (failure)");
                                return NodeResult.FAILURE;
                            })
                        .end()
                        .action("option2", ctx -> {
                            System.out.println("Option 2 executed (success)");
                            return NodeResult.SUCCESS;
                        })
                    .end()
                .end()
                .build();

        BaseContext ctx = new BaseContext();
        NodeResult result = tree.execute(ctx);
        assertEquals(NodeResult.SUCCESS, result);
    }
}
