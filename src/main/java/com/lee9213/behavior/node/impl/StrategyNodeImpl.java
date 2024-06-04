package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.exception.BehaviorNodeExecuteException;
import com.lee9213.behavior.node.IDecoratorNode;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * 策略节点实现
 *
 * @author lee9213@163.com
 * @date 2024/6/4 09:44
 */
@Log4j2
public final class StrategyNodeImpl<Result extends NodeResult,Context extends BaseContext> implements IDecoratorNode<Result, Context> {

    private BehaviorNodeWrapper<Result, Context> conditionNode;
    private Map<Result, BehaviorNodeWrapper<Result, Context>> strategyMap;

    public StrategyNodeImpl(BehaviorNodeWrapper<Result, Context> conditionNode, Map<Result, BehaviorNodeWrapper<Result, Context>> strategyMap) {
        this.conditionNode = conditionNode;
        this.strategyMap = strategyMap;
    }

    @Override
    public Result execute(Context context) {
        context.setCurrentNode(conditionNode);
        Result nodeResult = conditionNode.getNode().execute(context);
        log.info("节点{}执行结果：{}。", conditionNode.getNodeName(), nodeResult);
        checkNodeResult(nodeResult);
        BehaviorNodeWrapper<Result, Context> behaviorNodeWrapper = strategyMap.get(nodeResult);
        context.setCurrentNode(behaviorNodeWrapper);
        nodeResult = behaviorNodeWrapper.getNode().execute(context);
        log.info("节点{}执行结果：{}。", behaviorNodeWrapper.getNodeName(), nodeResult);
        checkNodeResult(nodeResult);
        return nodeResult;
    }

    public void checkNodeResult(Result nodeResult) {
        if (nodeResult == null) {
            throw new BehaviorNodeExecuteException("节点执行结果为空");
        }
    }
}
