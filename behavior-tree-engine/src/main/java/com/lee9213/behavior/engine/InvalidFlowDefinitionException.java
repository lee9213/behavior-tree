package com.lee9213.behavior.engine;

/**
 * Thrown when a {@link FlowDefinition} fails validation or is otherwise invalid.
 */
public class InvalidFlowDefinitionException extends RuntimeException {

    public InvalidFlowDefinitionException(String message) {
        super(message);
    }

    public InvalidFlowDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
