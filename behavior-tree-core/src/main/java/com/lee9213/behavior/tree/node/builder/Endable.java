package com.lee9213.behavior.tree.node.builder;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.ActionNodeFunction;

/**
 * 可结束的建造者接口，用于支持链式调用结束后的操作
 *
 * @param <R> 节点结果类型
 * @param <C> 上下文类型
 */
public interface Endable<R extends NodeResult, C extends BaseContext> {
    /**
     * 构建行为树
     *
     * @return 行为树实例
     */
    BehaviorTree<R, C> build();

    /**
     * 结束当前节点，返回父节点或行为树建造者
     *
     * @return 父节点建造者或行为树建造者
     */
    Endable<R, C> end();

    /**
     * 创建顺序节点建造者
     *
     * @param nodeName 节点名称
     * @return 顺序节点建造者
     */
    SequenceBuilder<R, C> sequence(String nodeName);

    /**
     * 创建选择节点建造者
     *
     * @param nodeName 节点名称
     * @return 选择节点建造者
     */
    SelectorBuilder<R, C> selector(String nodeName);

    /**
     * 创建并行节点建造者
     *
     * @param nodeName 节点名称
     * @return 并行节点建造者
     */
    ParallelBuilder<R, C> parallel(String nodeName);

    /**
     * 创建随机节点建造者
     *
     * @param nodeName 节点名称
     * @return 随机节点建造者
     */
    RandomBuilder<R, C> random(String nodeName);

    /**
     * 创建策略节点建造者
     *
     * @param nodeName 节点名称
     * @return 策略节点建造者
     */
    StrategyBuilder<R, C> strategy(String nodeName);

    /**
     * 创建动作节点
     *
     * @param nodeName 节点名称
     * @param action 动作函数
     * @return 当前建造者
     */
    Endable<R, C> action(String nodeName, ActionNodeFunction<R, C> action);
}
