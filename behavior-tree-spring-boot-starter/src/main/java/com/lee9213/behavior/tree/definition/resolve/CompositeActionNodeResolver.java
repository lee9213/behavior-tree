package com.lee9213.behavior.tree.definition.resolve;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.node.IActionNode;

/**
 * {@code container=spring} 时使用 Spring 解析，否则使用反射类名实例化。
 */
public final class CompositeActionNodeResolver implements ActionNodeResolver {

    private final SpringBeanActionNodeResolver spring;
    private final ReflectionActionNodeResolver reflection;

    public CompositeActionNodeResolver(
            SpringBeanActionNodeResolver spring,
            ReflectionActionNodeResolver reflection) {
        this.spring = spring;
        this.reflection = reflection;
    }

    @Override
    public <Result extends NodeResult, Context extends BaseContext> IActionNode<Result, Context> resolveAction(
            BehaviorDefinitionNode node,
            Class<Result> resultClass) {
        if (node.container() != null && node.container().equalsIgnoreCase("spring")) {
            return spring.resolveAction(node, resultClass);
        }
        return reflection.resolveAction(node, resultClass);
    }
}
