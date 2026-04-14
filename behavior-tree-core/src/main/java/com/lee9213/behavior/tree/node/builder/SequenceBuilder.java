package com.lee9213.behavior.tree.node.builder;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.NodeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 顺序节点建造者
 *
 * @param <Result> 节点结果类型
 * @param <Context> 上下文类型
 */
public class SequenceBuilder<Result extends NodeResult, Context extends BaseContext> extends NodeBuilder<Result, Context, SequenceBuilder<Result, Context>> {
    private final List<NodeBuilder<Result, Context, ?>> childBuilders = new ArrayList<>();

    public SequenceBuilder(BehaviorTree.Builder<Result, Context> treeBuilder, NodeBuilder<Result, Context, ?> parentBuilder, String nodeName) {
        super(treeBuilder, parentBuilder, nodeName);
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
        return NodeFactory.createSequenceNode(nodeName, childNodes);
    }
}
