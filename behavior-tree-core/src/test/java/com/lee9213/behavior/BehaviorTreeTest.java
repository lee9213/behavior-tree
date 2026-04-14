package com.lee9213.behavior;

import com.lee9213.behavior.definition.BehaviorTreeDefinitionLoader;
import com.lee9213.behavior.definition.DefinitionFormat;
import com.lee9213.behavior.definition.resolve.CompositeActionNodeResolver;
import com.lee9213.behavior.definition.resolve.ReflectionActionNodeResolver;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.spring.EnableBehavior;
import com.lee9213.behavior.spring.SpringBeanActionNodeResolver;
import com.lee9213.behavior.tree.BehaviorTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 15:09
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class BehaviorTreeTest {

    @Test
    public void execute() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/definitions/golden.json")) {
            CompositeActionNodeResolver resolver = new CompositeActionNodeResolver(
                    new SpringBeanActionNodeResolver(),
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
