package com.lee9213.behavior;

import com.lee9213.behavior.node.IActionNode;
import org.springframework.stereotype.Service;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 14:22
 */
@Service("ActionNodeImpl0")
public class ActionNodeImpl0 implements IActionNode<NodeResult, TestContext> {
    @Override
    public NodeResult execute(TestContext context) {
        return NodeResult.SUCCESS;
    }
}
