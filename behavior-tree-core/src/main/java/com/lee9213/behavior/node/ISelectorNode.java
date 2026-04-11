package com.lee9213.behavior.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;

/**
 * 选择节点，依次执行所有子节点，若当前子节点返回成功，则中断后续节点运行，并把结果返回给父节点
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:11
 */
public interface ISelectorNode<Result extends NodeResult,Context extends BaseContext> extends IControlNode<Result, Context> {

}
