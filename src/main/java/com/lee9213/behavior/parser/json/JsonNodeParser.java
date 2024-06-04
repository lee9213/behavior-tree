package com.lee9213.behavior.parser.json;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.enums.NodeType;
import com.lee9213.behavior.parser.*;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 15:07
 */
public class JsonNodeParser<Result extends NodeResult, Context extends BaseContext> {

    public BehaviorNodeWrapper<Result, Context> parse(String json, Class<?> resultClazz) {
        Node<Result> node = JSONObject.parseObject(json, new TypeReference<Node<Result>>() {});
        return parse(node, resultClazz);
    }

    public BehaviorNodeWrapper<Result, Context> parse(Node<Result> node, Class<?> resultClazz) {
        AbstractNodeParser<Result, Context> nodeParser;
        if (NodeType.Sequence.equals(node.getNodeType())) {
            nodeParser = new SequenceNodeParser<>(node, resultClazz);
        }  else if (NodeType.Parallel.equals(node.getNodeType())) {
            nodeParser =  new ParallelNodeParser<>(node, resultClazz);
        }  else if (NodeType.Selector.equals(node.getNodeType())) {
            nodeParser =  new SelectorNodeParser<>(node, resultClazz);
        } else if (NodeType.Random.equals(node.getNodeType())) {
            nodeParser =  new RandomNodeParser<>(node, resultClazz);
        } else if (NodeType.Action.equals(node.getNodeType())) {
            nodeParser =  new ActionNodeParser<>(node, resultClazz);
        } else if (NodeType.Strategy.equals(node.getNodeType())) {
            nodeParser =  new StrategyNodeParser<>(node, resultClazz);
        } else {
            throw new IllegalArgumentException("Unsupported node type: " + node.getNodeType());
        }
        return nodeParser.parse();
    }
}
