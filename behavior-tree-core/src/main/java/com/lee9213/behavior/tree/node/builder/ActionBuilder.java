package com.lee9213.behavior.tree.node.builder;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.ActionNodeFunction;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.NodeFactory;
import com.lee9213.behavior.tree.retry.RetryPolicy;

/**
 * 动作节点建造者
 *
 * @param <Result> 节点结果类型
 * @param <Context> 上下文类型
 */
public class ActionBuilder<Result extends NodeResult, Context extends BaseContext> extends NodeBuilder<Result, Context, ActionBuilder<Result, Context>> {
    private final ActionNodeFunction<Result, Context> action;
    private boolean retryEnabled = false;
    private RetryPolicy retryPolicy;

    public ActionBuilder(BehaviorTree.Builder<Result, Context> treeBuilder, NodeBuilder<Result, Context, ?> parentBuilder, String nodeName, ActionNodeFunction<Result, Context> action) {
        super(treeBuilder, parentBuilder, nodeName);
        this.action = action;
    }

    public ActionBuilder<Result, Context> withRetry(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
        return this;
    }

    public ActionBuilder<Result, Context> withRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        this.retryEnabled = true;
        return this;
    }

    @Override
    protected void addChild(NodeBuilder<Result, Context, ?> childBuilder) {
        throw new UnsupportedOperationException("Action node cannot have children");
    }

    @Override
    public INode<Result, Context> buildNode() {
        return NodeFactory.createActionNode(nodeName, retryEnabled, retryPolicy, action);
    }
}
