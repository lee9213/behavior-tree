package com.lee9213.behavior.tree.engine;

import com.lee9213.behavior.tree.engine.store.ProcessInstanceStore;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class FlowEngineConfig {

    private boolean retryEnabled = true;
    private ProcessInstanceStore store;
    private Executor parallelExecutor = ForkJoinPool.commonPool();

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
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
