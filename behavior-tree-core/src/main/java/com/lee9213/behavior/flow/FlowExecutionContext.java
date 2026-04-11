package com.lee9213.behavior.flow;

import com.lee9213.behavior.BaseContext;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution context for the flow engine. Parallel branches must use {@link #copyForParallelBranch()}
 * so mutable state is not shared across threads.
 */
@Getter
@Setter
public class FlowExecutionContext extends BaseContext {

    private String flowInstanceId;

    /**
     * Per-step-path retry counts; may be filled when loading a persisted instance snapshot in the engine.
     */
    private Map<String, Integer> retryCountByStepPath = new HashMap<>();

    protected FlowExecutionContext(FlowExecutionContext other) {
        this.setCurrentNode(other.getCurrentNode());
        this.flowInstanceId = other.flowInstanceId;
        if (other.retryCountByStepPath != null) {
            this.retryCountByStepPath = new HashMap<>(other.retryCountByStepPath);
        }
    }

    public FlowExecutionContext() {
    }

    /**
     * If subclasses carry mutable business fields that must be isolated per branch, override this in application code.
     */
    public FlowExecutionContext copyForParallelBranch() {
        return new FlowExecutionContext(this);
    }
}
