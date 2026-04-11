package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.INode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentParallelNodeImplTest {

    @Test
    void runsChildrenOnExecutorAndJoins() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        AtomicInteger counter = new AtomicInteger();
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> leaf1 = leaf("a", counter);
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> leaf2 = leaf("b", counter);
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> parallel = new BehaviorNodeWrapper<NodeResult, FlowExecutionContext>()
                .buildConcurrentParallelNode("p", List.of(leaf1, leaf2), pool);

        FlowExecutionContext ctx = new FlowExecutionContext();
        NodeResult r = parallel.getNode().execute(ctx);
        pool.shutdown();
        assertEquals(NodeResult.SUCCESS, r);
        assertEquals(2, counter.get());
    }

    private static BehaviorNodeWrapper<NodeResult, FlowExecutionContext> leaf(String name, AtomicInteger counter) {
        return new BehaviorNodeWrapper<>(name, new CountingLeaf(counter));
    }

    private static final class CountingLeaf implements INode<NodeResult, FlowExecutionContext> {
        private final AtomicInteger counter;

        private CountingLeaf(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public NodeResult execute(FlowExecutionContext context) {
            counter.incrementAndGet();
            return NodeResult.SUCCESS;
        }
    }
}
