package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.node.ISelectorNode;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * 选择节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:11
 */
@Log4j2
public final class SelectorNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements ISelectorNode<Result, Context> {

    public SelectorNodeImpl(List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        this.childNodeList = childNodeList;
    }

    @Override
    public Result execute(Context context) {
        for (BehaviorNodeWrapper<Result, Context> behaviorNodeWrapper : childNodeList) {
            context.setCurrentNode(behaviorNodeWrapper);
            Result nodeResult = behaviorNodeWrapper.getNode().execute(context);
            checkNodeResult(nodeResult);
            // 如果任何一个节点执行成功，则返回成功
            if (nodeResult.isSuccess()) {
                log.info("节点{}执行结果：{}", behaviorNodeWrapper.getNodeName(), nodeResult);
                return nodeResult;
            }
            log.info("节点{}执行结果：{}", behaviorNodeWrapper.getNodeName(), nodeResult);
        }
        // 如果所有节点都执行失败，则返回失败
        return (Result) NodeResult.FAILURE;
    }
}
