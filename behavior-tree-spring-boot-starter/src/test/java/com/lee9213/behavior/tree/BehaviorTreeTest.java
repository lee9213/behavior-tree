package com.lee9213.behavior.tree;

import com.lee9213.behavior.tree.definition.BehaviorTreeDefinitionLoader;
import com.lee9213.behavior.tree.definition.DefinitionFormat;
import com.lee9213.behavior.tree.definition.resolve.CompositeActionNodeResolver;
import com.lee9213.behavior.tree.definition.resolve.ReflectionActionNodeResolver;
import com.lee9213.behavior.tree.definition.resolve.SpringBeanActionNodeResolver;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.spring.annotation.EnableBehavior;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 15:09
 */
@SpringBootTest(classes = TestConfiguration.class)
public class BehaviorTreeTest {

    @Autowired
    private SpringBeanActionNodeResolver springBeanActionNodeResolver;

    @Test
    public void execute() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/definitions/golden.json")) {
            CompositeActionNodeResolver resolver = new CompositeActionNodeResolver(
                    springBeanActionNodeResolver,
                    new ReflectionActionNodeResolver());
            BehaviorTreeDefinitionLoader loader = new BehaviorTreeDefinitionLoader(resolver);
            INode<TestNodeResult, BaseContext> root =
                    loader.parse(in, StandardCharsets.UTF_8, DefinitionFormat.JSON, TestNodeResult.class);
            TestContext testContext = new TestContext();
            BehaviorTree<TestNodeResult, BaseContext> behaviorTree = new BehaviorTree<>(root);
            behaviorTree.execute(testContext);
        }
    }
}

@Configuration
@EnableBehavior
@ComponentScan("com.lee9213.behavior")
class TestConfiguration {

}
