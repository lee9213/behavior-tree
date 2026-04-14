package com.lee9213.behavior.tree.definition.resolve;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.tree.definition.exception.DefinitionAssemblyException;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.spring.SpringNodeUtil;
import com.lee9213.behavior.tree.node.IActionNode;

/**
 * 从 Spring {@link org.springframework.context.ApplicationContext} 按 Bean 名解析 {@link IActionNode}。
 */
public final class SpringBeanActionNodeResolver implements ActionNodeResolver {

    @Override
    @SuppressWarnings("unchecked")
    public <Result extends NodeResult, Context extends BaseContext> IActionNode<Result, Context> resolveAction(
            BehaviorDefinitionNode node,
            Class<Result> resultClass) {
        if (node.beanName() == null || node.beanName().isEmpty()) {
            throw new DefinitionAssemblyException("Action node missing beanName: " + node.nodeName());
        }
        IActionNode<?, ?> raw = SpringNodeUtil.getBehaviorNode(node.beanName());
        if (raw == null) {
            throw new DefinitionAssemblyException("No IActionNode bean named: " + node.beanName());
        }
        return (IActionNode<Result, Context>) raw;
    }
}
