package com.lee9213.behavior.tree.examples.basic.action;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.IActionNode;
import com.lee9213.behavior.tree.node.impl.AbstractActionNode;

/**
 * 示例动作 2
 */
public class Action2 extends AbstractActionNode<NodeResult, BaseContext> {

    public Action2() {
        super("action2", false, null);
    }

    @Override
    protected NodeResult doExecute(BaseContext context) {
        System.out.println("执行 Action2");
        return NodeResult.SUCCESS;
    }
}
