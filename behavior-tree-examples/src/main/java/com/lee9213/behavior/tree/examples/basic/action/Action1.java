package com.lee9213.behavior.tree.examples.basic.action;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.IActionNode;
import com.lee9213.behavior.tree.node.impl.AbstractActionNode;

/**
 * 示例动作 1
 */
public class Action1 extends AbstractActionNode<NodeResult, BaseContext> {

    public Action1() {
        super("action1", false, null);
    }

    @Override
    protected NodeResult doExecute(BaseContext context) {
        System.out.println("执行 Action1");
        return NodeResult.SUCCESS;
    }
}
