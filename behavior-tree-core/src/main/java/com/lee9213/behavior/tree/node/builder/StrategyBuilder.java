package com.lee9213.behavior.tree.node.builder;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.NodeFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 策略节点建造者
 *
 * @param <Result> 节点结果类型
 * @param <Context> 上下文类型
 */
public class StrategyBuilder<Result extends NodeResult, Context extends BaseContext> extends NodeBuilder<Result, Context, StrategyBuilder<Result, Context>> {
    private NodeBuilder<Result, Context, ?> conditionBuilder;
    private final Map<Result, NodeBuilder<Result, Context, ?>> strategyBuilders = new LinkedHashMap<>();

    public StrategyBuilder(BehaviorTree.Builder<Result, Context> treeBuilder, NodeBuilder<Result, Context, ?> parentBuilder, String nodeName) {
        super(treeBuilder, parentBuilder, nodeName);
    }

    public StrategyBuilder<Result, Context> condition(NodeBuilder<Result, Context, ?> conditionBuilder) {
        this.conditionBuilder = conditionBuilder;
        return this;
    }

    public StrategyBuilder<Result, Context> addStrategy(Result key, NodeBuilder<Result, Context, ?> strategyBuilder) {
        strategyBuilders.put(key, strategyBuilder);
        return this;
    }

    @Override
    protected void addChild(NodeBuilder<Result, Context, ?> childBuilder) {
        // Strategy 节点的子节点通过 condition 和 addStrategy 方法添加
        throw new UnsupportedOperationException("Strategy node cannot have children");
    }

    @Override
    public INode<Result, Context> buildNode() {
        if (conditionBuilder == null) {
            throw new IllegalStateException("Strategy node requires condition");
        }
        INode<Result, Context> conditionNode = conditionBuilder.buildNode();
        Map<Result, INode<Result, Context>> strategyMap = strategyBuilders.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().buildNode()));
        return NodeFactory.createStrategyNode(nodeName, conditionNode, strategyMap);
    }
}
