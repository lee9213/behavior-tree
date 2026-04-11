package com.lee9213.behavior.parser;

import com.google.common.base.Preconditions;
import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.node.IActionNode;
import com.lee9213.behavior.parser.spring.SpringNodeUtil;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 10:59
 */
public abstract class AbstractNodeParser<Result extends NodeResult, Context extends BaseContext> {

    protected Node<Result> node;
    protected Class<?> resultClazz;

    public AbstractNodeParser(Node<Result> node, Class<?> resultClazz) {
        Preconditions.checkArgument(node != null, "node is null");
        Preconditions.checkArgument(resultClazz != null, "resultClazz is null");
        this.node = node;
        this.resultClazz = resultClazz;
    }

    public abstract BehaviorNodeWrapper<Result, Context> parse();

    protected IActionNode<Result, Context> getActionNode(String container, String beanName) {
        if ("spring".equals(container)) {
            return SpringNodeUtil.getBehaviorNode(beanName);
        }
        try {
            Object o = Class.forName(beanName).newInstance();
            if (o instanceof IActionNode) {
                return (IActionNode<Result, Context>) o;
            }
            throw new IllegalArgumentException(beanName + " is not a IActionNode");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Result getResult(String resultCode) {
        try {
            return (Result) resultClazz.getConstructor(String.class).newInstance(resultCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
