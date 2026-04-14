package com.lee9213.behavior.definition.ir;

import com.lee9213.behavior.tree.enums.NodeType;

import java.util.List;
import java.util.Map;

/**
 * 行为树定义的不可变中间表示（单棵树由根节点递归表达）。
 */
public record BehaviorDefinitionNode(
        String nodeName,
        NodeType nodeType,
        String beanName,
        String container,
        List<BehaviorDefinitionNode> children,
        BehaviorDefinitionNode condition,
        Map<String, BehaviorDefinitionNode> strategyMap
) {
    public BehaviorDefinitionNode {
        children = children == null ? List.of() : List.copyOf(children);
        strategyMap = strategyMap == null ? Map.of() : Map.copyOf(strategyMap);
    }
}
