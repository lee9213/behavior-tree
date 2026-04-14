package com.lee9213.behavior.tree.examples.engine;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.engine.FlowDefinition;
import com.lee9213.behavior.tree.engine.FlowEngine;
import com.lee9213.behavior.tree.engine.FlowEngineConfig;

/**
 * 使用流程引擎的例子
 */
public class FlowEngineExample {

    public static void main(String[] args) {
        try {
            // 创建一个行为树
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

            // 创建流程定义
            FlowDefinition<BaseContext> definition = new FlowDefinition<>("test-flow", "1.0", tree);

            // 配置流程引擎
            FlowEngineConfig config = new FlowEngineConfig();
            // 添加内存存储
            config.setStore(new InMemoryProcessInstanceStore());

            // 创建流程引擎
            FlowEngine engine = new FlowEngine(config);

            // 执行流程
            BaseContext context = new BaseContext();
            String instanceId = "test-instance-123";
            NodeResult result = engine.run(instanceId, definition, context);

            System.out.println("流程执行结果: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
