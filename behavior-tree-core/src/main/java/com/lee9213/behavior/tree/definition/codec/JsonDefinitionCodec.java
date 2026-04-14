package com.lee9213.behavior.tree.definition.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lee9213.behavior.tree.definition.exception.DefinitionSyntaxException;
import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * JSON 文本 → {@link BehaviorDefinitionNode}。
 */
public final class JsonDefinitionCodec {

    private static final ObjectMapper MAPPER = DefinitionObjectMappers.jsonMapper();

    private JsonDefinitionCodec() {
    }

    public static BehaviorDefinitionNode readTree(String content) {
        try {
            return MAPPER.readValue(content, BehaviorDefinitionNode.class);
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition JSON", e);
        }
    }

    public static BehaviorDefinitionNode readTree(InputStream in, Charset charset) {
        try (InputStreamReader reader = new InputStreamReader(in, charset)) {
            return MAPPER.readValue(reader, BehaviorDefinitionNode.class);
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition JSON", e);
        }
    }
}
