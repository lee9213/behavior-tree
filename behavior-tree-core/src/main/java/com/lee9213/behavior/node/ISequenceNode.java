package com.lee9213.behavior.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;

/**
 * 顺序节点，依次执行所有子节点，若当前子节点返回成功，则继续执行下一个子节点；若子当前节点返回失败，则中断后续子节点的执行，并把结果返回给父节点。
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:11
 */
public interface ISequenceNode<Result extends NodeResult,Context extends BaseContext> extends IControlNode<Result, Context> {

}
