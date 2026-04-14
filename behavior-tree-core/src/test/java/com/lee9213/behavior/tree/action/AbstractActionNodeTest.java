package com.lee9213.behavior.tree.action;

import com.lee9213.behavior.tree.TestContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.impl.AbstractActionNode;
import com.lee9213.behavior.tree.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractActionNodeTest {

    @Test
    void retryDisabled_仅执行一次() {
        AtomicInteger attempts = new AtomicInteger();
        RetryPolicy policy = new RetryPolicy(5, 1L, 2.0, 0.0);
        AbstractActionNode<NodeResult, TestContext> node = new AbstractActionNode<NodeResult, TestContext>("test-node", false, policy) {
            @Override
            protected NodeResult doExecute(TestContext context) throws Exception {
                attempts.incrementAndGet();
                throw new IllegalStateException("fail");
            }
        };
        assertThrows(IllegalStateException.class, () -> node.execute(new TestContext()));
        assertEquals(1, attempts.get());
    }

    @Test
    void retryEnabled_失败时按策略多次尝试() {
        AtomicInteger attempts = new AtomicInteger();
        RetryPolicy policy = new RetryPolicy(3, 1L, 2.0, 0.0);
        AbstractActionNode<NodeResult, TestContext> node = new AbstractActionNode<NodeResult, TestContext>("test-node", true, policy) {
            @Override
            protected NodeResult doExecute(TestContext context) throws Exception {
                attempts.incrementAndGet();
                throw new IllegalStateException("fail");
            }
        };
        assertThrows(Exception.class, () -> node.execute(new TestContext()));
        assertEquals(3, attempts.get());
    }
}
