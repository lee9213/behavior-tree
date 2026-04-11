package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.node.IRandomNode;
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
public final class RandomNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements IRandomNode<Result, Context> {

    public RandomNodeImpl(List<BehaviorNodeWrapper<Result, Context>> childNodeList) {
        this.childNodeList = childNodeList;
    }

    @Override
    public Result execute(Context context) {
        int index = new Random().nextInt(childNodeList.size());
        BehaviorNodeWrapper<Result, Context> behaviorNodeWrapper = childNodeList.get(index);
        context.setCurrentNode(behaviorNodeWrapper);
        Result nodeResult = behaviorNodeWrapper.getNode().execute(context);
        checkNodeResult(nodeResult);
        log.info("节点{}执行结果：{}。", behaviorNodeWrapper.getNodeName(), nodeResult);
        return nodeResult;
    }
}
