package com.lee9213.behavior.definition.codec;

import com.lee9213.behavior.tree.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.definition.codec.JsonDefinitionCodec;
import com.lee9213.behavior.tree.definition.codec.XmlDefinitionCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefinitionCodecEquivalenceTest {

    @Test
    void jsonAndXmlGoldenProduceSameIr() throws Exception {
        String json = new String(
                getClass().getResourceAsStream("/definitions/golden.json").readAllBytes(),
                StandardCharsets.UTF_8);
        String xml = new String(
                getClass().getResourceAsStream("/definitions/golden.xml").readAllBytes(),
                StandardCharsets.UTF_8);

        BehaviorDefinitionNode fromJson = JsonDefinitionCodec.readTree(json);
        BehaviorDefinitionNode fromXml = XmlDefinitionCodec.readTree(xml);

        assertEquals(fromJson, fromXml);
    }

    @Test
    void xmlGoldenFromClasspath() throws Exception {
        try (var in = getClass().getResourceAsStream("/definitions/golden.xml")) {
            BehaviorDefinitionNode root = XmlDefinitionCodec.readTree(in, StandardCharsets.UTF_8);
            assertEquals("RootNodeTest1", root.nodeName());
        }
    }
}
