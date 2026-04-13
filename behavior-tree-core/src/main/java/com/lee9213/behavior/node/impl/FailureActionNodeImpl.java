package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.RetryPolicy;

/**
 * @author lee9213@163.com
 * @date 2024/5/31 09:44
 */
public final class FailureActionNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractActionNode<Result, Context> {

    public FailureActionNodeImpl(String nodeName) {
        super(nodeName, false, null);
    }

    @Override
    protected Result doExecute(Context context) {
        return (Result) NodeResult.FAILURE;
    }
}
