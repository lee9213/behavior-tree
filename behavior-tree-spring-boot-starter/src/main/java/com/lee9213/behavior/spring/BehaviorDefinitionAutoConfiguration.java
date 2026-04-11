package com.lee9213.behavior.spring;

import com.lee9213.behavior.definition.BehaviorTreeDefinitionLoader;
import com.lee9213.behavior.definition.resolve.CompositeActionNodeResolver;
import com.lee9213.behavior.definition.resolve.ReflectionActionNodeResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 注册默认的 {@link BehaviorTreeDefinitionLoader}（Spring Bean + 反射类名组合解析动作）。
 * 与 {@link EnableBehavior} 正交：需同时启用后者以便 {@code container=spring} 动作从容器解析。
 */
@AutoConfiguration
@EnableConfigurationProperties(BehaviorDefinitionProperties.class)
public class BehaviorDefinitionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    BehaviorTreeDefinitionLoader behaviorTreeDefinitionLoader() {
        return new BehaviorTreeDefinitionLoader(
                new CompositeActionNodeResolver(
                        new SpringBeanActionNodeResolver(),
                        new ReflectionActionNodeResolver()));
    }
}
