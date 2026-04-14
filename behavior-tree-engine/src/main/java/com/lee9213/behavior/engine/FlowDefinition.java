package com.lee9213.behavior.engine;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Immutable description of a flow: identity, version, and the behavior tree to run.
 */
@Getter
@RequiredArgsConstructor
public final class FlowDefinition {

    private final String id;
    private final String version;
    private final BehaviorTree<NodeResult, FlowExecutionContext> behaviorTree;
}
