package com.lee9213.behavior;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:28
 */
@Data
@AllArgsConstructor
public class NodeResult {

    public static final NodeResult SUCCESS = new NodeResult("SUCCESS");
    public static final NodeResult FAILURE = new NodeResult("FAILURE");
    public static final NodeResult RUNNING = new NodeResult("RUNNING");

    protected String code;

    public boolean isSuccess() {
        return this.equals(SUCCESS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeResult nodeResult = (NodeResult) o;
        return Objects.equals(code, nodeResult.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
