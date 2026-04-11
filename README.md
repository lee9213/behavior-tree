# behavior-tree
行为树

## 模块（Maven reactor）

| 模块 | 说明 |
|------|------|
| `behavior-tree-core` | 行为树内核；定义加载（`com.lee9213.behavior.definition.BehaviorTreeDefinitionLoader`，JSON/XML → IR → 装配）；`FlowExecutionContext`、`ParallelNodeImpl`；重试（`RetryExecutor`、`RetryPolicy`、`RetryPolicyRegistry`）；叶子抽象 `AbstractActionNode` 等 |
| `behavior-tree-engine` | 流程定义校验、`FlowEngine`、`FlowEngineConfig`、内存 `ProcessInstanceStore` 等 |
| `behavior-tree-spring-boot-starter` | Spring Boot 3.x：`BehaviorTreeDefinitionLoader` 默认 Bean（可与 `@EnableBehavior` 组合）；`RedisProcessInstanceStore`（`behavior.flow.redis-entry-ttl`）；流程引擎自动配置（无 Redis 时不静默降级为内存，需自行装配 store） |

构建：`mvn test`（JDK 17）。

### 流程引擎与规格对齐说明

- **持久化**：`FlowEngine.run` 在配置了 store 时会先 **load** 已有快照；若存在且 `definitionId`/`definitionVersion` 与当前 `FlowDefinition` 不一致则 **fail-closed**（`StoreException`）。
- **重试（core）**：`com.lee9213.behavior.retry` 提供 `RetryExecutor`、`RetryPolicy`、`RetryPolicyRegistry`；叶子可继承 `com.lee9213.behavior.action.AbstractActionNode`（构造参数传入是否启用重试与 `RetryPolicy`）。`FlowEngineConfig` 仍持有注册表供引擎侧使用。全局与节点级组合策略可后续扩展。
- 设计文档：`docs/superpowers/specs/2026-04-11-behavior-flow-engine-design.md`。

# 控制节点
## Sequence
顺序节点，依次执行所有子节点，若当前子节点返回成功，则继续执行下一个子节点；若子当前节点返回失败，则中断后续子节点的执行，并把结果返回给父节点。
## Selector
选择节点，依次执行所有子节点，若当前子节点返回成功，则中断后续节点运行，并把结果返回给父节点
## Parallel
并行节点：默认顺序执行所有子节点；使用 `buildParallelNode(name, children, executor)` 且 `executor` 非空时，在线程池上并发执行子节点（失败不取消兄弟任务，全部结束后合取结果）。
## Random
随机节点，随机选择一个子节点来运行。

# 装饰节点
## Strategy
策略节点，策略执行，根据condition节点执行结果，从strategy节点中选择一个执行。

# 动作节点
## Action
行为节点接口，具体执行某个行为的节点都需要实现该接口

# 定义加载（破坏性变更说明）

- 旧包 `com.lee9213.behavior.parser` 已移除。
- 使用 **`BehaviorTreeDefinitionLoader`**：传入 JSON 或 XML 字符串/流、`DefinitionFormat` 与 `Class<? extends NodeResult>`，得到根 `BehaviorNodeWrapper`（动作解析：`container=spring` 时从 Spring 容器按 Bean 名取 `IActionNode`，否则按 `beanName` 全限定类名反射 `newInstance`）。
- Spring：`@EnableBehavior`（`com.lee9213.behavior.spring`）注册 `SpringNodeUtil`；**starter** 另提供默认 **`BehaviorTreeDefinitionLoader` Bean**（`CompositeActionNodeResolver`）。可选配置前缀 **`behavior.definition`**：`location`、`format`（`JSON`/`XML`）、`charset`（用于应用侧自行用 `Resource` 读入定义时使用；自动装配仅注册 Loader，不强制从 `location` 解析根节点，以免缺少 `resultClass` 元数据）。

# 示例

1. 从 classpath 读取 JSON 定义并执行：见 `com.lee9213.behavior.BehaviorTreeTest`（`definitions/golden.json`）。
2. 执行根节点并返回结果：`BehaviorTree#execute`。