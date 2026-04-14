# 行为树定义解析（IR / Codec / Assembler / Loader）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `behavior-tree-core` 中实现规格所述 **语法层 → 不可变 IR → DefinitionAssembler → `BehaviorNodeWrapper`**，并暴露 **`BehaviorTreeDefinitionLoader`**；在 **`behavior-tree-spring-boot-starter`** 中增加 **可选** 的配置属性与 **Loader Bean**，与现有 `com.lee9213.behavior.spring.EnableBehavior` / `SpringNodeUtil` 正交。

**Architecture:** Jackson 将 JSON/XML 绑定到同一套 **record IR**；**无 Spring** 时使用 **反射按类名** 解析动作；**Spring** 时通过 **`ActionNodeResolver` 的 ApplicationContext 实现** 按 Bean 名解析。装配层仅依赖 **`ActionNodeResolver`** 与 **`Class<?>` result 类型**，不解析字符串。**Parallel** 首版 IR **不表达 executor**，装配时始终使用 `new ParallelNodeImpl(children, null)`（与当前默认「顺序执行」一致）。

**Tech Stack:** Java 17，Maven，Jackson Databind + Jackson XML（`jackson-dataformat-xml`），JUnit 5，Spring Framework 5.3（core 已有）/ Spring Boot 3.2（starter）。

---

## 文件结构（新建 / 修改职责）

| 路径 | 职责 |
|------|------|
| `behavior-tree-core/pom.xml` | 增加 Jackson 依赖（版本与 Spring Boot 3.2 BOM 对齐：在父 POM `dependencyManagement` 中导入 `jackson-bom` **或** 在子模块写死 `2.17.x` 与 Boot 3.2 兼容版本）；**不**在本任务移除 Fastjson（规格非目标）。 |
| `behavior-tree-core/.../definition/DefinitionFormat.java` | `JSON`, `XML` 枚举。 |
| `behavior-tree-core/.../definition/ir/BehaviorDefinitionNode.java` | 单一递归 IR：`nodeName`, `nodeType`, `beanName`, `container`, `children`, `condition`, `strategyMap`（`Map<String, BehaviorDefinitionNode>`，键为策略码字符串）。使用 **`record`** 或全 `final` + 工厂；列表/映射用不可变视图（`List.copyOf` / `Map.copyOf`）。 |
| `behavior-tree-core/.../definition/exception/DefinitionSyntaxException.java` | 语法层失败（包装 `JsonProcessingException` / XML 解析异常）。 |
| `behavior-tree-core/.../definition/exception/DefinitionAssemblyException.java` | 语义装配失败（缺字段、未知 `NodeType`、动作解析失败）。 |
| `behavior-tree-core/.../definition/resolve/ActionNodeResolver.java` | `IActionNode<Result, Context> resolveAction(BehaviorDefinitionNode node, Class<?> resultClazz)`（签名可收紧为与 `BaseContext` 一致泛型，见 Task 5）。 |
| `behavior-tree-core/.../definition/resolve/ReflectionActionNodeResolver.java` | `container` 非 `spring` 或缺省时：按 `beanName` 当 **全限定类名** 反射实例化 `IActionNode`（与旧 JSON 中 `com.lee9213...` 用法一致）。 |
| `behavior-tree-core/.../spring/SpringBeanActionNodeResolver.java` | `container` 为 `spring` 时：从 `ApplicationContext` 按 Bean 名取 `IActionNode`（可委托现有 `SpringNodeUtil.getBehaviorNode`，或直接用 `getBean(name, IActionNode.class)`）。 |
| `behavior-tree-core/.../definition/codec/JsonDefinitionCodec.java` | `String` / `InputStream` + `Charset` → 根 `BehaviorDefinitionNode`；失败抛 `DefinitionSyntaxException`。 |
| `behavior-tree-core/.../definition/codec/XmlDefinitionCodec.java` | 同上；XML 根元素与 JSON **字段名一致**（Jackson `@JacksonXmlRootElement` / `@JacksonXmlProperty`），子节点用 **统一 `<children><child>...</child></children>`** 列表风格，避免与嵌套节点两种风格混用。 |
| `behavior-tree-core/.../definition/assemble/DefinitionAssembler.java` | `assemble(BehaviorDefinitionNode root, Class<? extends NodeResult> resultClazz, ActionNodeResolver resolver)` → `BehaviorNodeWrapper`。 |
| `behavior-tree-core/.../definition/BehaviorTreeDefinitionLoader.java` | 门面：`parse(String, DefinitionFormat, Class<?>)`；`parse(InputStream, Charset, DefinitionFormat, Class<?>)` → codec → assembler。无 Spring 时内部使用 **`ReflectionActionNodeResolver`**。 |
| `behavior-tree-core/src/test/resources/definitions/golden.json` | 与现有 `test.json` 等价内容的黄金文件（可复制自 `behavior-tree-core/src/test/resources/test.json`）。 |
| `behavior-tree-core/src/test/resources/definitions/golden.xml` | 与 `golden.json` **语义等价**的 XML（供契约测试）。 |
| `behavior-tree-core/src/test/java/.../definition/*Test.java` | 单元测试与契约测试。 |
| `behavior-tree-spring-boot-starter/pom.xml` | 依赖 `behavior-tree-core`（若尚未直接依赖 core，需增加以便注册 Resolver Bean）。 |
| `behavior-tree-spring-boot-starter/.../BehaviorDefinitionProperties.java` | `behavior.definition.format`、`location`、`charset`。 |
| `behavior-tree-spring-boot-starter/.../BehaviorDefinitionAutoConfiguration.java` | `@ConditionalOnProperty(prefix="behavior.definition", name="location")`（或 `matchIfMissing=false` 且仅当 `location` 非空时创建）；暴露 `BehaviorTreeDefinitionLoader` 与/或 **已解析的** `BehaviorNodeWrapper` Bean（二选一或两者：计划采用 **Loader Bean** 为主，可选第二个 Bean 名 `behaviorRoot`）。 |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 追加 `BehaviorDefinitionAutoConfiguration` 全限定名（若项目使用 Spring Boot 3 `imports` 文件）。 |
| `README.md`（仓库根） | 破坏性变更：旧 `JsonNodeParser` 已删除；示例改为 `BehaviorTreeDefinitionLoader`。 |
| `CHANGELOG.md` | 若仓库已有则追加条目；若无则 **仅**在任务中创建最小条目（用户未要求可跳过创建，见 Task 12）。 |

---

### Task 1: 父 POM 与 core 引入 Jackson BOM / 依赖

**Files:**
- Modify: `behavior-tree-parent/pom.xml`
- Modify: `behavior-tree-core/pom.xml`

- [ ] **Step 1: 在父 POM `dependencyManagement` 中增加 `jackson-bom`（版本 `2.17.2`，与 Spring Boot 3.2.5 常用栈一致；若冲突以 `mvn dependency:tree` 为准微调）。**

```xml
<dependency>
  <groupId>com.fasterxml.jackson</groupId>
  <artifactId>jackson-bom</artifactId>
  <version>2.17.2</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

- [ ] **Step 2: 在 `behavior-tree-core/pom.xml` 增加（无版本，继承 BOM）：**

```xml
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
</dependency>
<dependency>
  <groupId>com.fasterxml.jackson.dataformat</groupId>
  <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

- [ ] **Step 3: 编译验证**

Run: `mvn -q -pl behavior-tree-core compile`  
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add behavior-tree-parent/pom.xml behavior-tree-core/pom.xml
git commit -m "build: add Jackson BOM and core dependencies for definition codecs"
```

---

### Task 2: IR、`DefinitionFormat`、异常类

**Files:**
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/DefinitionFormat.java`
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/ir/BehaviorDefinitionNode.java`
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/exception/DefinitionSyntaxException.java`
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/exception/DefinitionAssemblyException.java`

- [ ] **Step 1: 编写 `DefinitionFormat`**

```java
package com.lee9213.behavior.definition;

public enum DefinitionFormat {
    JSON,
    XML
}
```

- [ ] **Step 2: 编写 `BehaviorDefinitionNode`（record；字段与旧 `test.json` 对齐）**  
  Task 4 会为同一 record 增加 JSON/XML 的 Jackson 注解；若 Strategy 的 `Map` 在 XML 上无法稳定绑定，按 Task 4「`List<StrategyEntry>`」方案调整本 record 并同步 Assembler。

```java
package com.lee9213.behavior.definition.ir;

import com.lee9213.behavior.tree.enums.NodeType;

import java.util.List;
import java.util.Map;

public record BehaviorDefinitionNode(
        String nodeName,
        NodeType nodeType,
        String beanName,
        String container,
        List<BehaviorDefinitionNode> children,
        BehaviorDefinitionNode condition,
        Map<String, BehaviorDefinitionNode> strategyMap
) {
    public BehaviorDefinitionNode {
        children = children == null ? List.of() : List.copyOf(children);
        strategyMap = strategyMap == null ? Map.of() : Map.copyOf(strategyMap);
    }
}
```

- [ ] **Step 3: 异常类（继承 `RuntimeException`，含 cause 构造器）**

```java
package com.lee9213.behavior.definition.exception;

public class DefinitionSyntaxException extends RuntimeException {
    public DefinitionSyntaxException(String message) { super(message); }
    public DefinitionSyntaxException(String message, Throwable cause) { super(message, cause); }
}
```

```java
package com.lee9213.behavior.definition.exception;

public class DefinitionAssemblyException extends RuntimeException {
    public DefinitionAssemblyException(String message) { super(message); }
    public DefinitionAssemblyException(String message, Throwable cause) { super(message, cause); }
}
```

- [ ] **Step 4: 编译**

Run: `mvn -q -pl behavior-tree-core compile`  
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/definition
git commit -m "feat(definition): add IR record and syntax/assembly exceptions"
```

---

### Task 3: Jackson 反序列化配置（`NodeType`、空集合）

**Files:**
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/codec/DefinitionObjectMappers.java`（或 `JacksonDefinitionMapperFactory.java`）

- [ ] **Step 1: 提供静态工厂 `jsonMapper()` / `xmlMapper()`**，配置：
  - `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false`（便于向前兼容）
  - 注册 `NodeType` 枚举反序列化（默认 **大写枚举名** 与 `test.json` 中 `"Sequence"` 一致；若 JSON 为小写需在 `ObjectMapper` 上启用 `READ_ENUMS_USING_TO_STRING` **或** 自定义模块）

示例（节选）：

```java
public final class DefinitionObjectMappers {
    private DefinitionObjectMappers() {}

    public static ObjectMapper jsonMapper() {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return m;
    }

    public static XmlMapper xmlMapper() {
        XmlMapper m = new XmlMapper();
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return m;
    }
}
```

- [ ] **Step 2: 在 Task 4 编写 codec 集成测试时**，若 `NodeType` 反序列化失败，将 `BehaviorDefinitionNode` 的 `nodeType` 改为 **`String` 在 assembler 中 `NodeType.valueOf`** 或添加 `@JsonCreator` —— **本计划锁定**：保持 `NodeType` 类型，JSON 值为 **`Sequence` 等与枚举常量名一致**（与现有 `test.json` 一致）。

- [ ] **Step 3: Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/definition/codec/DefinitionObjectMappers.java
git commit -m "feat(definition): shared Jackson JSON/XML mapper configuration"
```

---

### Task 4: `JsonDefinitionCodec` 与 `XmlDefinitionCodec`

**Files:**
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/codec/JsonDefinitionCodec.java`
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/codec/XmlDefinitionCodec.java`
- Create: `behavior-tree-core/src/test/java/com/lee9213/behavior/definition/codec/JsonDefinitionCodecTest.java`

- [ ] **Step 1: JSON codec**

```java
package com.lee9213.behavior.definition.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lee9213.behavior.definition.exception.DefinitionSyntaxException;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class JsonDefinitionCodec {

    private static final ObjectMapper MAPPER = DefinitionObjectMappers.jsonMapper();

    private JsonDefinitionCodec() {}

    public static BehaviorDefinitionNode readTree(String content) {
        try {
            return MAPPER.readValue(content, BehaviorDefinitionNode.class);
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition JSON", e);
        }
    }

    public static BehaviorDefinitionNode readTree(InputStream in, Charset charset) {
        try (java.io.Reader reader = new java.io.InputStreamReader(in, charset)) {
            return MAPPER.readValue(reader, BehaviorDefinitionNode.class);
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition JSON", e);
        }
    }
}
```

- [ ] **Step 2: 将 `test.json` 复制为 `src/test/resources/definitions/golden.json`**

Run: `cp behavior-tree-core/src/test/resources/test.json behavior-tree-core/src/test/resources/definitions/golden.json`

- [ ] **Step 3: 编写 `JsonDefinitionCodecTest`：加载 `golden.json`，断言根 `nodeName` 为 `RootNodeTest1`、`nodeType` 为 `Sequence`。**

- [ ] **Step 4: 运行测试**

Run: `mvn -q -pl behavior-tree-core test -Dtest=JsonDefinitionCodecTest`  
Expected: `BUILD SUCCESS`

- [ ] **Step 5: 在 `BehaviorDefinitionNode` 上增加 Jackson XML 注解（与 JSON 共用同一 record）**

在 `record` 上增加（节选，按字段补全）：

```java
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "behavior")
public record BehaviorDefinitionNode(
        @JacksonXmlProperty String nodeName,
        @JacksonXmlProperty NodeType nodeType,
        @JacksonXmlProperty String beanName,
        @JacksonXmlProperty String container,
        @JacksonXmlElementWrapper(localName = "children") @JacksonXmlProperty(localName = "child")
        List<BehaviorDefinitionNode> children,
        @JacksonXmlProperty BehaviorDefinitionNode condition,
        @JacksonXmlProperty Map<String, BehaviorDefinitionNode> strategyMap
) { /* compact constructor 同 Task 2 */ }
```

**策略节点 XML：** `strategyMap` 用 `<strategyMap><entry key="A">...</entry></strategyMap>` 需 Jackson `Map` 序列化支持；若默认映射失败，在首版 **将 Strategy 的 map 拆为 `List<StrategyEntry>`**（`record StrategyEntry(String key, BehaviorDefinitionNode node)`）并在 Assembler 中转 `Map`，避免阻塞实现。

- [ ] **Step 5b: `XmlDefinitionCodec`**

```java
package com.lee9213.behavior.definition.codec;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.lee9213.behavior.definition.exception.DefinitionSyntaxException;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public final class XmlDefinitionCodec {

    private static final XmlMapper MAPPER = DefinitionObjectMappers.xmlMapper();

    private XmlDefinitionCodec() {}

    public static BehaviorDefinitionNode readTree(String content) {
        try {
            return MAPPER.readValue(content, BehaviorDefinitionNode.class);
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition XML", e);
        }
    }

    public static BehaviorDefinitionNode readTree(InputStream in, Charset charset) {
        try (InputStreamReader reader = new InputStreamReader(in, charset)) {
            return MAPPER.readValue(reader, BehaviorDefinitionNode.class);
        } catch (IOException e) {
            throw new DefinitionSyntaxException("Invalid behavior definition XML", e);
        }
    }
}
```

- [ ] **Step 5c: 手写 `golden.xml`（与 `golden.json` 等价）并编写 `XmlDefinitionCodecTest`**

- [ ] **Step 6: Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/definition/codec \
  behavior-tree-core/src/test/resources/definitions/golden.json \
  behavior-tree-core/src/test/java/com/lee9213/behavior/definition/codec
git commit -m "feat(definition): JSON and XML codecs for BehaviorDefinitionNode"
```

---

### Task 5: `ActionNodeResolver` 实现（反射 + Spring）

**Files:**
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/resolve/ActionNodeResolver.java`
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/resolve/ReflectionActionNodeResolver.java`
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/spring/SpringBeanActionNodeResolver.java`
- Test: `behavior-tree-core/src/test/java/com/lee9213/behavior/definition/resolve/ReflectionActionNodeResolverTest.java`

- [ ] **Step 1: 接口**

```java
package com.lee9213.behavior.definition.resolve;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.tree.node.IActionNode;

public interface ActionNodeResolver {
    <Result extends NodeResult, Context extends BaseContext>
    IActionNode<Result, Context> resolveAction(BehaviorDefinitionNode node, Class<Result> resultClass);
}
```

- [ ] **Step 2: `ReflectionActionNodeResolver`**：`beanName` 非空时 `Class.forName(beanName).getDeclaredConstructor().newInstance()`，强制转换为 `IActionNode`；失败抛 `DefinitionAssemblyException`。

- [ ] **Step 3: `SpringBeanActionNodeResolver`**：仅当 `"spring".equalsIgnoreCase(node.container())` 时委托 `SpringNodeUtil.getBehaviorNode(node.beanName())`；否则抛 `DefinitionAssemblyException`（或文档说明「非 spring 不得走此 Resolver」）。

- [ ] **Step 4: 单元测试 `ReflectionActionNodeResolverTest`**：解析 `com.lee9213.behavior.tree.node.impl.SuccessActionNodeImpl` 全限定名，断言非空。

- [ ] **Step 5: Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/definition/resolve \
  behavior-tree-core/src/main/java/com/lee9213/behavior/spring/SpringBeanActionNodeResolver.java \
  behavior-tree-core/src/test/java/com/lee9213/behavior/definition/resolve
git commit -m "feat(definition): ActionNodeResolver for reflection and Spring beans"
```

---

### Task 6: `DefinitionAssembler`

**Files:**
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/assemble/DefinitionAssembler.java`
- Test: `behavior-tree-core/src/test/java/com/lee9213/behavior/definition/assemble/DefinitionAssemblerTest.java`

- [ ] **Step 1: 实现递归装配**  
  - `Sequence` → `new SequenceNodeImpl<>(childWrappers)`  
  - `Selector` / `Random` / `Parallel` 同理；**Parallel** 使用 `new ParallelNodeImpl<>(childWrappers, null)`  
  - `Strategy`：`condition` 与 `strategyMap` 转 `StrategyNodeImpl`；**策略键**：`resultClass.getDeclaredField` / 反射查找与 `NodeResult` 子类中 **同名静态常量**（与旧逻辑一致），或规格允许的 **`resultClass` 上静态工厂 `fromCode(String)`**（若不存在则 **仅支持 `TestNodeResult.A/B` 模式**：通过 `Enum.valueOf` 不适用；**本计划锁定**：使用反射在 `resultClass` 上找 `public static final Result X` 匹配字符串键，找不到抛 `DefinitionAssemblyException`）。  
  - `Action`：调用 `ActionNodeResolver.resolveAction`。

- [ ] **Step 2: 测试**  
  使用 `ReflectionActionNodeResolver` + 小型 IR（单 `Action` 指向 `SuccessActionNodeImpl`），断言 `execute` 返回 `SUCCESS`（可用 `TestContext`）。

- [ ] **Step 3: Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/definition/assemble \
  behavior-tree-core/src/test/java/com/lee9213/behavior/definition/assemble
git commit -m "feat(definition): DefinitionAssembler for all NodeTypes"
```

---

### Task 7: `BehaviorTreeDefinitionLoader`

**Files:**
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/BehaviorTreeDefinitionLoader.java`
- Test: `behavior-tree-core/src/test/java/com/lee9213/behavior/definition/BehaviorTreeDefinitionLoaderTest.java`

- [ ] **Step 1: 门面类**

```java
package com.lee9213.behavior.definition;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.definition.assemble.DefinitionAssembler;
import com.lee9213.behavior.definition.codec.JsonDefinitionCodec;
import com.lee9213.behavior.definition.codec.XmlDefinitionCodec;
import com.lee9213.behavior.definition.ir.BehaviorDefinitionNode;
import com.lee9213.behavior.definition.resolve.ActionNodeResolver;
import com.lee9213.behavior.definition.resolve.ReflectionActionNodeResolver;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class BehaviorTreeDefinitionLoader {

    private final ActionNodeResolver resolver;

    public BehaviorTreeDefinitionLoader(ActionNodeResolver resolver) {
        this.resolver = resolver;
    }

    public BehaviorTreeDefinitionLoader() {
        this(new ReflectionActionNodeResolver());
    }

    public <R extends NodeResult> BehaviorNodeWrapper<R, ?> parse(
            String content,
            DefinitionFormat format,
            Class<R> resultClass) {
        BehaviorDefinitionNode root = switch (format) {
            case JSON -> JsonDefinitionCodec.readTree(content);
            case XML -> XmlDefinitionCodec.readTree(content);
        };
        return DefinitionAssembler.assemble(root, resultClass, resolver);
    }

    public <R extends NodeResult> BehaviorNodeWrapper<R, ?> parse(
            InputStream in,
            Charset charset,
            DefinitionFormat format,
            Class<R> resultClass) {
        BehaviorDefinitionNode root = switch (format) {
            case JSON -> JsonDefinitionCodec.readTree(in, charset);
            case XML -> XmlDefinitionCodec.readTree(in, charset);
        };
        return DefinitionAssembler.assemble(root, resultClass, resolver);
    }
}
```

**注意：** `BehaviorNodeWrapper` 第二泛型若需 `BaseContext` 子类，Assembler 返回类型可具体化为 `BehaviorNodeWrapper<R, TestContext>` 测试侧再收窄；若编译推断困难，将 `assemble` 返回 `BehaviorNodeWrapper<R, ? extends BaseContext>`。

- [ ] **Step 2: 契约测试 `LoaderEquivalenceTest`**：同一 `golden.json` 与 `golden.xml` 经 Loader 装配后，比较 **节点类型序列** 或 **自定义 `visit` 遍历** 断言结构一致。

- [ ] **Step 3: Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/definition/BehaviorTreeDefinitionLoader.java \
  behavior-tree-core/src/test/java/com/lee9213/behavior/definition
git commit -m "feat(definition): BehaviorTreeDefinitionLoader facade"
```

---

### Task 8: 组合解析器（可选）与 `BehaviorTreeTest` 迁移

**Files:**
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/resolve/CompositeActionNodeResolver.java`（可选）
- Modify: `behavior-tree-core/src/test/java/com/lee9213/behavior/BehaviorTreeTest.java`

- [ ] **Step 1: 实现 `CompositeActionNodeResolver`**：`spring` → `SpringBeanActionNodeResolver`，否则 → `ReflectionActionNodeResolver`（仅在 Spring 测试上下文中有 `ApplicationContext` 时注册第二段）。

- [ ] **Step 2: 将 `BehaviorTreeTest` 改为**：`new BehaviorTreeDefinitionLoader(compositeOrSpringResolver).parse(..., DefinitionFormat.JSON, TestNodeResult.class)` 加载 `classpath:definitions/golden.json`，`execute` 断言与当前手工树一致（失败/成功日志与行为一致）。

- [ ] **Step 3: 运行**

Run: `mvn -q -pl behavior-tree-core test`  
Expected: 全部通过

- [ ] **Step 4: Commit**

```bash
git add behavior-tree-core/src/test/java/com/lee9213/behavior/BehaviorTreeTest.java \
  behavior-tree-core/src/main/java/com/lee9213/behavior/definition/resolve/CompositeActionNodeResolver.java
git commit -m "test: load behavior tree via BehaviorTreeDefinitionLoader in BehaviorTreeTest"
```

---

### Task 9: Spring Boot — `BehaviorDefinitionProperties` 与自动配置

**Files:**
- Modify: `behavior-tree-spring-boot-starter/pom.xml`
- Create: `behavior-tree-spring-boot-starter/src/main/java/com/lee9213/behavior/spring/BehaviorDefinitionProperties.java`
- Create: `behavior-tree-spring-boot-starter/src/main/java/com/lee9213/behavior/spring/BehaviorDefinitionAutoConfiguration.java`
- Modify: `behavior-tree-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（若不存在则创建）

- [ ] **Step 1: starter 增加对 `behavior-tree-core` 的依赖**

```xml
<dependency>
  <groupId>com.lee9213.behavior</groupId>
  <artifactId>behavior-tree-core</artifactId>
</dependency>
```

- [ ] **Step 2: `BehaviorDefinitionProperties`**

```java
package com.lee9213.behavior.spring;

import com.lee9213.behavior.definition.DefinitionFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "behavior.definition")
public class BehaviorDefinitionProperties {

    private DefinitionFormat format = DefinitionFormat.JSON;
    private String location = "";
    private Charset charset = StandardCharsets.UTF_8;

    public DefinitionFormat getFormat() { return format; }
    public void setFormat(DefinitionFormat format) { this.format = format; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Charset getCharset() { return charset; }
    public void setCharset(Charset charset) { this.charset = charset; }
}
```

**注意：** Spring Boot 绑定 `format` 为字符串时需 **自定义 `Converter<String, DefinitionFormat>`** 或属性类型改为 `String` 再在 `@Bean` 中 `DefinitionFormat.valueOf`。计划中在 `@Bean` 方法内解析字符串。

- [ ] **Step 3: `BehaviorDefinitionAutoConfiguration`**

  - `@ConditionalOnProperty(prefix = "behavior.definition", name = "location", matchIfMissing = false)`  
  - `@Bean BehaviorTreeDefinitionLoader behaviorTreeDefinitionLoader(ApplicationContext ctx)`：内部 `SpringBeanActionNodeResolver` + `ReflectionActionNodeResolver` 组合（与 Task 8 相同策略）。

- [ ] **Step 4: 注册自动配置类到 `imports` 文件**

```
com.lee9213.behavior.spring.BehaviorDefinitionAutoConfiguration
```

- [ ] **Step 5: 在 `behavior-tree-spring-boot-starter/src/test/java` 增加 `@SpringBootTest` 切片测试**（`properties = "behavior.definition.location=classpath:..."`），断言 Loader Bean 存在。**若模块暂无测试依赖**，在 `pom.xml` 增加 `spring-boot-starter-test` `scope=test`。

- [ ] **Step 6: Commit**

```bash
git add behavior-tree-spring-boot-starter
git commit -m "feat(spring-boot): optional BehaviorDefinitionProperties and Loader autoconfiguration"
```

---

### Task 10: 文档

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 记录破坏性变更**：旧 `com.lee9213.behavior.parser` 已移除；示例代码使用 `BehaviorTreeDefinitionLoader` + `golden.json` 片段；Spring 配置 `behavior.definition.location` / `format` / `charset`。

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: document BehaviorTreeDefinitionLoader and Spring properties"
```

---

### Task 11: 全量验证

- [ ] **Step 1: 运行**

Run: `mvn -q test`  
Expected: 全模块 `BUILD SUCCESS`

- [ ] **Step 2: （可选）提交**

```bash
git commit --allow-empty -m "chore: verify full build after definition loader feature"
```

---

## Self-review（计划作者自检）

**1. Spec coverage**

| 规格章节 | 对应 Task |
|----------|-----------|
| §2 IR + 分层依赖 | Task 2, 4–7 |
| §3 不可变 IR | Task 2 |
| §4 JSON/XML + 格式枚举 | Task 1–4 |
| §5 Assembler + Parallel 无 executor 首版 | Task 6 |
| §6 Loader + 异常类型 | Task 2, 7 |
| §7 Spring 属性 + Loader Bean + 与 EnableBehavior 正交 | Task 9 |
| §8 README | Task 10 |
| §9 测试策略 | Task 3–8, 9 |

**2. Placeholder 扫描：** 无 `TBD` / 空「实现细节」步骤；策略键解析在 Task 6 以「反射查找静态字段」为具体手段，若实现时改为显式注册表，须在 **同一任务** 内替换为完整代码并更新测试。

**3. Type consistency：** `BehaviorDefinitionNode`、`DefinitionFormat`、`BehaviorTreeDefinitionLoader`、`DefinitionSyntaxException`、`DefinitionAssemblyException`、`ActionNodeResolver`、`DefinitionAssembler` 全文一致；Loader 泛型与 `DefinitionAssembler.assemble` 签名须在编码时统一为同一组 `Class<R extends NodeResult>`。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-11-behavior-tree-definition-loader.md`. Two execution options:

**1. Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration. **REQUIRED SUB-SKILL:** `superpowers:subagent-driven-development`.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints. **REQUIRED SUB-SKILL:** `superpowers:executing-plans`.

Which approach?
