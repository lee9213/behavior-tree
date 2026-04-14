package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.retry.RetryPolicy;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 09:43
 */
public final class SuccessActionNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractActionNode<Result, Context> {

    public SuccessActionNodeImpl() {
        super("SuccessActionNode", false, null);
    }

    public SuccessActionNodeImpl(String nodeName) {
        super(nodeName, false, null);
    }

    @Override
    protected Result doExecute(Context context) {
        return (Result) NodeResult.SUCCESS;
    }
}
