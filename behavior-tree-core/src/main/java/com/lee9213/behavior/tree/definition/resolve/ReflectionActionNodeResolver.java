package com.lee9213.behavior.tree.definition.resolve;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.definition.exception.DefinitionAssemblyException;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.node.IActionNode;

/**
 * 按 {@code beanName} 全限定类名反射实例化 {@link IActionNode}（无 Spring 容器）。
 */
public final class ReflectionActionNodeResolver implements ActionNodeResolver {

    @Override
    @SuppressWarnings("unchecked")
    public <Result extends NodeResult, Context extends BaseContext> IActionNode<Result, Context> resolveAction(
            BehaviorDefinitionNode node,
            Class<Result> resultClass) {
        if (node.beanName() == null || node.beanName().isEmpty()) {
            throw new DefinitionAssemblyException("Action node missing beanName: " + node.nodeName());
        }
        try {
            Class<?> cls = Class.forName(node.beanName());
            Object o = cls.getDeclaredConstructor().newInstance();
            if (!(o instanceof IActionNode)) {
                throw new DefinitionAssemblyException("Not an IActionNode: " + node.beanName());
            }
            return (IActionNode<Result, Context>) o;
        } catch (DefinitionAssemblyException e) {
            throw e;
        } catch (Exception e) {
            throw new DefinitionAssemblyException("Failed to instantiate action: " + node.beanName(), e);
        }
    }
}
