package com.lee9213.behavior.tree.definition.resolve;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.definition.exception.DefinitionAssemblyException;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.node.IActionNode;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 从 Spring {@link org.springframework.context.ApplicationContext} 按 Bean 名解析 {@link IActionNode}。
 */
public final class SpringBeanActionNodeResolver implements ActionNodeResolver, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Result extends NodeResult, Context extends BaseContext> IActionNode<Result, Context> resolveAction(
            BehaviorDefinitionNode node,
            Class<Result> resultClass) {
        if (node.beanName() == null || node.beanName().isEmpty()) {
            throw new DefinitionAssemblyException("Action node missing beanName: " + node.nodeName());
        }
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext is not set. Please ensure SpringBeanActionNodeResolver is managed by Spring container.");
        }
        IActionNode<?, ?> raw = applicationContext.getBean(node.beanName(), IActionNode.class);
        return (IActionNode<Result, Context>) raw;
    }
}
