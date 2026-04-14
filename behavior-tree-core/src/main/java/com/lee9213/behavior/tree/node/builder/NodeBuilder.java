package com.lee9213.behavior.tree.node.builder;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.*;

/**
 * 节点建造者基类，用于构建行为树节点
 *
 * @param <Result> 节点结果类型
 * @param <Context> 上下文类型
 * @param <Self> 当前建造者类型
 */
public abstract class NodeBuilder<Result extends NodeResult, Context extends BaseContext, Self extends NodeBuilder<Result, Context, Self>> implements Endable<Result, Context> {
    protected final BehaviorTree.Builder<Result, Context> treeBuilder;
    protected final NodeBuilder<Result, Context, ?> parentBuilder;
    protected final String nodeName;

    protected NodeBuilder(BehaviorTree.Builder<Result, Context> treeBuilder, NodeBuilder<Result, Context, ?> parentBuilder, String nodeName) {
        this.treeBuilder = treeBuilder;
        this.parentBuilder = parentBuilder;
        this.nodeName = nodeName;
    }

    public abstract INode<Result, Context> buildNode();

    @Override
    public BehaviorTree<Result, Context> build() {
        INode<Result, Context> rootNode = buildNode();
        return new BehaviorTree<>(rootNode);
    }

    @Override
    public Endable<Result, Context> end() {
        if (parentBuilder == null) {
            return treeBuilder;
        }
        return parentBuilder;
    }

    @Override
    public SequenceBuilder<Result, Context> sequence(String nodeName) {
        SequenceBuilder<Result, Context> builder = new SequenceBuilder<>(treeBuilder, this, nodeName);
        addChild(builder);
        return builder;
    }

    @Override
    public SelectorBuilder<Result, Context> selector(String nodeName) {
        SelectorBuilder<Result, Context> builder = new SelectorBuilder<>(treeBuilder, this, nodeName);
        addChild(builder);
        return builder;
    }

    @Override
    public ParallelBuilder<Result, Context> parallel(String nodeName) {
        ParallelBuilder<Result, Context> builder = new ParallelBuilder<>(treeBuilder, this, nodeName);
        addChild(builder);
        return builder;
    }

    @Override
    public RandomBuilder<Result, Context> random(String nodeName) {
        RandomBuilder<Result, Context> builder = new RandomBuilder<>(treeBuilder, this, nodeName);
        addChild(builder);
        return builder;
    }

    @Override
    public StrategyBuilder<Result, Context> strategy(String nodeName) {
        StrategyBuilder<Result, Context> builder = new StrategyBuilder<>(treeBuilder, this, nodeName);
        addChild(builder);
        return builder;
    }

    @Override
    public Endable<Result, Context> action(String nodeName, ActionNodeFunction<Result, Context> action) {
        ActionBuilder<Result, Context> builder = new ActionBuilder<>(treeBuilder, this, nodeName, action);
        addChild(builder);
        return this;
    }

    protected abstract void addChild(NodeBuilder<Result, Context, ?> childBuilder);
}
