package com.lee9213.behavior.tree.examples.basic;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.ActionNodeFunction;

/**
 * 手动创建行为树的例子
 */
public class ManualCreationExample {

    public static void main(String[] args) {
        // 创建一个简单的行为树：顺序执行两个动作
        BehaviorTree<NodeResult, BaseContext> tree = BehaviorTree.<NodeResult, BaseContext>builder()
                .sequence("root")
                    .action("action1", ctx -> {
                        System.out.println("执行动作 1");
                        return NodeResult.SUCCESS;
                    })
                    .action("action2", ctx -> {
                        System.out.println("执行动作 2");
                        return NodeResult.SUCCESS;
                    })
                .end()
                .build();

        // 执行行为树
        BaseContext context = new BaseContext();
        NodeResult result = tree.execute(context);
        System.out.println("行为树执行结果: " + result);

        // 创建一个包含选择节点的行为树
        BehaviorTree<NodeResult, BaseContext> selectionTree = BehaviorTree.<NodeResult, BaseContext>builder()
                .selector("root")
                    .action("action1", ctx -> {
                        System.out.println("执行动作 1 (失败)");
                        return NodeResult.FAILURE;
                    })
                    .action("action2", ctx -> {
                        System.out.println("执行动作 2 (成功)");
                        return NodeResult.SUCCESS;
                    })
                .end()
                .build();

        // 执行选择行为树
        NodeResult selectionResult = selectionTree.execute(context);
        System.out.println("选择行为树执行结果: " + selectionResult);

        // 创建一个嵌套的行为树
        BehaviorTree<NodeResult, BaseContext> nestedTree = BehaviorTree.<NodeResult, BaseContext>builder()
                .sequence("root")
                    .action("checkCondition", ctx -> {
                        System.out.println("检查条件");
                        return NodeResult.SUCCESS;
                    })
                    .selector("decision")
                        .sequence("option1")
                            .action("option1_1", ctx -> {
                                System.out.println("执行选项 1.1 (失败)");
                                return NodeResult.FAILURE;
                            })
                        .end()
                        .action("option2", ctx -> {
                            System.out.println("执行选项 2 (成功)");
                            return NodeResult.SUCCESS;
                        })
                    .end()
                    .action("finalAction", ctx -> {
                        System.out.println("执行最终动作");
                        return NodeResult.SUCCESS;
                    })
                .end()
                .build();

        // 执行嵌套行为树
        NodeResult nestedResult = nestedTree.execute(context);
        System.out.println("嵌套行为树执行结果: " + nestedResult);
    }
}
