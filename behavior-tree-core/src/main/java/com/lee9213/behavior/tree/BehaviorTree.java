package com.lee9213.behavior.tree;

import com.lee9213.behavior.tree.node.*;
import com.lee9213.behavior.tree.node.builder.*;
import lombok.Data;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:12
 */
@Data
public class BehaviorTree<Result extends NodeResult, Context extends BaseContext> {
    private INode<Result, Context> rootNode;
    public BehaviorTree() { }
    public BehaviorTree(INode<Result, Context> rootNode) {
        this.rootNode = rootNode;
    }

    public Result execute(Context context) {
        context.setCurrentNode(rootNode);
        return rootNode.execute(context);
    }

    public static <Result extends NodeResult, Context extends BaseContext> Builder<Result, Context> builder() {
        return new Builder<>();
    }

    public static class Builder<Result extends NodeResult, Context extends BaseContext> implements Endable<Result, Context> {
        private NodeBuilder<Result, Context, ?> rootBuilder;

        @Override
        public SequenceBuilder<Result, Context> sequence(String nodeName) {
            SequenceBuilder<Result, Context> builder = new SequenceBuilder<>(this, null, nodeName);
            this.rootBuilder = builder;
            return builder;
        }

        @Override
        public SelectorBuilder<Result, Context> selector(String nodeName) {
            SelectorBuilder<Result, Context> builder = new SelectorBuilder<>(this, null, nodeName);
            this.rootBuilder = builder;
            return builder;
        }

        @Override
        public ParallelBuilder<Result, Context> parallel(String nodeName) {
            ParallelBuilder<Result, Context> builder = new ParallelBuilder<>(this, null, nodeName);
            this.rootBuilder = builder;
            return builder;
        }

        @Override
        public RandomBuilder<Result, Context> random(String nodeName) {
            RandomBuilder<Result, Context> builder = new RandomBuilder<>(this, null, nodeName);
            this.rootBuilder = builder;
            return builder;
        }

        @Override
        public StrategyBuilder<Result, Context> strategy(String nodeName) {
            StrategyBuilder<Result, Context> builder = new StrategyBuilder<>(this, null, nodeName);
            this.rootBuilder = builder;
            return builder;
        }

        @Override
        public Endable<Result, Context> action(String nodeName, ActionNodeFunction<Result, Context> action) {
            ActionBuilder<Result, Context> builder = new ActionBuilder<>(this, null, nodeName, action);
            this.rootBuilder = builder;
            return this;
        }

        @Override
        public Endable<Result, Context> end() {
            return this;
        }

        @Override
        public BehaviorTree<Result, Context> build() {
            if (rootBuilder == null) {
                throw new IllegalStateException("Root node not defined");
            }
            INode<Result, Context> rootNode = rootBuilder.buildNode();
            return new BehaviorTree<>(rootNode);
        }
    }
}
