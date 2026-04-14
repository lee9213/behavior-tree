package com.lee9213.behavior.engine;

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
        INode<NodeResult, FlowExecutionContext> root = tree.getRootNode();
        if (root == null) {
            throw new InvalidFlowDefinitionException("Behavior tree root is null");
        }

        bfsAssignPathIds(root);
        IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> adj = buildStructuralAdjacency(root);
        if (hasCycle(adj, root)) {
            throw new InvalidFlowDefinitionException("Behavior graph contains a cycle");
        }
    }

    /**
     * BFS from root assigning stable path ids ("0", "0_0", "0_1", ...).
     */
    private static void bfsAssignPathIds(INode<NodeResult, FlowExecutionContext> root) {
        ArrayDeque<PathItem> queue = new ArrayDeque<>();
        queue.add(new PathItem(root, "0"));
        while (!queue.isEmpty()) {
            PathItem item = queue.remove();
            List<INode<NodeResult, FlowExecutionContext>> children = structuralChildren(item.node);
            for (int i = 0; i < children.size(); i++) {
                INode<NodeResult, FlowExecutionContext> child = children.get(i);
                if (child == null) {
                    throw new InvalidFlowDefinitionException("Structural child is null at path " + item.pathId);
                }
                queue.add(new PathItem(child, item.pathId + "_" + i));
            }
        }
    }

    private static final class PathItem {
        final INode<NodeResult, FlowExecutionContext> node;
        final String pathId;

        PathItem(INode<NodeResult, FlowExecutionContext> node, String pathId) {
            this.node = node;
            this.pathId = pathId;
        }
    }

    /**
     * Structural edges by node identity (shared nodes can form cycles).
     */
    private static IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> buildStructuralAdjacency(
            INode<NodeResult, FlowExecutionContext> root) {
        IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> adj = new IdentityHashMap<>();
        ArrayDeque<INode<NodeResult, FlowExecutionContext>> queue = new ArrayDeque<>();
        IdentityHashMap<INode<NodeResult, FlowExecutionContext>, Boolean> expandedFrom = new IdentityHashMap<>();

        queue.add(root);
        expandedFrom.put(root, Boolean.TRUE);
        while (!queue.isEmpty()) {
            INode<NodeResult, FlowExecutionContext> parent = queue.remove();
            for (INode<NodeResult, FlowExecutionContext> child : structuralChildren(parent)) {
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

    private static List<INode<NodeResult, FlowExecutionContext>> structuralChildren(
            INode<NodeResult, FlowExecutionContext> node) {
        if (node instanceof AbstractControlNode) {
            return ((AbstractControlNode<NodeResult, FlowExecutionContext>) node).getChildNodes();
        }
        if (node instanceof StrategyNodeImpl) {
            StrategyNodeImpl<NodeResult, FlowExecutionContext> sn = (StrategyNodeImpl<NodeResult, FlowExecutionContext>) node;
            INode<NodeResult, FlowExecutionContext> condition = sn.getConditionNode();
            if (condition == null) {
                throw new InvalidFlowDefinitionException("Strategy node condition is null");
            }
            List<INode<NodeResult, FlowExecutionContext>> list = new ArrayList<>();
            list.add(condition);
            Map<NodeResult, INode<NodeResult, FlowExecutionContext>> map = sn.getStrategyMap();
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
            IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> adj,
            INode<NodeResult, FlowExecutionContext> root) {
        IdentityHashMap<INode<?, ?>, Mark> mark = new IdentityHashMap<>();
        return dfsHasCycle(root, adj, mark);
    }

    private static boolean dfsHasCycle(
            INode<?, ?> node,
            IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> adj,
            IdentityHashMap<INode<?, ?>, Mark> mark) {
        Mark m = mark.get(node);
        if (m == Mark.GRAY) {
            return true;
        }
        if (m == Mark.BLACK) {
            return false;
        }
        mark.put(node, Mark.GRAY);
        for (INode<?, ?> next : adj.getOrDefault(node, List.of())) {
            if (dfsHasCycle(next, adj, mark)) {
                return true;
            }
        }
        mark.put(node, Mark.BLACK);
        return false;
    }
}
