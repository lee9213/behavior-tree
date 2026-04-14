package com.lee9213.behavior.tree.node.impl;

import com.google.common.collect.Lists;
import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.tree.exception.BehaviorNodeExecuteException;
import com.lee9213.behavior.tree.node.IControlNode;
import com.lee9213.behavior.tree.node.INode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:36
 */
public abstract class AbstractControlNode<Result extends NodeResult, Context extends BaseContext> implements IControlNode<Result, Context> {
    protected String nodeName;
    protected String stepTag;
    protected List<INode<Result, Context>> childNodeList;

    public AbstractControlNode(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public INode<Result, Context> addChild(INode<Result, Context> childNode) {
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

    /**
     * Structural child nodes for validation and tooling. Empty when there are no children.
     */
    public List<INode<Result, Context>> getChildNodes() {
        if (childNodeList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(childNodeList));
    }
    
    @Override
    public String getNodeName() {
        return nodeName;
    }
    
    @Override
    public String getStepTag() {
        return stepTag;
    }
    
    @Override
    public void setStepTag(String stepTag) {
        this.stepTag = stepTag;
    }
}
