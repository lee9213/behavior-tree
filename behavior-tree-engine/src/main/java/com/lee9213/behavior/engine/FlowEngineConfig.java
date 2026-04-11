package com.lee9213.behavior.engine;

import com.lee9213.behavior.engine.retry.RetryPolicyRegistry;
import com.lee9213.behavior.engine.store.ProcessInstanceStore;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class FlowEngineConfig {

    private boolean retryEnabled = true;
    private RetryPolicyRegistry retryPolicyRegistry = new RetryPolicyRegistry();
    private ProcessInstanceStore store;
    private Executor parallelExecutor = ForkJoinPool.commonPool();

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public RetryPolicyRegistry getRetryPolicyRegistry() {
        return retryPolicyRegistry;
    }

    public void setRetryPolicyRegistry(RetryPolicyRegistry retryPolicyRegistry) {
        this.retryPolicyRegistry = retryPolicyRegistry;
    }

    public ProcessInstanceStore getStore() {
        return store;
    }

    public void setStore(ProcessInstanceStore store) {
        this.store = store;
    }

    public Executor getParallelExecutor() {
        return parallelExecutor;
    }

    public void setParallelExecutor(Executor parallelExecutor) {
        this.parallelExecutor = parallelExecutor;
    }
}
