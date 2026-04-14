package com.lee9213.behavior.tree;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.tree.node.INode;
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
}
