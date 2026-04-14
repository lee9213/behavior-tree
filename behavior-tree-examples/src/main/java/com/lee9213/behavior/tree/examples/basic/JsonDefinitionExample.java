package com.lee9213.behavior.tree.examples.basic;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.definition.BehaviorTreeDefinitionLoader;
import com.lee9213.behavior.tree.definition.DefinitionFormat;

import java.io.InputStream;

/**
 * 使用 JSON 定义加载行为树的例子
 */
public class JsonDefinitionExample {

    public static void main(String[] args) {
        // 创建行为树定义加载器
        BehaviorTreeDefinitionLoader loader = new BehaviorTreeDefinitionLoader();

        // 从资源文件加载 JSON 定义
        try (InputStream inputStream = JsonDefinitionExample.class.getClassLoader()
                .getResourceAsStream("definitions/basic.json")) {

            if (inputStream == null) {
                System.err.println("找不到定义文件: definitions/basic.json");
                return;
            }

            // 解析 JSON 定义为行为树节点
            var rootNode = loader.parse(inputStream, DefinitionFormat.JSON, NodeResult.class);

            // 创建行为树并执行
            BehaviorTree<NodeResult, BaseContext> tree = new BehaviorTree<>(rootNode);
            BaseContext context = new BaseContext();
            NodeResult result = tree.execute(context);

            System.out.println("行为树执行结果: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
