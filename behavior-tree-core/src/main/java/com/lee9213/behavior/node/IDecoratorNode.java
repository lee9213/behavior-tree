package com.lee9213.behavior.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;

/**
 * 装饰节点接口
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
public interface IDecoratorNode<Result extends NodeResult,Context extends BaseContext> extends INode<Result, Context> {


}
