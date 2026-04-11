package com.lee9213.behavior.definition.exception;

/**
 * 语法层失败：文本无法解析为 IR。
 */
public class DefinitionSyntaxException extends RuntimeException {

    public DefinitionSyntaxException(String message) {
        super(message);
    }

    public DefinitionSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
