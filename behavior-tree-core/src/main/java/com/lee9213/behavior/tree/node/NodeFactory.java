package com.lee9213.behavior.tree.node;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.impl.*;
import com.lee9213.behavior.tree.retry.RetryPolicy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 节点工厂类，用于创建各种行为树节点
 *
 * @author lee9213@163.com
 * @date 2024/6/4 10:00
 */
public class NodeFactory {

    private NodeFactory() {
    }

    /**
     * 创建顺序节点
     *
     * @param nodeName    节点名称
     * @param childNodes  子节点列表
     * @param <Result>    节点结果类型
     * @param <Context>   上下文类型
     * @return 顺序节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IControlNode<Result, Context> createSequenceNode(String nodeName, List<INode<Result, Context>> childNodes) {
        return new SequenceNodeImpl<>(nodeName, childNodes);
    }

    /**
     * 创建选择节点
     *
     * @param nodeName    节点名称
     * @param childNodes  子节点列表
     * @param <Result>    节点结果类型
     * @param <Context>   上下文类型
     * @return 选择节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IControlNode<Result, Context> createSelectorNode(String nodeName, List<INode<Result, Context>> childNodes) {
        return new SelectorNodeImpl<>(nodeName, childNodes);
    }

    /**
     * 创建并行节点
     *
     * @param nodeName    节点名称
     * @param childNodes  子节点列表
     * @param <Result>    节点结果类型
     * @param <Context>   上下文类型
     * @return 并行节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IControlNode<Result, Context> createParallelNode(String nodeName, List<INode<Result, Context>> childNodes) {
        return new ParallelNodeImpl<>(nodeName, childNodes);
    }

    /**
     * 创建并行节点（带执行器）
     *
     * @param nodeName    节点名称
     * @param childNodes  子节点列表
     * @param executor    执行器
     * @param <Result>    节点结果类型
     * @param <Context>   上下文类型
     * @return 并行节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IControlNode<Result, Context> createParallelNode(String nodeName, List<INode<Result, Context>> childNodes, Executor executor) {
        return new ParallelNodeImpl<>(nodeName, childNodes, executor);
    }

    /**
     * 创建随机节点
     *
     * @param nodeName    节点名称
     * @param childNodes  子节点列表
     * @param <Result>    节点结果类型
     * @param <Context>   上下文类型
     * @return 随机节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IControlNode<Result, Context> createRandomNode(String nodeName, List<INode<Result, Context>> childNodes) {
        return new RandomNodeImpl<>(nodeName, childNodes);
    }

    /**
     * 创建策略节点
     *
     * @param nodeName      节点名称
     * @param conditionNode 条件节点
     * @param strategyMap   策略映射
     * @param <Result>      节点结果类型
     * @param <Context>     上下文类型
     * @return 策略节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createStrategyNode(String nodeName, INode<Result, Context> conditionNode, Map<Result, INode<Result, Context>> strategyMap) {
        return new StrategyNodeImpl<>(nodeName, conditionNode, strategyMap);
    }

    /**
     * 创建成功动作节点
     *
     * @param nodeName  节点名称
     * @param <Result>  节点结果类型
     * @param <Context> 上下文类型
     * @return 成功动作节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IActionNode<Result, Context> createSuccessActionNode(String nodeName) {
        return new SuccessActionNodeImpl<>(nodeName);
    }

    /**
     * 创建失败动作节点
     *
     * @param nodeName  节点名称
     * @param <Result>  节点结果类型
     * @param <Context> 上下文类型
     * @return 失败动作节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IActionNode<Result, Context> createFailureActionNode(String nodeName) {
        return new FailureActionNodeImpl<>(nodeName);
    }

    /**
     * 创建自定义动作节点
     *
     * @param nodeName     节点名称
     * @param retryEnabled 是否启用重试
     * @param retryPolicy  重试策略
     * @param action       动作函数
     * @param <Result>     节点结果类型
     * @param <Context>    上下文类型
     * @return 自定义动作节点
     */
    public static <Result extends NodeResult, Context extends BaseContext> IActionNode<Result, Context> createActionNode(String nodeName, boolean retryEnabled, RetryPolicy retryPolicy, ActionNodeFunction<Result, Context> action) {
        return new DefaultActionNodeImpl<>(nodeName, retryEnabled, retryPolicy, action);
    }
}