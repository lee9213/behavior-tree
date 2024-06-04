package com.lee9213.behavior.parser;

import com.google.common.base.Preconditions;
import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.enums.NodeType;
import com.lee9213.behavior.parser.json.JsonNodeParser;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 10:16
 */
public class StrategyNodeParser<Result extends NodeResult, Context extends BaseContext> extends AbstractNodeParser<Result, Context> {

    public StrategyNodeParser(Node<Result> node, Class<?> resultClazz) {
        super(node, resultClazz);
        Preconditions.checkArgument(NodeType.Strategy.equals(node.getNodeType()), "Node type is not Strategy");
        Preconditions.checkArgument(node.getStrategyMap() != null, "Node strategyMap is null");
    }

    @Override
    public BehaviorNodeWrapper<Result, Context> parse() {
        BehaviorNodeWrapper<Result, Context> conditionNode = new JsonNodeParser<Result, Context>().parse(node.getCondition(), resultClazz);
        Map<Result, BehaviorNodeWrapper<Result, Context>> strategyMap = node.getStrategyMap().entrySet().stream().collect(Collectors.toMap(entry -> this.getResult(entry.getKey()), entry -> new JsonNodeParser<Result, Context>().parse(entry.getValue(), resultClazz)));
        return new BehaviorNodeWrapper<Result, Context>().buildStrategyNode(node.getNodeName(), conditionNode, strategyMap);
    }
}
