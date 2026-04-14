package com.lee9213.behavior.definition.codec;

import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.definition.codec.JsonDefinitionCodec;
import com.lee9213.behavior.tree.enums.NodeType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonDefinitionCodecTest {

    @Test
    void readGoldenJsonFromClasspath() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/definitions/golden.json")) {
            assertNotNull(in);
            BehaviorDefinitionNode root = JsonDefinitionCodec.readTree(in, StandardCharsets.UTF_8);
            assertEquals("RootNodeTest1", root.nodeName());
            assertEquals(NodeType.Sequence, root.nodeType());
            assertEquals(4, root.children().size());
        }
    }
}
