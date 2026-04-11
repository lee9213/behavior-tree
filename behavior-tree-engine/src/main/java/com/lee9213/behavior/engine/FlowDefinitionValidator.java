package com.lee9213.behavior.engine;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.BehaviorTree;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.INode;
import com.lee9213.behavior.node.impl.AbstractControlNode;
import com.lee9213.behavior.node.impl.StrategyNodeImpl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates {@link FlowDefinition} instances: required fields, structural graph, and absence of cycles.
 */
public final class FlowDefinitionValidator {

    private FlowDefinitionValidator() {
    }

    public static void validate(FlowDefinition def) {
        if (def == null) {
            throw new InvalidFlowDefinitionException("FlowDefinition is null");
        }
        String id = def.getId() == null ? "" : def.getId().trim();
        String version = def.getVersion() == null ? "" : def.getVersion().trim();
        if (id.isEmpty()) {
            throw new InvalidFlowDefinitionException("Flow id is blank");
        }
        if (version.isEmpty()) {
            throw new InvalidFlowDefinitionException("Flow version is blank");
        }
        BehaviorTree<NodeResult, FlowExecutionContext> tree = def.getBehaviorTree();
        if (tree == null) {
            throw new InvalidFlowDefinitionException("BehaviorTree is null");
        }
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root = tree.getRootNode();
        if (root == null) {
            throw new InvalidFlowDefinitionException("Behavior tree root is null");
        }

        bfsAssignPathIds(root);
        IdentityHashMap<BehaviorNodeWrapper<?, ?>, List<BehaviorNodeWrapper<?, ?>>> adj = buildStructuralAdjacency(root);
        if (hasCycle(adj, root)) {
            throw new InvalidFlowDefinitionException("Behavior graph contains a cycle");
        }
    }

    /**
     * BFS from root assigning stable path ids ("0", "0_0", "0_1", ...).
     */
    private static void bfsAssignPathIds(BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root) {
        ArrayDeque<PathItem> queue = new ArrayDeque<>();
        queue.add(new PathItem(root, "0"));
        while (!queue.isEmpty()) {
            PathItem item = queue.remove();
            List<BehaviorNodeWrapper<NodeResult, FlowExecutionContext>> children = structuralChildren(item.wrapper);
            for (int i = 0; i < children.size(); i++) {
                BehaviorNodeWrapper<NodeResult, FlowExecutionContext> child = children.get(i);
                if (child == null) {
                    throw new InvalidFlowDefinitionException("Structural child is null at path " + item.pathId);
                }
                queue.add(new PathItem(child, item.pathId + "_" + i));
            }
        }
    }

    private static final class PathItem {
        final BehaviorNodeWrapper<NodeResult, FlowExecutionContext> wrapper;
        final String pathId;

        PathItem(BehaviorNodeWrapper<NodeResult, FlowExecutionContext> wrapper, String pathId) {
            this.wrapper = wrapper;
            this.pathId = pathId;
        }
    }

    /**
     * Structural edges by wrapper identity (shared wrappers can form cycles).
     */
    private static IdentityHashMap<BehaviorNodeWrapper<?, ?>, List<BehaviorNodeWrapper<?, ?>>> buildStructuralAdjacency(
            BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root) {
        IdentityHashMap<BehaviorNodeWrapper<?, ?>, List<BehaviorNodeWrapper<?, ?>>> adj = new IdentityHashMap<>();
        ArrayDeque<BehaviorNodeWrapper<NodeResult, FlowExecutionContext>> queue = new ArrayDeque<>();
        IdentityHashMap<BehaviorNodeWrapper<NodeResult, FlowExecutionContext>, Boolean> expandedFrom = new IdentityHashMap<>();

        queue.add(root);
        expandedFrom.put(root, Boolean.TRUE);
        while (!queue.isEmpty()) {
            BehaviorNodeWrapper<NodeResult, FlowExecutionContext> parent = queue.remove();
            for (BehaviorNodeWrapper<NodeResult, FlowExecutionContext> child : structuralChildren(parent)) {
                if (child == null) {
                    throw new InvalidFlowDefinitionException("Structural child is null");
                }
                adj.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
                if (!expandedFrom.containsKey(child)) {
                    expandedFrom.put(child, Boolean.TRUE);
                    queue.add(child);
                }
            }
        }
        return adj;
    }

    private static List<BehaviorNodeWrapper<NodeResult, FlowExecutionContext>> structuralChildren(
            BehaviorNodeWrapper<NodeResult, FlowExecutionContext> wrapper) {
        INode<NodeResult, FlowExecutionContext> node = wrapper.getNode();
        if (node instanceof AbstractControlNode) {
            return ((AbstractControlNode<NodeResult, FlowExecutionContext>) node).getChildWrappers();
        }
        if (node instanceof StrategyNodeImpl) {
            StrategyNodeImpl<NodeResult, FlowExecutionContext> sn = (StrategyNodeImpl<NodeResult, FlowExecutionContext>) node;
            BehaviorNodeWrapper<NodeResult, FlowExecutionContext> condition = sn.getConditionWrapper();
            if (condition == null) {
                throw new InvalidFlowDefinitionException("Strategy node condition is null");
            }
            List<BehaviorNodeWrapper<NodeResult, FlowExecutionContext>> list = new ArrayList<>();
            list.add(condition);
            Map<NodeResult, BehaviorNodeWrapper<NodeResult, FlowExecutionContext>> map = sn.getStrategyMap();
            if (map != null) {
                list.addAll(map.values());
            }
            return list;
        }
        return List.of();
    }

    private enum Mark {
        WHITE, GRAY, BLACK
    }

    private static boolean hasCycle(
            IdentityHashMap<BehaviorNodeWrapper<?, ?>, List<BehaviorNodeWrapper<?, ?>>> adj,
            BehaviorNodeWrapper<NodeResult, FlowExecutionContext> root) {
        IdentityHashMap<BehaviorNodeWrapper<?, ?>, Mark> mark = new IdentityHashMap<>();
        return dfsHasCycle(root, adj, mark);
    }

    private static boolean dfsHasCycle(
            BehaviorNodeWrapper<?, ?> node,
            IdentityHashMap<BehaviorNodeWrapper<?, ?>, List<BehaviorNodeWrapper<?, ?>>> adj,
            IdentityHashMap<BehaviorNodeWrapper<?, ?>, Mark> mark) {
        Mark m = mark.get(node);
        if (m == Mark.GRAY) {
            return true;
        }
        if (m == Mark.BLACK) {
            return false;
        }
        mark.put(node, Mark.GRAY);
        for (BehaviorNodeWrapper<?, ?> next : adj.getOrDefault(node, List.of())) {
            if (dfsHasCycle(next, adj, mark)) {
                return true;
            }
        }
        mark.put(node, Mark.BLACK);
        return false;
    }
}
