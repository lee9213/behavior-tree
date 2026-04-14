package com.lee9213.behavior.tree.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.tree.retry.RetryPolicy;
import com.lee9213.behavior.tree.node.ActionNodeFunction;

/**
 * 自定义动作节点实现
 *
 * @author lee9213@163.com
 * @date 2024/6/4 10:00
 */
public final class DefaultActionNodeImpl<Result extends NodeResult, Context extends BaseContext> extends AbstractActionNode<Result, Context> {

    private final ActionNodeFunction<Result, Context> action;

    public DefaultActionNodeImpl(String nodeName, boolean retryEnabled, RetryPolicy retryPolicy, ActionNodeFunction<Result, Context> action) {
        super(nodeName, retryEnabled, retryPolicy);
        this.action = action;
    }

    @Override
    protected Result doExecute(Context context) {
        return action.apply(context);
    }


}