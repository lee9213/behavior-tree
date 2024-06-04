package com.lee9213.behavior.parser;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.enums.NodeType;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 09:58
 */
@Data
public class Node<Result extends NodeResult> {
    private String beanName;
    private String nodeName = beanName;
    private NodeType nodeType;
    private String container;

    /** 控制节点子节点（序列节点、选择节点、并行节点、随机节点） */
    private List<Node<Result>> children;

    /** 策略节点（条件节点） */
    private Node<Result> condition;
    /** 策略节点（具体策略节点） */
    private Map<String, Node<Result>> strategyMap;
}
