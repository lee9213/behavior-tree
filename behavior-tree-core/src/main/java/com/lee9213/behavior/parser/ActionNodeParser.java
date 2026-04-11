package com.lee9213.behavior.parser;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.enums.NodeType;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 10:16
 */
public class ActionNodeParser<Result extends NodeResult, Context extends BaseContext> extends AbstractNodeParser<Result, Context> {

    public ActionNodeParser(Node<Result> node, Class<?> resultClazz) {
        super(node, resultClazz);
        Preconditions.checkArgument(NodeType.Action.equals(node.getNodeType()), "Node type is not action");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(node.getBeanName()), "beanName is null");
    }
    @Override
    public BehaviorNodeWrapper<Result, Context> parse() {
        return new BehaviorNodeWrapper<>(node.getNodeName(), getActionNode(node.getContainer(), node.getBeanName()));
    }
}
