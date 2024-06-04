package com.lee9213.behavior.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;

/**
 * 行为节点接口，具体执行某个行为的节点都需要实现该接口
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
public interface IActionNode<Result extends NodeResult,Context extends BaseContext> extends INode<Result, Context> {

}
