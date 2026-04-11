package com.lee9213.behavior.parser;

import com.google.common.base.Preconditions;
import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.enums.NodeType;
import com.lee9213.behavior.parser.json.JsonNodeParser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 11:01
 */
public class ParallelNodeParser<Result extends NodeResult, Context extends BaseContext> extends AbstractNodeParser<Result, Context> {

    public ParallelNodeParser(Node<Result> node, Class<?> resultClazz) {
        super(node, resultClazz);
        Preconditions.checkArgument(NodeType.Parallel.equals(node.getNodeType()), "Node type is not Parallel");
        Preconditions.checkArgument(node.getChildren() != null, "Node children is null");
    }

    @Override
    public BehaviorNodeWrapper<Result, Context> parse() {
        List<BehaviorNodeWrapper<Result, Context>> children = node.getChildren().stream().map(node -> new JsonNodeParser<Result, Context>().parse(node, resultClazz)).collect(Collectors.toList());
        return new BehaviorNodeWrapper<Result, Context>().buildParallelNode(node.getNodeName(), children);
    }
}
