package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.INode;
import com.lee9213.behavior.node.NodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelNodeImplConcurrentTest {

    @Test
    void executor非空时在线程池上并发执行子节点并汇合() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        AtomicInteger counter = new AtomicInteger();
        INode<NodeResult, FlowExecutionContext> leaf1 = leaf("a", counter);
        INode<NodeResult, FlowExecutionContext> leaf2 = leaf("b", counter);
        INode<NodeResult, FlowExecutionContext> parallel = NodeFactory.createParallelNode("p", List.of(leaf1, leaf2), pool);

        FlowExecutionContext ctx = new FlowExecutionContext();
        NodeResult r = parallel.execute(ctx);
        pool.shutdown();
        assertEquals(NodeResult.SUCCESS, r);
        assertEquals(2, counter.get());
    }

    private static INode<NodeResult, FlowExecutionContext> leaf(String name, AtomicInteger counter) {
        return NodeFactory.createActionNode(name, false, null, context -> {
            counter.incrementAndGet();
            return NodeResult.SUCCESS;
        });
    }

    private static final class CountingLeaf implements INode<NodeResult, FlowExecutionContext> {
        private final AtomicInteger counter;
        private final String nodeName;

        private CountingLeaf(String nodeName, AtomicInteger counter) {
            this.nodeName = nodeName;
            this.counter = counter;
        }

        @Override
        public NodeResult execute(FlowExecutionContext context) {
            counter.incrementAndGet();
            return NodeResult.SUCCESS;
        }

        @Override
        public String getNodeName() {
            return nodeName;
        }

        @Override
        public String getStepTag() {
            return null;
        }

        @Override
        public void setStepTag(String stepTag) {
        }
    }
}
