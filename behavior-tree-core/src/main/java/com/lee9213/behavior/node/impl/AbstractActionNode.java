package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.exception.BehaviorNodeExecuteException;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.IActionNode;
import com.lee9213.behavior.retry.RetryExecutor;
import com.lee9213.behavior.retry.RetryPolicy;

/**
 * 叶子动作节点抽象基类：按构造参数决定是否对 {@link #doExecute} 做自动重试（指数退避 + 抖动见 {@link RetryPolicy}）。
 * 全局引擎开关与节点开关的组合可在子类或后续版本中扩展。
 */
public abstract class AbstractActionNode implements IActionNode<NodeResult, FlowExecutionContext> {

    private final boolean retryEnabled;
    private final RetryPolicy retryPolicy;

    protected AbstractActionNode(boolean retryEnabled, RetryPolicy retryPolicy) {
        this.retryEnabled = retryEnabled;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public NodeResult execute(FlowExecutionContext context) {
        try {
            return RetryExecutor.execute(retryEnabled, retryPolicy, () -> doExecute(context));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BehaviorNodeExecuteException("可重试动作执行失败", e);
        }
    }

    /**
     * 单次执行业务逻辑；失败时由 {@link RetryExecutor} 按策略重试（当 {@code retryEnabled} 为 true）。
     */
    protected abstract NodeResult doExecute(FlowExecutionContext context) throws Exception;
}
