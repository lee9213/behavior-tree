package com.lee9213.behavior.tree.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.tree.node.INode;

import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Random;

/**
 * 随机节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:52
 */
@Log4j2
public final class RandomNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> {

    public RandomNodeImpl(String nodeName, List<INode<Result, Context>> childNodeList) {
        super(nodeName);
        this.childNodeList = childNodeList;
    }

    @Override
    public Result execute(Context context) {
        int index = new Random().nextInt(childNodeList.size());
        INode<Result, Context> node = childNodeList.get(index);
        context.setCurrentNode(node);
        Result nodeResult = node.execute(context);
        checkNodeResult(nodeResult);
        log.info("节点{}执行结果：{}。", node.getNodeName(), nodeResult);
        return nodeResult;
    }
}
