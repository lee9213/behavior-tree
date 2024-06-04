package com.lee9213.behavior.node.impl;

import com.google.common.collect.Lists;
import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.exception.BehaviorNodeExecuteException;
import com.lee9213.behavior.node.IControlNode;

import java.util.List;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:36
 */
public abstract class AbstractControlNode<Result extends NodeResult,Context extends BaseContext> implements IControlNode<Result, Context> {
    protected List<BehaviorNodeWrapper<Result, Context>> childNodeList;

    @Override
    public BehaviorNodeWrapper<Result, Context> addChild(BehaviorNodeWrapper<Result, Context> childNode) {
        if (childNodeList == null) {
            childNodeList = Lists.newArrayList();
        }
        childNodeList.add(childNode);
        return null;
    }

    public void checkNodeResult(Result nodeResult) {
        if (nodeResult == null) {
            throw new BehaviorNodeExecuteException("节点执行结果为空");
        }
    }
}
