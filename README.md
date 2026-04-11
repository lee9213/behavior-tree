# behavior-tree
行为树

## 模块（Maven reactor）

| 模块 | 说明 |
|------|------|
| `behavior-tree-core` | 行为树内核、JSON/XML 解析、`FlowExecutionContext`、`ConcurrentParallelNodeImpl` 等 |
| `behavior-tree-engine` | 流程定义校验、`FlowEngine`、重试策略 SPI、内存 `ProcessInstanceStore` |
| `behavior-tree-spring-boot-starter` | Spring Boot 3.x：`RedisProcessInstanceStore`、自动配置（无 Redis 时不静默降级为内存，需自行装配 store） |

构建：`mvn test`（JDK 17）。

### 流程引擎与规格对齐说明

- **持久化**：`FlowEngine.run` 在配置了 store 时会先 **load** 已有快照；若存在且 `definitionId`/`definitionVersion` 与当前 `FlowDefinition` 不一致则 **fail-closed**（`StoreException`）。
- **重试**：`RetryExecutor` + `RetryPolicyRegistry` 已提供；**自动套在每条叶子 `execute` 上** 需业务侧装饰节点或后续 `RetryingINodeDecorator`（见实现计划任务 9）。
- 设计文档：`docs/superpowers/specs/2026-04-11-behavior-flow-engine-design.md`。

# 控制节点
## Sequence
顺序节点，依次执行所有子节点，若当前子节点返回成功，则继续执行下一个子节点；若子当前节点返回失败，则中断后续子节点的执行，并把结果返回给父节点。
## Selector
选择节点，依次执行所有子节点，若当前子节点返回成功，则中断后续节点运行，并把结果返回给父节点
## Parallel
并行节点接口，依次执行所有子节点，无论失败与否，都会把所有子节点执行一遍
## Random
随机节点，随机选择一个子节点来运行。

# 装饰节点
## Strategy
策略节点，策略执行，根据condition节点执行结果，从strategy节点中选择一个执行。

# 动作节点
## Action
行为节点接口，具体执行某个行为的节点都需要实现该接口

# 具体功能
支持json文件解析
支持spring容器的动作节点。


# 示例
1. 创建一个行为树，并添加根节点，具体见com.lee9213.behavior.BehaviorTreeTest。
2. 执行根节点，并返回执行结果