package com.lee9213.behavior.tree.engine;

/**
 * 当 {@link FlowDefinition} 验证失败或其他原因无效时抛出。
 */
public class InvalidFlowDefinitionException extends RuntimeException {

    public InvalidFlowDefinitionException(String message) {
        super(message);
    }

    public InvalidFlowDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
