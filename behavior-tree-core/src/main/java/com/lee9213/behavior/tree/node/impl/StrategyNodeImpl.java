package com.lee9213.behavior.tree.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.exception.BehaviorNodeExecuteException;

import com.lee9213.behavior.tree.node.INode;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * 策略节点实现
 *
 * @author lee9213@163.com
 * @date 2024/6/4 09:44
 */
@Log4j2
public final class StrategyNodeImpl<Result extends NodeResult,Context extends BaseContext> implements INode<Result, Context> {

    private INode<Result, Context> conditionNode;
    private Map<Result, INode<Result, Context>> strategyMap;
    private String nodeName;
    private String stepTag;

    public StrategyNodeImpl(String nodeName, INode<Result, Context> conditionNode, Map<Result, INode<Result, Context>> strategyMap) {
        this.nodeName = nodeName;
        this.conditionNode = conditionNode;
        this.strategyMap = strategyMap;
    }

    public INode<Result, Context> getConditionNode() {
        return conditionNode;
    }

    public Map<Result, INode<Result, Context>> getStrategyMap() {
        return strategyMap;
    }

    @Override
    public Result execute(Context context) {
        context.setCurrentNode(conditionNode);
        Result nodeResult = conditionNode.execute(context);
        log.info("节点{}执行结果：{}。", conditionNode.getNodeName(), nodeResult);
        checkNodeResult(nodeResult);
        INode<Result, Context> node = strategyMap.get(nodeResult);
        if (node == null) {
            throw new BehaviorNodeExecuteException("策略映射中未找到与结果 " + nodeResult + " 匹配的策略");
        }
        context.setCurrentNode(node);
        nodeResult = node.execute(context);
        log.info("节点{}执行结果：{}。", node.getNodeName(), nodeResult);
        checkNodeResult(nodeResult);
        return nodeResult;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public String getStepTag() {
        return stepTag;
    }

    @Override
    public void setStepTag(String stepTag) {
        this.stepTag = stepTag;
    }

    public void checkNodeResult(Result nodeResult) {
        if (nodeResult == null) {
            throw new BehaviorNodeExecuteException("节点执行结果为空");
        }
    }
}
