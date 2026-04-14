package com.lee9213.behavior.tree.examples.spring;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.definition.BehaviorTreeDefinitionLoader;
import com.lee9213.behavior.tree.definition.DefinitionFormat;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;

/**
 * 行为树服务
 */
@Service
public class BehaviorTreeService {

    private BehaviorTree<NodeResult, BaseContext> tree;

    @PostConstruct
    public void init() {
        try {
            // 从 JSON 定义加载行为树
            BehaviorTreeDefinitionLoader loader = new BehaviorTreeDefinitionLoader();
            try (InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream("definitions/basic.json")) {

                if (inputStream == null) {
                    System.err.println("找不到定义文件: definitions/basic.json");
                    return;
                }

                var rootNode = loader.parse(inputStream, DefinitionFormat.JSON, NodeResult.class);
                tree = new BehaviorTree<>(rootNode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public NodeResult execute() {
        if (tree == null) {
            System.err.println("行为树未初始化");
            return NodeResult.FAILURE;
        }

        BaseContext context = new BaseContext();
        return tree.execute(context);
    }

    public NodeResult executeManualTree() {
        // 手动创建行为树
        BehaviorTree<NodeResult, BaseContext> manualTree = BehaviorTree.<NodeResult, BaseContext>builder()
                .sequence("root")
                    .action("action1", ctx -> {
                        System.out.println("执行手动创建的动作 1");
                        return NodeResult.SUCCESS;
                    })
                    .action("action2", ctx -> {
                        System.out.println("执行手动创建的动作 2");
                        return NodeResult.SUCCESS;
                    })
                .end()
                .build();

        BaseContext context = new BaseContext();
        return manualTree.execute(context);
    }
}
