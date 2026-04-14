package com.lee9213.behavior.tree.definition.resolve;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.node.IActionNode;

/**
 * 将 IR 中的动作引用解析为 {@link IActionNode} 实例。
 */
public interface ActionNodeResolver {

    <Result extends NodeResult, Context extends BaseContext>
    IActionNode<Result, Context> resolveAction(BehaviorDefinitionNode node, Class<Result> resultClass);
}
