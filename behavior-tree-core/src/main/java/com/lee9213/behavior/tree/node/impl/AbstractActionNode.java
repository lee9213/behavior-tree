package com.lee9213.behavior.tree.node.impl;

import java.util.concurrent.ThreadLocalRandom;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.tree.exception.BehaviorNodeExecuteException;
import com.lee9213.behavior.tree.node.IActionNode;
import com.lee9213.behavior.tree.retry.RetryPolicy;

/**
 * 叶子动作节点抽象基类：按构造参数决定是否对 {@link #doExecute} 做自动重试（指数退避 + 抖动见 {@link RetryPolicy}）。
 * 全局引擎开关与节点开关的组合可在子类或后续版本中扩展。
 */
public abstract class AbstractActionNode<Result extends NodeResult, Context extends BaseContext> implements IActionNode<Result, Context> {

    protected String nodeName;
    protected String stepTag;
    private final boolean retryEnabled;
    private final RetryPolicy retryPolicy;

    protected AbstractActionNode(String nodeName, boolean retryEnabled, RetryPolicy retryPolicy) {
        this.nodeName = nodeName;
        this.retryEnabled = retryEnabled;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }
    
    @Override
    public String getStepTag() {
        return stepTag;
    }
    
    @Override
    public void setStepTag(String stepTag) {
        this.stepTag = stepTag;
    }

    @Override
    public Result execute(Context context) {
        try {
            // return RetryExecutor.execute(retryEnabled, retryPolicy, () -> doExecute(context));
            if (!retryEnabled) {
                return doExecute(context);
            }
            int attemptNo = 0;
            long delay = retryPolicy.baseDelayMillis();
            Exception last = null;
            while (attemptNo < retryPolicy.maxAttempts()) {
                try {
                    return doExecute(context);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                } catch (Exception ex) {
                    last = ex;
                    attemptNo++;
                    if (attemptNo >= retryPolicy.maxAttempts()) {
                        break;
                    }
                    long jitter = (long) (delay * retryPolicy.jitterRatio() * ThreadLocalRandom.current().nextDouble());
                    try {
                        Thread.sleep(delay + jitter);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                    delay = (long) (delay * retryPolicy.multiplier());
                }
            }
            if (last != null) {
                throw last;
            }
            throw new IllegalStateException("retry without exception");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BehaviorNodeExecuteException("可重试动作执行失败", e);
        }
    }

    /**
     * 单次执行业务逻辑；失败时由 {@link RetryExecutor} 按策略重试（当 {@code retryEnabled} 为 true）。
     */
    protected abstract Result doExecute(Context context) throws Exception;
}
