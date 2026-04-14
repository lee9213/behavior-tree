package com.lee9213.behavior.tree;

import com.lee9213.behavior.tree.node.INode;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:27
 */
@Data
public class BaseContext implements Serializable {

    protected INode currentNode;
    protected String flowInstanceId;
}
