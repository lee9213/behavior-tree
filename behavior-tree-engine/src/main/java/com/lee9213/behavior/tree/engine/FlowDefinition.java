package com.lee9213.behavior.tree.engine;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 流程的不可变描述：标识、版本和要运行的行为树。
 */
@Getter
@RequiredArgsConstructor
public final class FlowDefinition<C extends BaseContext> {

    private final String id;
    private final String version;
    private final BehaviorTree<NodeResult, C> behaviorTree;
}
