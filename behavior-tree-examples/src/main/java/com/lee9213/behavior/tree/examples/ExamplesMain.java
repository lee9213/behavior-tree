package com.lee9213.behavior.tree.examples;

import com.lee9213.behavior.tree.examples.basic.JsonDefinitionExample;
import com.lee9213.behavior.tree.examples.basic.ManualCreationExample;
import com.lee9213.behavior.tree.examples.basic.XmlDefinitionExample;
import com.lee9213.behavior.tree.examples.engine.FlowEngineExample;

/**
 * 所有例子的主入口
 */
public class ExamplesMain {

    public static void main(String[] args) {
        System.out.println("=== 行为树示例 ===");

        // 1. 手动创建行为树
        System.out.println("\n1. 手动创建行为树:");
        ManualCreationExample.main(args);

        // 2. 使用 JSON 定义加载行为树
        System.out.println("\n2. 使用 JSON 定义加载行为树:");
        JsonDefinitionExample.main(args);

        // 3. 使用 XML 定义加载行为树
        System.out.println("\n3. 使用 XML 定义加载行为树:");
        XmlDefinitionExample.main(args);

        // 4. 使用流程引擎
        System.out.println("\n4. 使用流程引擎:");
        FlowEngineExample.main(args);

        // 5. Spring 集成例子
        System.out.println("\n5. Spring 集成例子:");
        System.out.println("   运行 BehaviorTreeSpringBootApplication 类，然后访问以下 URL:");
        System.out.println("   - http://localhost:8080/execute (执行从 JSON 加载的行为树)");
        System.out.println("   - http://localhost:8080/execute-manual (执行手动创建的行为树)");

        System.out.println("\n=== 示例结束 ===");
    }
}
