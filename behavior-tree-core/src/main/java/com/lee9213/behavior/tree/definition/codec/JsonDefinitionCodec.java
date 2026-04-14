package com.lee9213.behavior.tree.definition.codec;

import com.alibaba.fastjson.JSON;
import com.lee9213.behavior.tree.definition.exception.DefinitionSyntaxException;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * JSON 文本 → {@link BehaviorDefinitionNode}。
 */
public final class JsonDefinitionCodec {

    private JsonDefinitionCodec() {
    }

    public static BehaviorDefinitionNode readTree(String content) {
        Objects.requireNonNull(content, "content cannot be null");
        try {
            return JSON.parseObject(content, BehaviorDefinitionNode.class);
        } catch (Exception e) {
            throw new DefinitionSyntaxException("Invalid behavior definition JSON", e);
        }
    }

    public static BehaviorDefinitionNode readTree(InputStream in, Charset charset) {
        Objects.requireNonNull(in, "input stream cannot be null");
        Objects.requireNonNull(charset, "charset cannot be null");
        try {
            return JSON.parseObject(in, charset, BehaviorDefinitionNode.class);
        } catch (Exception e) {
            throw new DefinitionSyntaxException("Invalid behavior definition JSON", e);
        }
    }
}
