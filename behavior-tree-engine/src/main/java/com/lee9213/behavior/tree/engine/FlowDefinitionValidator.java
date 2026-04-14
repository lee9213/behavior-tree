package com.lee9213.behavior.tree.engine;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.node.impl.AbstractControlNode;
import com.lee9213.behavior.tree.node.impl.StrategyNodeImpl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证 {@link FlowDefinition} 实例：必填字段、结构图和无环检查。
 */
public final class FlowDefinitionValidator {

    private FlowDefinitionValidator() {
    }

    public static <C extends BaseContext> void validate(FlowDefinition<C> def) {
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
        BehaviorTree<NodeResult, C> tree = def.getBehaviorTree();
        if (tree == null) {
            throw new InvalidFlowDefinitionException("BehaviorTree is null");
        }
        INode<NodeResult, C> root = tree.getRootNode();
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
     * 从根节点开始进行 BFS，分配稳定的路径 ID（"0", "0_0", "0_1", ...）。
     */
    private static <C extends BaseContext> void bfsAssignPathIds(INode<NodeResult, C> root) {
        ArrayDeque<PathItem<C>> queue = new ArrayDeque<>();
        queue.add(new PathItem<>(root, "0"));
        while (!queue.isEmpty()) {
            PathItem<C> item = queue.remove();
            List<INode<NodeResult, C>> children = structuralChildren(item.node);
            for (int i = 0; i < children.size(); i++) {
                INode<NodeResult, C> child = children.get(i);
                if (child == null) {
                    throw new InvalidFlowDefinitionException("Structural child is null at path " + item.pathId);
                }
                queue.add(new PathItem<>(child, item.pathId + "_" + i));
            }
        }
    }

    private static final class PathItem<C extends BaseContext> {
        final INode<NodeResult, C> node;
        final String pathId;

        PathItem(INode<NodeResult, C> node, String pathId) {
            this.node = node;
            this.pathId = pathId;
        }
    }

    /**
     * 按节点标识的结构边（共享节点可能形成循环）。
     */
    private static <C extends BaseContext> IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> buildStructuralAdjacency(
            INode<NodeResult, C> root) {
        IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> adj = new IdentityHashMap<>();
        ArrayDeque<INode<NodeResult, C>> queue = new ArrayDeque<>();
        IdentityHashMap<INode<NodeResult, C>, Boolean> expandedFrom = new IdentityHashMap<>();

        queue.add(root);
        expandedFrom.put(root, Boolean.TRUE);
        while (!queue.isEmpty()) {
            INode<NodeResult, C> parent = queue.remove();
            for (INode<NodeResult, C> child : structuralChildren(parent)) {
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

    private static <C extends BaseContext> List<INode<NodeResult, C>> structuralChildren(
            INode<NodeResult, C> node) {
        if (node instanceof AbstractControlNode) {
            return ((AbstractControlNode<NodeResult, C>) node).getChildNodes();
        }
        if (node instanceof StrategyNodeImpl) {
            StrategyNodeImpl<NodeResult, C> sn = (StrategyNodeImpl<NodeResult, C>) node;
            INode<NodeResult, C> condition = sn.getConditionNode();
            if (condition == null) {
                throw new InvalidFlowDefinitionException("Strategy node condition is null");
            }
            List<INode<NodeResult, C>> list = new ArrayList<>();
            list.add(condition);
            Map<NodeResult, INode<NodeResult, C>> map = sn.getStrategyMap();
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

    private static <C extends BaseContext> boolean hasCycle(
            IdentityHashMap<INode<?, ?>, List<INode<?, ?>>> adj,
            INode<NodeResult, C> root) {
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
