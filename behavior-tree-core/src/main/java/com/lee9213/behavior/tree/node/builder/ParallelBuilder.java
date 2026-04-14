package com.lee9213.behavior.tree.node.builder;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.NodeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 并行节点建造者
 *
 * @param <Result> 节点结果类型
 * @param <Context> 上下文类型
 */
public class ParallelBuilder<Result extends NodeResult, Context extends BaseContext> extends NodeBuilder<Result, Context, ParallelBuilder<Result, Context>> {
    private final List<NodeBuilder<Result, Context, ?>> childBuilders = new ArrayList<>();
    private Executor executor;

    public ParallelBuilder(BehaviorTree.Builder<Result, Context> treeBuilder, NodeBuilder<Result, Context, ?> parentBuilder, String nodeName) {
        super(treeBuilder, parentBuilder, nodeName);
    }

    public ParallelBuilder<Result, Context> withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    protected void addChild(NodeBuilder<Result, Context, ?> childBuilder) {
        childBuilders.add(childBuilder);
    }

    @Override
    public INode<Result, Context> buildNode() {
        List<INode<Result, Context>> childNodes = childBuilders.stream()
                .map(NodeBuilder::buildNode)
                .collect(Collectors.toList());
        if (executor != null) {
            return NodeFactory.createParallelNode(nodeName, childNodes, executor);
        }
        return NodeFactory.createParallelNode(nodeName, childNodes);
    }
}
