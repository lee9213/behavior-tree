# behavior-tree

基于 Java 的行为树（Behavior Tree）实现，支持 JSON/XML 定义加载、Spring 集成与可选的流程引擎（持久化快照、重试）。

- **JDK**：17  
- **构建**：`mvn test`  
- **坐标**：`com.lee9213.behavior:behavior-tree-parent:1.0-SNAPSHOT`（多模块 reactor）

---

## 模块

| 模块 | 职责 |
|------|------|
| **behavior-tree-core** | 行为树内核：节点实现、执行上下文、`BehaviorTree#execute`；定义加载 `BehaviorTreeDefinitionLoader`（JSON/XML → 中间表示 → 装配）；并行 `ParallelNodeImpl`；重试 `RetryExecutor` / `RetryPolicy` / `RetryPolicyRegistry`；叶子基类 `AbstractActionNode` 等。 |
| **behavior-tree-engine** | 流程定义 `FlowDefinition`、校验、`FlowEngine` / `FlowEngineConfig`、内存 `ProcessInstanceStore`。 |
| **behavior-tree-spring-boot-starter** | Spring Boot 3.x：默认 `BehaviorTreeDefinitionLoader` Bean（可与 `@EnableBehavior` 配合）；`FlowEngine` 自动配置；存在 `StringRedisTemplate` 时注册 `RedisProcessInstanceStore`。**无 Redis 时不会静默降级为内存 store**，需自行装配持久化实现。 |

---

## 快速开始（编程式）

1. 使用 `BehaviorTreeDefinitionLoader` 解析 JSON 或 XML（指定 `DefinitionFormat` 与结果类型 `Class<? extends NodeResult>`），得到根节点 `BehaviorNodeWrapper`。  
2. 构造 `BehaviorTree`，调用 `execute(context)` 执行。

动作节点解析：

- 定义中 `container=spring` 时，通过 Spring 按 Bean 名解析 `IActionNode`（需 `@EnableBehavior` 等以注册 `SpringNodeUtil`）。  
- 否则按 `beanName` 当作全限定类名，反射 `newInstance`。

完整示例见测试：`com.lee9213.behavior.BehaviorTreeTest`，定义文件：`behavior-tree-core/src/test/resources/definitions/golden.json`。

---

## Spring Boot

- **`@EnableBehavior`**（`com.lee9213.behavior.spring`）：启用 Spring 侧动作解析能力。  
- **Starter** 提供默认 **`BehaviorTreeDefinitionLoader`** Bean（`CompositeActionNodeResolver`：Spring Bean + 反射类名）。

可选配置 **`behavior.definition`**（声明式描述资源位置与编码；自动配置只注册 Loader，不强制从 `location` 解析根节点，以免缺少 `resultClass` 等元数据）：

| 属性 | 说明 |
|------|------|
| `behavior.definition.location` | 资源位置，如 `classpath:definitions/golden.json` |
| `behavior.definition.format` | `JSON` 或 `XML` |
| `behavior.definition.charset` | 字符集，默认 UTF-8 |

流程引擎（**`behavior.flow`**）：

| 属性 | 说明 |
|------|------|
| `behavior.flow.retry-enabled` | 是否启用重试（默认 `true`） |
| `behavior.flow.redis-entry-ttl` | Redis 中流程快照 TTL；未设置或非正数表示不过期 |

---

## 节点类型

### 控制节点

| 类型 | 行为 |
|------|------|
| **Sequence** | 顺序执行子节点；当前子节点成功则继续，失败则中断并向上返回失败。 |
| **Selector** | 依次尝试子节点；任一子节点成功即停止并返回成功，全部失败则返回失败。 |
| **Parallel** | 默认同线程顺序执行子节点；使用 `buildParallelNode(name, children, executor)` 且 `executor` 非空时，在线程池上并发执行。**失败不会取消兄弟任务**；全部结束后按合取规则汇总结果。 |
| **Random** | 随机选一个子节点执行。 |

### 装饰节点

| 类型 | 行为 |
|------|------|
| **Strategy** | 根据 condition 的执行结果，从 strategy 子节点中选择一个执行。 |

### 动作节点

实现 **`IActionNode`**（或通过 **`AbstractActionNode`** 接入重试策略），表示具体业务动作。

---

## 流程引擎要点

- **持久化**：`FlowEngine.run` 在配置了 store 时会先 **load** 已有快照；若快照存在且 `definitionId` / `definitionVersion` 与当前 `FlowDefinition` 不一致，则 **fail-closed**（抛出 `StoreException`）。  
- **重试**：core 包 `com.lee9213.behavior.retry` 提供通用重试抽象；叶子可继承 `AbstractActionNode` 并传入重试开关与 `RetryPolicy`。`FlowEngineConfig` 持有注册表供引擎侧使用。

设计说明见：`docs/superpowers/specs/2026-04-11-behavior-flow-engine-design.md`。

---

## 破坏性变更（定义加载）

旧包 **`com.lee9213.behavior.parser`** 已移除，请统一使用 **`BehaviorTreeDefinitionLoader`** 加载定义。
