package com.lee9213.behavior;

import com.lee9213.behavior.exception.BehaviorNodeNotFoundException;
import com.lee9213.behavior.node.INode;
import com.lee9213.behavior.node.impl.*;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:15
 */
@Getter
public class BehaviorNodeWrapper<Result extends NodeResult, Context extends BaseContext> {
    private String nodeName;
    private INode<Result, Context> node;

    public BehaviorNodeWrapper() { }
    public BehaviorNodeWrapper(String nodeName, INode<Result, Context> node) {
        this.nodeName = nodeName;
        this.node = node;
    }
    public BehaviorNodeWrapper<Result, Context> buildSequenceNode(String nodeName, List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        return new BehaviorNodeWrapper<>(nodeName, new SequenceNodeImpl<>(childNodeList));
    }
    public BehaviorNodeWrapper<Result, Context> buildSelectorNode(String nodeName, List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        return new BehaviorNodeWrapper<>(nodeName, new SelectorNodeImpl<>(childNodeList));
    }

    public BehaviorNodeWrapper<Result, Context> buildParallelNode(String nodeName, List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        return new BehaviorNodeWrapper<>(nodeName, new ParallelNodeImpl<>(childNodeList));
    }

    public BehaviorNodeWrapper<Result, Context> buildRandomNode(String nodeName, List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        return new BehaviorNodeWrapper<>(nodeName, new RandomNodeImpl<>(childNodeList));
    }

    public BehaviorNodeWrapper<Result, Context> buildStrategyNode(String nodeName, BehaviorNodeWrapper<Result, Context> conditionNode, Map<Result, BehaviorNodeWrapper<Result, Context>> strategyMap) {
        return new BehaviorNodeWrapper<>(nodeName, new StrategyNodeImpl(conditionNode, strategyMap));
    }

    public BehaviorNodeWrapper<Result, Context> buildSuccessNode() {
        return new BehaviorNodeWrapper<>("DefaultSuccessNode", new SuccessActionNodeImpl<>());
    }

    public BehaviorNodeWrapper<Result, Context> buildFailureNode() {
        return new BehaviorNodeWrapper<>("DefaultFailureNode", new FailureActionNodeImpl<>());
    }

    public INode<Result, Context> getNode() {
        if (node == null) {
            throw new BehaviorNodeNotFoundException("节点不存在");
        }
        return node;
    }
}
