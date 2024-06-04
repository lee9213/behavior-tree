package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.node.IParallelNode;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * 并行节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:17
 */
@Log4j2
public final class ParallelNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements IParallelNode<Result, Context> {

    public ParallelNodeImpl(List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        this.childNodeList = childNodeList;
    }

    @Override
    public Result execute(Context context) {
        boolean isSuccess = true;
        for (BehaviorNodeWrapper<Result, Context> behaviorNodeWrapper : childNodeList) {
            context.setCurrentNode(behaviorNodeWrapper);
            Result nodeResult = behaviorNodeWrapper.getNode().execute(context);
            checkNodeResult(nodeResult);
            if (!nodeResult.isSuccess()) {
                isSuccess = false;
            }
            log.info("节点{}执行结果：{}", behaviorNodeWrapper.getNodeName(), nodeResult);
        }
        // 如果所有节点都执行成功，则返回成功
        return (Result) (isSuccess ? NodeResult.SUCCESS : NodeResult.FAILURE);
    }
}
