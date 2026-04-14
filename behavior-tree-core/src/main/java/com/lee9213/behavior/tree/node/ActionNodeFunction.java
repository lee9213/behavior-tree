package com.lee9213.behavior.tree.node;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;

/**
 * 动作函数接口，用于定义动作节点的执行逻辑
 *
 * @author lee9213@163.com
 * @date 2024/6/4 10:00
 */
public interface ActionNodeFunction<Result extends NodeResult, Context extends BaseContext> {

    /**
     * 执行动作
     *
     * @param context 上下文
     * @return 执行结果
     */
    Result apply(Context context);

}