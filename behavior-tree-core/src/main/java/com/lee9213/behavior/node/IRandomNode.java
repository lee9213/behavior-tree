package com.lee9213.behavior.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;

/**
 * 随机节点，随机选择一个子节点来运行。
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:52
 */
public interface IRandomNode<Result extends NodeResult,Context extends BaseContext> extends IControlNode<Result, Context> {


}
