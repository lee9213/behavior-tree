package com.lee9213.behavior;

import com.lee9213.behavior.node.IActionNode;
import org.springframework.stereotype.Service;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 14:22
 */
@Service("ActionNodeImpl1")
public class ActionNodeImpl1 implements IActionNode<TestNodeResult, TestContext> {
    @Override
    public TestNodeResult execute(TestContext context) {
        return TestNodeResult.B;
    }
}
