package com.lee9213.behavior.definition.assemble;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.definition.exception.DefinitionAssemblyException;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.definition.resolve.ActionNodeResolver;
import com.lee9213.behavior.enums.NodeType;
import com.lee9213.behavior.node.IActionNode;
import com.lee9213.behavior.node.impl.ParallelNodeImpl;
import com.lee9213.behavior.node.impl.RandomNodeImpl;
import com.lee9213.behavior.node.impl.SelectorNodeImpl;
import com.lee9213.behavior.node.impl.SequenceNodeImpl;
import com.lee9213.behavior.node.impl.StrategyNodeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IR → 运行时 {@link BehaviorNodeWrapper}。Parallel 首版不解析 executor，固定为 {@code null}。
 * <p>
 * 上下文泛型统一为 {@link BaseContext}（定义加载阶段不携带具体 {@code Context} 类型形参）。
 */
public final class DefinitionAssembler {

    private DefinitionAssembler() {
    }

    @SuppressWarnings("unchecked")
    public static <R extends NodeResult> BehaviorNodeWrapper<R, BaseContext> assemble(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        NodeType type = node.nodeType();
        if (type == null) {
            throw new DefinitionAssemblyException("nodeType is required: " + node.nodeName());
        }
        return switch (type) {
            case Action -> assembleAction(node, resultClass, resolver);
            case Sequence -> assembleSequence(node, resultClass, resolver);
            case Selector -> assembleSelector(node, resultClass, resolver);
            case Parallel -> assembleParallel(node, resultClass, resolver);
            case Random -> assembleRandom(node, resultClass, resolver);
            case Strategy -> assembleStrategy(node, resultClass, resolver);
        };
    }

    private static <R extends NodeResult> BehaviorNodeWrapper<R, BaseContext> assembleAction(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        IActionNode<R, BaseContext> action =
                (IActionNode<R, BaseContext>) resolver.resolveAction(node, resultClass);
        return new BehaviorNodeWrapper<>(node.nodeName(), action);
    }

    private static <R extends NodeResult> BehaviorNodeWrapper<R, BaseContext> assembleSequence(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        List<BehaviorNodeWrapper<R, BaseContext>> kids = mapChildren(node, resultClass, resolver);
        return new BehaviorNodeWrapper<>(node.nodeName(), new SequenceNodeImpl<>(kids));
    }

    private static <R extends NodeResult> BehaviorNodeWrapper<R, BaseContext> assembleSelector(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        List<BehaviorNodeWrapper<R, BaseContext>> kids = mapChildren(node, resultClass, resolver);
        return new BehaviorNodeWrapper<>(node.nodeName(), new SelectorNodeImpl<>(kids));
    }

    private static <R extends NodeResult> BehaviorNodeWrapper<R, BaseContext> assembleParallel(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        List<BehaviorNodeWrapper<R, BaseContext>> kids = mapChildren(node, resultClass, resolver);
        return new BehaviorNodeWrapper<>(node.nodeName(), new ParallelNodeImpl<>(kids, null));
    }

    private static <R extends NodeResult> BehaviorNodeWrapper<R, BaseContext> assembleRandom(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        List<BehaviorNodeWrapper<R, BaseContext>> kids = mapChildren(node, resultClass, resolver);
        return new BehaviorNodeWrapper<>(node.nodeName(), new RandomNodeImpl<>(kids));
    }

    private static <R extends NodeResult> BehaviorNodeWrapper<R, BaseContext> assembleStrategy(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        if (node.condition() == null) {
            throw new DefinitionAssemblyException("Strategy requires condition: " + node.nodeName());
        }
        BehaviorNodeWrapper<R, BaseContext> condition = assemble(node.condition(), resultClass, resolver);
        Map<R, BehaviorNodeWrapper<R, BaseContext>> map = new LinkedHashMap<>();
        for (Map.Entry<String, BehaviorDefinitionNode> e : node.strategyMap().entrySet()) {
            R key = resolveStrategyKey(resultClass, e.getKey());
            map.put(key, assemble(e.getValue(), resultClass, resolver));
        }
        return new BehaviorNodeWrapper<>(node.nodeName(), new StrategyNodeImpl<>(condition, map));
    }

    private static <R extends NodeResult> List<BehaviorNodeWrapper<R, BaseContext>> mapChildren(
            BehaviorDefinitionNode node,
            Class<R> resultClass,
            ActionNodeResolver resolver) {
        return node.children().stream()
                .map(c -> assemble(c, resultClass, resolver))
                .collect(Collectors.toList());
    }

    private static <R extends NodeResult> R resolveStrategyKey(Class<R> clazz, String key) {
        try {
            Field f = clazz.getField(key);
            if (Modifier.isStatic(f.getModifiers()) && NodeResult.class.isAssignableFrom(f.getType())) {
                @SuppressWarnings("unchecked")
                R r = (R) f.get(null);
                return r;
            }
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException e) {
            throw new DefinitionAssemblyException("Cannot read strategy key field: " + key, e);
        }
        for (Field f : clazz.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || !NodeResult.class.isAssignableFrom(f.getType())) {
                continue;
            }
            try {
                NodeResult nr = (NodeResult) f.get(null);
                if (key.equals(nr.getCode())) {
                    @SuppressWarnings("unchecked")
                    R r = (R) nr;
                    return r;
                }
            } catch (IllegalAccessException e) {
                throw new DefinitionAssemblyException("Cannot read strategy constant: " + f.getName(), e);
            }
        }
        throw new DefinitionAssemblyException("Unknown strategy key for " + clazz.getName() + ": " + key);
    }
}
