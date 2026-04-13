package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.IParallelNode;
import com.lee9213.behavior.node.INode;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 并行节点：{@code executor == null} 时顺序执行子节点；非空时在 {@link Executor} 上并发执行（不取消兄弟任务，全部结束后合取成功/失败）。
 * 并发路径建议使用 {@link FlowExecutionContext} 以便分支隔离；否则多线程可能共享同一可变 {@link BaseContext}。
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:17
 */
@Log4j2
public final class ParallelNodeImpl<Result extends NodeResult, Context extends BaseContext> extends AbstractControlNode<Result, Context> implements IParallelNode<Result, Context> {

    private final Executor executor;

    public ParallelNodeImpl(String nodeName, List<INode<Result, Context>> childNodeList) {
        this(nodeName, childNodeList, null);
    }

    public ParallelNodeImpl(String nodeName, List<INode<Result, Context>> childNodeList, Executor executor) {
        super(nodeName);
        this.childNodeList = childNodeList;
        this.executor = executor;
    }

    @Override
    public Result execute(Context context) {
        if (childNodeList == null || childNodeList.isEmpty()) {
            return (Result) NodeResult.SUCCESS;
        }
        if (executor == null) {
            return executeSequential(context);
        }
        return executeConcurrent(context);
    }

    private Result executeSequential(Context context) {
        boolean isSuccess = true;
        for (INode<Result, Context> node : childNodeList) {
            context.setCurrentNode(node);
            Result nodeResult = node.execute(context);
            checkNodeResult(nodeResult);
            if (!nodeResult.isSuccess()) {
                isSuccess = false;
            }
            log.info("节点{}执行结果：{}", node.getNodeName(), nodeResult);
        }
        return (Result) (isSuccess ? NodeResult.SUCCESS : NodeResult.FAILURE);
    }

    private Result executeConcurrent(Context context) {
        List<CompletableFuture<Result>> futures = new ArrayList<>();
        for (INode<Result, Context> node : childNodeList) {
            final INode<Result, Context> n = node;
            Context branchContext = context;
            if (context instanceof FlowExecutionContext) {
                branchContext = (Context) ((FlowExecutionContext) context).copyForParallelBranch();
            }
            Context bc = branchContext;
            CompletableFuture<Result> future = CompletableFuture.supplyAsync(() -> {
                bc.setCurrentNode(n);
                return n.execute(bc);
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
