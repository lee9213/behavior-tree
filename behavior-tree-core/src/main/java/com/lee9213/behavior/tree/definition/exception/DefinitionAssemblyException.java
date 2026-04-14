package com.lee9213.behavior.tree.definition.exception;

import com.lee9213.behavior.tree.node.INode;

/**
 * 语义装配失败：IR 无法转为运行时 {@link INode}。
 */
public class DefinitionAssemblyException extends RuntimeException {

    public DefinitionAssemblyException(String message) {
        super(message);
    }

    public DefinitionAssemblyException(String message, Throwable cause) {
        super(message, cause);
    }
}
