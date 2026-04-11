package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.node.ISequenceNode;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * 顺序节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
@Log4j2
public final class SequenceNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements ISequenceNode<Result, Context> {
    public SequenceNodeImpl(List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        this.childNodeList = childNodeList;
    }
    @Override
    public Result execute(Context context) {
        for (BehaviorNodeWrapper<Result, Context> behaviorNodeWrapper : childNodeList) {
            context.setCurrentNode(behaviorNodeWrapper);
            Result nodeResult = behaviorNodeWrapper.getNode().execute(context);
            checkNodeResult(nodeResult);
            if (nodeResult.isSuccess()) {
                log.info("节点{}执行结果：{}", behaviorNodeWrapper.getNodeName(), nodeResult);
                continue;
            }
            log.info("节点{}执行结果：{}，流程终止。", behaviorNodeWrapper.getNodeName(), nodeResult);
            // 如果有一个节点执行结果非成功，则直接返回执行结果
            return nodeResult;
        }
        // 如果所有节点都执行成功，则返回成功
        return (Result) NodeResult.SUCCESS;
    }
}
