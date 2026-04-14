package com.lee9213.behavior.tree.node.impl;

import com.lee9213.behavior.tree.TestContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.NodeFactory;
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
        INode<NodeResult, TestContext> leaf1 = leaf("a", counter);
        INode<NodeResult, TestContext> leaf2 = leaf("b", counter);
        INode<NodeResult, TestContext> parallel = NodeFactory.createParallelNode("p", List.of(leaf1, leaf2), pool);

        TestContext ctx = new TestContext();
        NodeResult r = parallel.execute(ctx);
        pool.shutdown();
        assertEquals(NodeResult.SUCCESS, r);
        assertEquals(2, counter.get());
    }

    private static INode<NodeResult, TestContext> leaf(String name, AtomicInteger counter) {
        return NodeFactory.createActionNode(name, false, null, context -> {
            counter.incrementAndGet();
            return NodeResult.SUCCESS;
        });
    }

    private static final class CountingLeaf implements INode<NodeResult, TestContext> {
        private final AtomicInteger counter;
        private final String nodeName;

        private CountingLeaf(String nodeName, AtomicInteger counter) {
            this.nodeName = nodeName;
            this.counter = counter;
        }

        @Override
        public NodeResult execute(TestContext context) {
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
