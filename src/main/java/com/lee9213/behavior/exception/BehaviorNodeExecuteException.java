package com.lee9213.behavior.exception;

/**
 * 节点执行异常
 *
 * @author lee9213@163.com
 * @date 2024/5/30 15:00
 */
public class BehaviorNodeExecuteException extends RuntimeException {
    public BehaviorNodeExecuteException(String message) {
        super(message);
    }
}
