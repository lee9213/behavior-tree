package com.lee9213.behavior.tree.definition.assemble;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.definition.exception.DefinitionAssemblyException;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.definition.resolve.ActionNodeResolver;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.NodeFactory;
import com.lee9213.behavior.tree.node.impl.ParallelNodeImpl;
import com.lee9213.behavior.tree.node.impl.RandomNodeImpl;
import com.lee9213.behavior.tree.node.impl.SelectorNodeImpl;
import com.lee9213.behavior.tree.node.impl.SequenceNodeImpl;
import com.lee9213.behavior.tree.node.impl.StrategyNodeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lee9213.behavior.tree.node.NodeFactory.*;

/**
 * 将行为树定义的中间表示（IR）转换为运行时 {@link INode}。
 * <p>
 * 注意：Parallel 节点首版不解析 executor，固定为 {@code null}。
 * <p>
 * 上下文泛型统一为 {@link BaseContext}（定义加载阶段不携带具体 {@code Context} 类型形参）。
 */
public final class DefinitionAssembler {

    private DefinitionAssembler() {
    }

    /**
     * 将行为树定义节点组装为运行时节点
     *
     * @param node        行为树定义节点
     * @param resultClass 结果类型
     * @param resolver    动作节点解析器
     * @param <R>         结果类型泛型
     * @return 运行时节点
     */
    @SuppressWarnings("unchecked")
    public static <R extends NodeResult> INode<R, BaseContext> assemble(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        if (node.nodeType() == null) {
            throw new DefinitionAssemblyException("nodeType is required: " + node.nodeName());
        }
        return switch (node.nodeType()) {
            case Action -> resolver.resolveAction(node, resultClass);
            case Sequence -> createSequenceNode(node.nodeName(), assembleChildren(node, resultClass, resolver));
            case Selector -> createSelectorNode(node.nodeName(), assembleChildren(node, resultClass, resolver));
            case Parallel -> createParallelNode(node.nodeName(), assembleChildren(node, resultClass, resolver), null);
            case Random -> createRandomNode(node.nodeName(), assembleChildren(node, resultClass, resolver));
            case Strategy -> assembleStrategy(node, resultClass, resolver);
        };
    }

    /**
     * 组装策略节点
     */
    private static <R extends NodeResult> INode<R, BaseContext> assembleStrategy(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        if (node.condition() == null) {
            throw new DefinitionAssemblyException("Strategy node requires condition: " + node.nodeName());
        }

        INode<R, BaseContext> condition = assemble(node.condition(), resultClass, resolver);
        Map<R, INode<R, BaseContext>> strategyMap = assembleStrategyMap(node, resultClass, resolver);

        return new StrategyNodeImpl<>(node.nodeName(), condition, strategyMap);
    }

    /**
     * 组装子节点列表
     */
    private static <R extends NodeResult> List<INode<R, BaseContext>> assembleChildren(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        return node.children().stream()
                .map(child -> assemble(child, resultClass, resolver))
                .collect(Collectors.toList());
    }

    /**
     * 组装策略映射
     */
    private static <R extends NodeResult> Map<R, INode<R, BaseContext>> assembleStrategyMap(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        Map<R, INode<R, BaseContext>> strategyMap = new LinkedHashMap<>();

        for (Map.Entry<String, BehaviorDefinitionNode> entry : node.strategyMap().entrySet()) {
            R key = resolveStrategyKey(resultClass, entry.getKey());
            INode<R, BaseContext> strategyNode = assemble(entry.getValue(), resultClass, resolver);
            strategyMap.put(key, strategyNode);
        }

        return strategyMap;
    }

    /**
     * 解析策略键
     */
    private static <R extends NodeResult> R resolveStrategyKey(Class<R> resultClass, String key) {
        // 尝试通过字段名直接查找
        try {
            Field field = resultClass.getField(key);
            if (Modifier.isStatic(field.getModifiers()) && NodeResult.class.isAssignableFrom(field.getType())) {
                @SuppressWarnings("unchecked")
                R result = (R) field.get(null);
                return result;
            }
        } catch (NoSuchFieldException ignored) {
            // 字段不存在，继续尝试通过代码查找
        } catch (IllegalAccessException e) {
            throw new DefinitionAssemblyException("Cannot read strategy key field: " + key, e);
        }

        // 尝试通过代码查找
        for (Field field : resultClass.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !NodeResult.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                NodeResult nodeResult = (NodeResult) field.get(null);
                if (key.equals(nodeResult.getCode())) {
                    @SuppressWarnings("unchecked")
                    R result = (R) nodeResult;
                    return result;
                }
            } catch (IllegalAccessException e) {
                throw new DefinitionAssemblyException("Cannot read strategy constant: " + field.getName(), e);
            }
        }

        throw new DefinitionAssemblyException("Unknown strategy key for " + resultClass.getName() + ": " + key);
    }
}
