package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.IParallelNode;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Log4j2
public final class ConcurrentParallelNodeImpl<Result extends NodeResult, Context extends BaseContext>
        extends AbstractControlNode<Result, Context> implements IParallelNode<Result, Context> {

    private final Executor executor;

    public ConcurrentParallelNodeImpl(List<BehaviorNodeWrapper<Result, Context>> childNodeList, Executor executor) {
        this.childNodeList = childNodeList;
        this.executor = executor;
    }

    @Override
    public Result execute(Context context) {
        if (childNodeList == null || childNodeList.isEmpty()) {
            return (Result) NodeResult.SUCCESS;
        }
        List<CompletableFuture<Result>> futures = new ArrayList<>();
        for (BehaviorNodeWrapper<Result, Context> wrapper : childNodeList) {
            final BehaviorNodeWrapper<Result, Context> w = wrapper;
            Context branchContext = context;
            if (context instanceof FlowExecutionContext) {
                branchContext = (Context) ((FlowExecutionContext) context).copyForParallelBranch();
            }
            Context bc = branchContext;
            CompletableFuture<Result> future = CompletableFuture.supplyAsync(() -> {
                bc.setCurrentNode(w);
                return w.getNode().execute(bc);
            }, executor);
            futures.add(future);
        }
        boolean isSuccess = true;
        for (CompletableFuture<Result> future : futures) {
            try {
                Result nodeResult = future.join();
                checkNodeResult(nodeResult);
                if (!nodeResult.isSuccess()) {
                    isSuccess = false;
                }
                log.info("parallel child result: {}", nodeResult);
            } catch (Exception ex) {
                isSuccess = false;
                log.error("parallel child failed", ex);
            }
        }
        return (Result) (isSuccess ? NodeResult.SUCCESS : NodeResult.FAILURE);
    }
}
