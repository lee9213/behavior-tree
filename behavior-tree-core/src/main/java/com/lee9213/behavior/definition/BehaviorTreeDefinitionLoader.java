package com.lee9213.behavior.definition;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.definition.assemble.DefinitionAssembler;
import com.lee9213.behavior.definition.codec.JsonDefinitionCodec;
import com.lee9213.behavior.definition.codec.XmlDefinitionCodec;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.definition.resolve.ActionNodeResolver;
import com.lee9213.behavior.definition.resolve.ReflectionActionNodeResolver;
import com.lee9213.behavior.node.INode;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 定义文本 → 运行时根节点：先语法 codec，再 {@link DefinitionAssembler}。
 */
public final class BehaviorTreeDefinitionLoader {

    private final ActionNodeResolver resolver;

    public BehaviorTreeDefinitionLoader(ActionNodeResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 无 Spring：仅支持反射类名动作引用。
     */
    public BehaviorTreeDefinitionLoader() {
        this(new ReflectionActionNodeResolver());
    }

    public <R extends NodeResult> INode<R, BaseContext> parse(
            String content,
            DefinitionFormat format,
            Class<R> resultClass) {
        BehaviorDefinitionNode root = readRoot(content, format);
        return DefinitionAssembler.assemble(root, resultClass, resolver);
    }

    public <R extends NodeResult> INode<R, BaseContext> parse(
            InputStream in,
            Charset charset,
            DefinitionFormat format,
            Class<R> resultClass) {
        BehaviorDefinitionNode root = readRoot(in, charset, format);
        return DefinitionAssembler.assemble(root, resultClass, resolver);
    }

    private static BehaviorDefinitionNode readRoot(String content, DefinitionFormat format) {
        return switch (format) {
            case JSON -> JsonDefinitionCodec.readTree(content);
            case XML -> XmlDefinitionCodec.readTree(content);
        };
    }

    private static BehaviorDefinitionNode readRoot(InputStream in, Charset charset, DefinitionFormat format) {
        return switch (format) {
            case JSON -> JsonDefinitionCodec.readTree(in, charset);
            case XML -> XmlDefinitionCodec.readTree(in, charset);
        };
    }

    /**
     * 便捷重载：默认 UTF-8。
     */
    public <R extends NodeResult> INode<R, BaseContext> parse(
            InputStream in,
            DefinitionFormat format,
            Class<R> resultClass) {
        return parse(in, StandardCharsets.UTF_8, format, resultClass);
    }
}
