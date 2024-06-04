package com.lee9213.behavior.exception;

/**
 * 节点不存在异常
 *
 * @author lee9213@163.com
 * @date 2024/5/30 15:00
 */
public class BehaviorNodeNotFoundException extends RuntimeException {
    public BehaviorNodeNotFoundException(String message) {
        super(message);
    }
}
