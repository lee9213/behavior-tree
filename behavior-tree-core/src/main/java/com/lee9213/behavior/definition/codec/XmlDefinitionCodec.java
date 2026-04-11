package com.lee9213.behavior.definition.codec;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.lee9213.behavior.definition.exception.DefinitionSyntaxException;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.enums.NodeType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * XML 文本 → {@link BehaviorDefinitionNode}（与 JSON 同构语义）。
 * <p>
 * 策略分支使用 {@code <strategyMap><entry key="...">...</entry></strategyMap>}，条目的子元素经 {@link JsonUnwrapped} 与动作节点字段对齐。
 */
public final class XmlDefinitionCodec {

    private static final XmlMapper MAPPER = DefinitionObjectMappers.xmlMapper();

    private XmlDefinitionCodec() {
    }

    public static BehaviorDefinitionNode readTree(String content) {
        try {
            BehaviorDefinitionNodeXml root = MAPPER.readValue(content, BehaviorDefinitionNodeXml.class);
            return root.toModel();
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition XML", e);
        }
    }

    public static BehaviorDefinitionNode readTree(InputStream in, Charset charset) {
        try (InputStreamReader reader = new InputStreamReader(in, charset)) {
            BehaviorDefinitionNodeXml root = MAPPER.readValue(reader, BehaviorDefinitionNodeXml.class);
            return root.toModel();
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition XML", e);
        }
    }

    @JacksonXmlRootElement(localName = "behavior")
    static final class BehaviorDefinitionNodeXml {
        public String nodeName;
        public String nodeType;
        public String beanName;
        public String container;
        @JacksonXmlElementWrapper(localName = "children")
        @JacksonXmlProperty(localName = "child")
        public List<BehaviorDefinitionNodeXml> children;
        public BehaviorDefinitionNodeXml condition;
        @JacksonXmlElementWrapper(localName = "strategyMap")
        @JacksonXmlProperty(localName = "entry")
        public List<StrategyEntryXml> strategyEntries;

        BehaviorDefinitionNode toModel() {
            List<BehaviorDefinitionNode> childModels = children == null
                    ? List.of()
                    : children.stream().map(BehaviorDefinitionNodeXml::toModel).collect(Collectors.toList());
            BehaviorDefinitionNode cond = condition == null ? null : condition.toModel();
            Map<String, BehaviorDefinitionNode> map = new LinkedHashMap<>();
            if (strategyEntries != null) {
                for (StrategyEntryXml e : strategyEntries) {
                    map.put(e.key, e.nodeBody.toModel());
                }
            }
            return new BehaviorDefinitionNode(
                    nodeName,
                    NodeType.valueOf(nodeType),
                    beanName,
                    container,
                    childModels,
                    cond,
                    map
            );
        }
    }

    static final class StrategyEntryXml {
        @JacksonXmlProperty(isAttribute = true)
        public String key;
        @JsonUnwrapped
        public BehaviorDefinitionNodeXml nodeBody;
    }
}
