package com.lee9213.behavior;

import lombok.Data;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:12
 */
@Data
public class BehaviorTree<Result extends NodeResult, Context extends BaseContext> {
    private BehaviorNodeWrapper<Result, Context> rootNode;
    public BehaviorTree() { }
    public BehaviorTree(BehaviorNodeWrapper<Result, Context> rootNode) {
        this.rootNode = rootNode;
    }

    public Result execute(Context context) {
        context.setCurrentNode(rootNode);
        return rootNode.getNode().execute(context);
    }
}
