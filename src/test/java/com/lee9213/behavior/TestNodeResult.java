package com.lee9213.behavior;

import java.util.Objects;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:28
 */
public class TestNodeResult extends NodeResult {

    public static final TestNodeResult A = new TestNodeResult("A");
    public static final TestNodeResult B = new TestNodeResult("B");

    public TestNodeResult(String code) {
        super(code);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestNodeResult nodeResult = (TestNodeResult) o;
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
