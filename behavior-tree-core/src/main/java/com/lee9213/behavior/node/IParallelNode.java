package com.lee9213.behavior.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;

/**
 * 并行节点接口，依次执行所有子节点，无论失败与否，都会把所有子节点执行一遍
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:17
 */
public interface IParallelNode<Result extends NodeResult,Context extends BaseContext> extends IControlNode<Result, Context> {

}
