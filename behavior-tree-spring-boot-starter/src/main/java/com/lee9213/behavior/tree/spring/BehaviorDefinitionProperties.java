package com.lee9213.behavior.tree.spring;

import com.lee9213.behavior.tree.definition.DefinitionFormat;
import com.lee9213.behavior.tree.definition.BehaviorTreeDefinitionLoader;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 行为树定义资源（可选）：由应用通过 {@link org.springframework.core.io.Resource} 自行加载时仅需
 * {@link BehaviorTreeDefinitionLoader} Bean；本属性用于声明式配置位置与格式。
 */
@ConfigurationProperties(prefix = "behavior.definition")
public class BehaviorDefinitionProperties {

    /**
     * 定义文件位置（Spring {@code Resource} 语义），例如 {@code classpath:definitions/golden.json}。
     */
    private String location = "";

    private DefinitionFormat format = DefinitionFormat.JSON;

    private Charset charset = StandardCharsets.UTF_8;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public DefinitionFormat getFormat() {
        return format;
    }

    public void setFormat(DefinitionFormat format) {
        this.format = format;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
