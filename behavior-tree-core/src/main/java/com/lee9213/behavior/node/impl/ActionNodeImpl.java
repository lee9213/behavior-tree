package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.RetryPolicy;

/**
 * 自定义动作节点实现
 *
 * @author lee9213@163.com
 * @date 2024/6/4 10:00
 */
public final class ActionNodeImpl<Result extends NodeResult, Context extends BaseContext> extends AbstractActionNode<Result, Context> {

    private final ActionFunction<Result, Context> action;

    public ActionNodeImpl(String nodeName, boolean retryEnabled, RetryPolicy retryPolicy, ActionFunction<Result, Context> action) {
        super(nodeName, retryEnabled, retryPolicy);
        this.action = action;
    }

    @Override
    protected Result doExecute(Context context) {
        return action.apply(context);
    }

    /**
     * 动作函数接口
     *
     * @param <Result>  节点结果类型
     * @param <Context> 上下文类型
     */
    @FunctionalInterface
    public interface ActionFunction<Result extends NodeResult, Context extends BaseContext> {
        Result apply(Context context);
    }
}