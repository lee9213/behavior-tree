package com.lee9213.behavior.tree.node;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:11
 */
public interface INode<Result extends NodeResult,Context extends BaseContext> {

    /**
     * 执行具体业务
     *
     * @param context 业务上下文
     * @return 执行结果
     */
    Result execute(Context context);
    
    /**
     * 获取节点名称
     *
     * @return 节点名称
     */
    String getNodeName();
    
    /**
     * 获取步骤标签
     *
     * @return 步骤标签
     */
    String getStepTag();
    
    /**
     * 设置步骤标签
     *
     * @param stepTag 步骤标签
     */
    void setStepTag(String stepTag);
}
