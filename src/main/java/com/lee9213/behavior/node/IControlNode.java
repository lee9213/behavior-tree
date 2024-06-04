package com.lee9213.behavior.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;

/**
 * 控制节点接口，一般为中间节点，用于控制行为树的执行流程，决定了其子节点是以顺序、并行、随机或其它方式执行。
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:13
 */
public interface IControlNode<Result extends NodeResult,Context extends BaseContext> extends INode<Result, Context> {

    /**
     * 添加子节点
     *
     * @param childNode 子节点
     * @return 当前节点
     */
    BehaviorNodeWrapper<Result, Context> addChild(BehaviorNodeWrapper<Result, Context> childNode);
}
