# 行为流引擎实现计划

> **面向代理执行者：** 必须配合子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans，**按任务逐步**落实本计划。步骤使用复选框（`- [ ]`）语法便于跟踪。

**目标：** 在现有 behavior-tree 库之上增加流程引擎层：同步阻塞式 `run`；可选用 Redis 或内存的 `ProcessInstanceStore`；引擎级重试总开关；按标签的重试策略（指数退避 + 抖动）；通过注入的 `Executor` 实现 Parallel **真并发**，子分支失败时**不**取消兄弟任务；v1 定义保持纯树，并做防御性无环校验。

**架构：** 将仓库改为 **Maven 多模块 reactor**（`behavior-tree-core` = 现有库，**JDK 17**；`behavior-tree-engine` = `FlowDefinition`、`FlowEngine`、重试、存储 SPI + 内存、`ConcurrentParallelNodeImpl`、校验；`behavior-tree-spring-boot-starter` = Spring Boot 3 / Spring 6 自动配置 `Executor`、基于 Redis 的存储与引擎 Bean）。执行仍以 **行为树** 为驱动：引擎在 `BehaviorTree.execute` 外包一层，使用支持 **分支级上下文拷贝** 的 `FlowExecutionContext`，以保证并行安全。

**技术栈：** Maven 3.9+，JDK 17，JUnit 5，Lombok（版本在父 POM 对齐）；core 中沿用现有 Guava / log4j / fastjson；engine 尽量少加依赖；starter 模块使用 Spring Boot 3.2.x（Spring Framework 6.x）+ `spring-boot-starter-data-redis`（Lettuce）；Redis 载荷 JSON 序列化使用 **Jackson**（在 starter 中引入，必要时 engine 测试 scope）。

**上下文：** 建议在 **独立 git worktree** 中实现（见 superpowers:using-git-worktrees），reactor 重构在全部变绿前保持隔离。

---

## 目标文件结构

| 路径 | 职责 |
|------|------|
| `pom.xml` | 父聚合：`dependencyManagement`、JDK 17、插件版本、`<modules>` |
| `behavior-tree-core/pom.xml` | 原 behavior-tree 构件（坐标可改为 `behavior-tree-core`—见任务 1 说明） |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/**` | 现有代码 **除下列小改外保持不变**（`BehaviorNodeWrapper`、新并行实现） |
| `behavior-tree-engine/pom.xml` | 依赖 `behavior-tree-core` |
| `behavior-tree-engine/.../FlowDefinition.java` | 不可变：id、version、`BehaviorTree` 根 |
| `behavior-tree-engine/.../FlowInstanceSnapshot.java` | 可序列化、供存储使用的实例状态 |
| `behavior-tree-engine/.../FlowEngineConfig.java` | `retryEnabled`、`Executor`、`ProcessInstanceStore`、`RetryPolicyRegistry`、超时等 |
| `behavior-tree-core/.../flow/FlowExecutionContext.java` | 继承 `BaseContext`；`copyForParallelBranch()` 用于线程隔离（**必须放在 core**，避免 `ConcurrentParallelNodeImpl` 依赖 engine） |
| `behavior-tree-engine/.../FlowEngine.java` | `run(instanceId, FlowDefinition, FlowExecutionContext initialContext)` |
| `behavior-tree-engine/.../retry/RetryPolicy.java` | 最大次数、基础延迟、倍数、抖动比例 |
| `behavior-tree-engine/.../retry/RetryPolicyRegistry.java` | 按步骤 tag / 类型键解析策略 |
| `behavior-tree-engine/.../retry/RetryExecutor.java` | 执行策略 + 休眠；尊重 `retryEnabled` |
| `behavior-tree-engine/.../store/ProcessInstanceStore.java` | load / save / delete |
| `behavior-tree-engine/.../store/InMemoryProcessInstanceStore.java` | `ConcurrentHashMap` |
| `behavior-tree-engine/.../store/StoreException.java` | 受检或非受检—任选一种，全文一致 |
| `behavior-tree-engine/.../validation/FlowDefinitionValidator.java` | 结构检查 + `ExecutionGraphSanityChecker`（树 ⇒ DAG） |
| `behavior-tree-core/.../ParallelNodeImpl.java` | 保持兼容，行为不变 |
| `behavior-tree-core/.../ConcurrentParallelNodeImpl.java` | 新增：`Executor` + 子节点；汇合语义对齐规格 §4 |
| `behavior-tree-spring-boot-starter/pom.xml` | 依赖 engine + spring-boot-starter-data-redis |
| `behavior-tree-spring-boot-starter/.../BehaviorFlowAutoConfiguration.java` | `@AutoConfiguration`、Bean 定义 |
| `behavior-tree-engine/src/test/java/**` | JUnit 5 测试 |

---

## 与规格章节的对应关系

| 规格章节 | 任务 |
|----------|------|
| §2 Retry 总开关 | 任务 4（`FlowEngineConfig.retryEnabled`、`RetryExecutor`） |
| §3 Loader + 校验 | 任务 3（`FlowDefinitionValidator`、`FlowDefinition`） |
| §3 Engine 同步 run + 持久化边界 | 任务 5（`FlowEngine`、快照写入点） |
| §3–4 Parallel 真并发、不取消 | 任务 2（`ConcurrentParallelNodeImpl`、`FlowExecutionContext.copyForParallelBranch`） |
| §5 重试策略 | 任务 4（`RetryPolicy`、注册表、退避 + 抖动） |
| §6 存储 fail-closed | 任务 6 + 7（禁止静默降级；抛出 `StoreException`） |
| §7 测试 | 任务 8（必做），任务 9 可选装饰器 |
| §8 多模块 JDK17 / Spring6 | 任务 1 + 7 |

---

### 任务 1：多模块 reactor + `behavior-tree-core` 使用 JDK 17

**涉及文件：**

- 修改：**仓库根目录** `pom.xml`（将当前内容替换为 **父 POM** 聚合）
- 新建：`behavior-tree-core/pom.xml`（将原根项目元数据与依赖迁入）
- 移动：`src/` → `behavior-tree-core/src/`（main + test）
- 新建：`behavior-tree-engine/pom.xml`（最小骨架，后续任务补全）
- 新建：`behavior-tree-spring-boot-starter/pom.xml`（骨架）

**说明：** 若外部已依赖 `com.lee9213.behavior:behavior-tree`，可仅在 core 模块保留 **artifactId** `behavior-tree`，或发布 **relocation POM**—择一并在 `README` 说明。本计划为清晰起见使用目录名与 **artifactId** `behavior-tree-core`；若必须保留原坐标，可改为 `behavior-tree`。

- [ ] **步骤 1：在仓库根目录写入新的父 `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.lee9213.behavior</groupId>
  <artifactId>behavior-tree-parent</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>behavior-tree-parent</name>

  <modules>
    <module>behavior-tree-core</module>
    <module>behavior-tree-engine</module>
    <module>behavior-tree-spring-boot-starter</module>
  </modules>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>17</java.version>
    <maven.compiler.release>17</maven.compiler.release>
    <lombok.version>1.18.34</lombok.version>
    <junit.version>5.10.2</junit.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.lee9213.behavior</groupId>
        <artifactId>behavior-tree-core</artifactId>
        <version>${project.version}</version>
      </dependency>
      <dependency>
        <groupId>com.lee9213.behavior</groupId>
        <artifactId>behavior-tree-engine</artifactId>
        <version>${project.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.13.0</version>
          <configuration>
            <release>${maven.compiler.release}</release>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.2.5</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **步骤 2：将现有工程移入 `behavior-tree-core`**

在仓库根目录执行（路径不同时自行调整）：

```bash
mkdir -p behavior-tree-core
git mv src behavior-tree-core/src
```

- [ ] **步骤 3：创建 `behavior-tree-core/pom.xml`**（从旧根 `pom.xml` 复制依赖，设置 parent、`artifactId` 为 `behavior-tree-core`、`java.version` 17；依赖版本可保守沿用或小幅升级）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.lee9213.behavior</groupId>
    <artifactId>behavior-tree-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>

  <artifactId>behavior-tree-core</artifactId>
  <packaging>jar</packaging>
  <name>behavior-tree-core</name>

  <dependencies>
    <!-- 复制：lombok, guava, log4j-core, fastjson, spring-webmvc（未使用可删）, junit-jupiter, spring-test -->
  </dependencies>
</project>
```

将重构前 `pom.xml` 第 29–70 行左右依赖填入 `<dependencies>`；若仅为在 JDK 17 下编译，core 可暂时保留 Spring 5—**更推荐**在 engine/starter 使用 Spring 6 时，从 core 移除未使用的 `spring-webmvc`。

- [ ] **步骤 3b：为 `behavior-tree-engine` 与 `behavior-tree-spring-boot-starter` 添加最小 POM**

以便在父工程执行 `mvn` 时能解析全部 `<module>`。示例 `behavior-tree-engine/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.lee9213.behavior</groupId>
    <artifactId>behavior-tree-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>
  <artifactId>behavior-tree-engine</artifactId>
  <packaging>jar</packaging>
  <dependencies>
    <dependency>
      <groupId>com.lee9213.behavior</groupId>
      <artifactId>behavior-tree-core</artifactId>
    </dependency>
  </dependencies>
</project>
```

示例 `behavior-tree-spring-boot-starter/pom.xml`（Spring Boot 版本在任务 6–7 中固定）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.lee9213.behavior</groupId>
    <artifactId>behavior-tree-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>
  <artifactId>behavior-tree-spring-boot-starter</artifactId>
  <packaging>jar</packaging>
  <dependencies>
    <dependency>
      <groupId>com.lee9213.behavior</groupId>
      <artifactId>behavior-tree-engine</artifactId>
    </dependency>
  </dependencies>
</project>
```

若父 POM 尚未列出 `behavior-tree-engine` 的 `dependencyManagement`，请补上（任务 1 父 POM 片段已含 engine）。引入 `behavior-tree-spring-boot-starter` 的 GAV 后，再将其加入 `dependencyManagement`。

- [ ] **步骤 4：验证 core 可编译**

执行：

```bash
mvn -pl behavior-tree-core -am test
```

期望：`BUILD SUCCESS`（修复 core 源码在 Java 17 下的告警/错误）。

- [ ] **步骤 5：提交**

```bash
git add pom.xml behavior-tree-core
git commit -m "build: multi-module parent and behavior-tree-core on JDK 17"
```

---

### 任务 2：`FlowExecutionContext` + `ConcurrentParallelNodeImpl`

**涉及文件：**

- 新建：`behavior-tree-core/src/main/java/com/lee9213/behavior/flow/FlowExecutionContext.java`（**放在 core**，不是 engine—避免 core→engine 依赖）
- 新建：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/ConcurrentParallelNodeImpl.java`
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorNodeWrapper.java` — 增加 `buildConcurrentParallelNode(String nodeName, List<...> children, Executor executor)`

**原因：** 并行子节点不能在线程间共享同一个可变 `BaseContext`。`FlowExecutionContext` 提供 `copyForParallelBranch()`；`ConcurrentParallelNodeImpl` 在每个子节点 `execute` **之前**调用它。

- [ ] **步骤 1：engine 模块依赖 core**

在 `behavior-tree-engine/pom.xml` 中：

```xml
<project>
  <parent>
    <groupId>com.lee9213.behavior</groupId>
    <artifactId>behavior-tree-parent</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>
  <artifactId>behavior-tree-engine</artifactId>
  <packaging>jar</packaging>
  <dependencies>
    <dependency>
      <groupId>com.lee9213.behavior</groupId>
      <artifactId>behavior-tree-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.2</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

父 POM 中已包含 `<module>behavior-tree-engine</module>`（任务 1）。

- [ ] **步骤 2：实现 `FlowExecutionContext`（在 core 中）**

创建 `behavior-tree-core/src/main/java/com/lee9213/behavior/flow/FlowExecutionContext.java`：

```java
package com.lee9213.behavior.flow;

import com.lee9213.behavior.tree.BaseContext;
import lombok.Getter;
import lombok.Setter;

/**
 * 流程引擎执行上下文。并行分支必须使用 {@link #copyForParallelBranch()}。
 */
@Getter
@Setter
public class FlowExecutionContext extends BaseContext {

    private String flowInstanceId;

    protected FlowExecutionContext(FlowExecutionContext other) {
        this.setCurrentNode(other.getCurrentNode());
        this.flowInstanceId = other.flowInstanceId;
    }

    public FlowExecutionContext() {
    }

    /**
     * 若子类携带需在分支间隔离的可变业务字段，请在应用代码中覆盖本方法。
     */
    public FlowExecutionContext copyForParallelBranch() {
        return new FlowExecutionContext(this);
    }
}
```

- [ ] **步骤 3：实现 `ConcurrentParallelNodeImpl`**

创建 `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/ConcurrentParallelNodeImpl.java`：

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.IParallelNode;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Log4j2
public final class ConcurrentParallelNodeImpl<Result extends NodeResult, Context extends BaseContext>
        extends AbstractControlNode<Result, Context> implements IParallelNode<Result, Context> {

    private final Executor executor;

    public ConcurrentParallelNodeImpl(List<BehaviorNodeWrapper<Result, Context>> childNodeList, Executor executor) {
        this.childNodeList = childNodeList;
        this.executor = executor;
    }

    @Override
    public Result execute(Context context) {
        if (childNodeList == null || childNodeList.isEmpty()) {
            return (Result) NodeResult.SUCCESS;
        }
        List<CompletableFuture<Result>> futures = new ArrayList<>();
        for (BehaviorNodeWrapper<Result, Context> wrapper : childNodeList) {
            final BehaviorNodeWrapper<Result, Context> w = wrapper;
            Context branchContext = context;
            if (context instanceof FlowExecutionContext) {
                branchContext = (Context) ((FlowExecutionContext) context).copyForParallelBranch();
            }
            Context bc = branchContext;
            CompletableFuture<Result> future = CompletableFuture.supplyAsync(() -> {
                bc.setCurrentNode(w);
                return w.getNode().execute(bc);
            }, executor);
            futures.add(future);
        }
        boolean isSuccess = true;
        for (CompletableFuture<Result> future : futures) {
            try {
                Result nodeResult = future.join();
                checkNodeResult(nodeResult);
                if (!nodeResult.isSuccess()) {
                    isSuccess = false;
                }
                log.info("parallel child result: {}", nodeResult);
            } catch (Exception ex) {
                isSuccess = false;
                log.error("parallel child failed", ex);
            }
        }
        return (Result) (isSuccess ? NodeResult.SUCCESS : NodeResult.FAILURE);
    }
}
```

- [ ] **步骤 4：在 `BehaviorNodeWrapper` 上增加工厂方法**

在 `BehaviorNodeWrapper.java` 中增加：

```java
import java.util.concurrent.Executor;

public BehaviorNodeWrapper<Result, Context> buildConcurrentParallelNode(
        String nodeName,
        List<BehaviorNodeWrapper<Result, Context>> childNodeList,
        Executor executor) {
    return new BehaviorNodeWrapper<>(nodeName, new ConcurrentParallelNodeImpl<>(childNodeList, executor));
}
```

- [ ] **步骤 5：编写失败用例先行测试 `ConcurrentParallelNodeImplTest`**

创建 `behavior-tree-core/src/test/java/com/lee9213/behavior/node/impl/ConcurrentParallelNodeImplTest.java`：

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.tree.node.INode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentParallelNodeImplTest {

    @Test
    void runsChildrenOnExecutorAndJoins() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        AtomicInteger counter = new AtomicInteger();
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> leaf1 = leaf("a", counter);
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> leaf2 = leaf("b", counter);
        BehaviorNodeWrapper<NodeResult, FlowExecutionContext> parallel = new BehaviorNodeWrapper<NodeResult, FlowExecutionContext>()
                .buildConcurrentParallelNode("p", List.of(leaf1, leaf2), pool);

        FlowExecutionContext ctx = new FlowExecutionContext();
        NodeResult r = parallel.getNode().execute(ctx);
        pool.shutdown();
        assertEquals(NodeResult.SUCCESS, r);
        assertEquals(2, counter.get());
    }

    private static BehaviorNodeWrapper<NodeResult, FlowExecutionContext> leaf(String name, AtomicInteger counter) {
        return new BehaviorNodeWrapper<>(name, new CountingLeaf(counter));
    }

    private static final class CountingLeaf implements INode<NodeResult, FlowExecutionContext> {
        private final AtomicInteger counter;

        private CountingLeaf(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public NodeResult execute(FlowExecutionContext context) {
            counter.incrementAndGet();
            return NodeResult.SUCCESS;
        }
    }
}
```

- [ ] **步骤 6：运行测试**

```bash
mvn -pl behavior-tree-core -Dtest=ConcurrentParallelNodeImplTest test
```

期望：编译与测试替身修正后 **通过**。

- [ ] **步骤 7：提交**

```bash
git add behavior-tree-core behavior-tree-engine/pom.xml
git commit -m "feat(core): concurrent parallel node and FlowExecutionContext"
```

---

### 任务 3：`FlowDefinition`、结构校验、执行图无环检查

**涉及文件：**

- 新建：`behavior-tree-engine/src/main/java/com/lee9213/behavior/engine/FlowDefinition.java`
- 新建：`behavior-tree-engine/src/main/java/com/lee9213/behavior/engine/validation/FlowDefinitionValidator.java`
- 新建：`behavior-tree-engine/src/main/java/com/lee9213/behavior/engine/validation/InvalidFlowDefinitionException.java`

- [ ] **步骤 1：`FlowDefinition`**

```java
package com.lee9213.behavior.engine;

import com.lee9213.behavior.tree.BehaviorTree;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class FlowDefinition {
    private final String id;
    private final String version;
    private final BehaviorTree<NodeResult, FlowExecutionContext> behaviorTree;
}
```

- [ ] **步骤 2：校验器 — 结构 + DAG 合理性**

`FlowDefinitionValidator.validate(FlowDefinition def)`：

- `id` / `version` 非空
- `behaviorTree.getRootNode()` 非 null
- 构图：每个 `BehaviorNodeWrapper` 赋予合成 id（BFS 路径）；边为 parent→child；对**树**而言无回边；仍实现基于 **DFS 三色** 或 **Kahn** 的 `hasCycle`（在由树构建的有向图上）。若发现环 → `InvalidFlowDefinitionException`（纯树下理论上不应出现）。

图中建边示意（在实现文件中写完整 Java，勿留伪代码）：

```java
void validateAcyclic(BehaviorNodeWrapper<?, ?> root) {
    Map<String, List<String>> adj = new HashMap<>();
    buildEdges(root, "0", adj);
    if (hasCycle(adj)) {
        throw new InvalidFlowDefinitionException("execution graph has a cycle");
    }
}
```

- [ ] **步骤 3：单元测试 `FlowDefinitionValidatorTest`**

合法树应通过；环场景可用包内测试替身构造畸形邻接表，或仅测 **空 id 失败** 等最小用例。

最低限度：**空 id 必须失败**。

- [ ] **步骤 4：运行并提交**

```bash
mvn -pl behavior-tree-engine test
git add behavior-tree-engine
git commit -m "feat(engine): FlowDefinition and validator"
```

---

### 任务 4：重试策略 + 引擎级开关

**涉及文件：**

- 新建：`behavior-tree-engine/.../retry/RetryPolicy.java`
- 新建：`behavior-tree-engine/.../retry/RetryPolicyRegistry.java`
- 新建：`behavior-tree-engine/.../retry/RetryExecutor.java`
- 新建：`behavior-tree-engine/.../FlowEngineConfig.java`
- 修改：`behavior-tree-core/.../BehaviorNodeWrapper.java` — 增加可选字段 `String stepTag` 及访问器（供引擎解析策略）

- [ ] **步骤 1：`RetryPolicy` record**

```java
package com.lee9213.behavior.engine.retry;

public record RetryPolicy(
        int maxAttempts,
        long baseDelayMillis,
        double multiplier,
        double jitterRatio // 相对当前 delay，0..1
) {
    public RetryPolicy {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts");
        if (jitterRatio < 0 || jitterRatio > 1) throw new IllegalArgumentException("jitterRatio");
    }
}
```

- [ ] **步骤 2：`RetryPolicyRegistry`**

```java
package com.lee9213.behavior.engine.retry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RetryPolicyRegistry {
    private final Map<String, RetryPolicy> byTag = new ConcurrentHashMap<>();
    private RetryPolicy defaultPolicy = new RetryPolicy(3, 50L, 2.0, 0.2);

    public void registerDefault(RetryPolicy policy) {
        this.defaultPolicy = policy;
    }

    public void registerForTag(String tag, RetryPolicy policy) {
        byTag.put(tag, policy);
    }

    public RetryPolicy resolve(String stepTag) {
        if (stepTag != null && byTag.containsKey(stepTag)) {
            return byTag.get(stepTag);
        }
        return defaultPolicy;
    }
}
```

- [ ] **步骤 3：带退避 + 抖动的 `RetryExecutor`**

```java
package com.lee9213.behavior.engine.retry;

import java.util.concurrent.ThreadLocalRandom;

public final class RetryExecutor {

    public interface RunnableAttempt<T> {
        T run() throws Exception;
    }

    public static <T> T execute(boolean retryEnabled,
                              RetryPolicy policy,
                              RunnableAttempt<T> attempt) throws Exception {
        if (!retryEnabled) {
            return attempt.run();
        }
        int attemptNo = 0;
        long delay = policy.baseDelayMillis();
        Exception last = null;
        while (attemptNo < policy.maxAttempts()) {
            try {
                return attempt.run();
            } catch (Exception ex) {
                last = ex;
                attemptNo++;
                if (attemptNo >= policy.maxAttempts()) {
                    break;
                }
                long jitter = (long) (delay * policy.jitterRatio() * ThreadLocalRandom.current().nextDouble());
                Thread.sleep(delay + jitter);
                delay = (long) (delay * policy.multiplier());
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("retry without exception");
    }
}
```

- [ ] **步骤 4：`FlowEngineConfig`**

```java
package com.lee9213.behavior.engine;

import com.lee9213.behavior.engine.retry.RetryPolicyRegistry;
import com.lee9213.behavior.tree.store.engine.ProcessInstanceStore;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class FlowEngineConfig {
    private boolean retryEnabled = true;
    private RetryPolicyRegistry retryPolicyRegistry = new RetryPolicyRegistry();
    private ProcessInstanceStore store;
    private Executor parallelExecutor = ForkJoinPool.commonPool();

    public boolean isRetryEnabled() { return retryEnabled; }
    public void setRetryEnabled(boolean retryEnabled) { this.retryEnabled = retryEnabled; }
    public RetryPolicyRegistry getRetryPolicyRegistry() { return retryPolicyRegistry; }
    public void setRetryPolicyRegistry(RetryPolicyRegistry retryPolicyRegistry) {
        this.retryPolicyRegistry = retryPolicyRegistry;
    }
    public ProcessInstanceStore getStore() { return store; }
    public void setStore(ProcessInstanceStore store) { this.store = store; }
    public Executor getParallelExecutor() { return parallelExecutor; }
    public void setParallelExecutor(Executor parallelExecutor) { this.parallelExecutor = parallelExecutor; }
}
```

在任务 5 执行带 tag 的叶子节点时接入 `RetryExecutor`。

- [ ] **步骤 5：为 `BehaviorNodeWrapper` 增加 `stepTag`**

```java
private String stepTag;

public String getStepTag() { return stepTag; }
public void setStepTag(String stepTag) { this.stepTag = stepTag; }
```

- [ ] **步骤 6：测试重试开关**

在 `behavior-tree-engine` 中验证：`retryEnabled=false` 时 `RetryExecutor` **只执行一次**（抛错尝试 + 计数器）。

- [ ] **步骤 7：提交**

```bash
git commit -am "feat(engine): retry policies and engine-level retry toggle"
```

---

### 任务 5：`FlowInstanceSnapshot`、`ProcessInstanceStore`、`FlowEngine.run`

**涉及文件：**

- 新建：`behavior-tree-engine/.../FlowInstanceSnapshot.java`
- 新建：`behavior-tree-engine/.../store/ProcessInstanceStore.java`
- 新建：`behavior-tree-engine/.../store/InMemoryProcessInstanceStore.java`
- 新建：`behavior-tree-engine/.../store/StoreException.java`
- 新建：`behavior-tree-engine/.../FlowEngine.java`

- [ ] **步骤 1：快照**

```java
package com.lee9213.behavior.engine;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.Map;

@Value
@Builder
public class FlowInstanceSnapshot implements Serializable {
    String definitionId;
    String definitionVersion;
    String status; // RUNNING, SUCCESS, FAILURE
    Map<String, Integer> retryCountByStepPath;
}
```

- [ ] **步骤 2：存储接口**

```java
package com.lee9213.behavior.engine.store;

import com.lee9213.behavior.tree.engine.FlowInstanceSnapshot;

import java.util.Optional;

public interface ProcessInstanceStore {
    Optional<FlowInstanceSnapshot> load(String instanceId) throws StoreException;
    void save(String instanceId, FlowInstanceSnapshot snapshot) throws StoreException;
    void delete(String instanceId) throws StoreException;
}
```

- [ ] **步骤 3：内存实现**

`ConcurrentHashMap<String, FlowInstanceSnapshot>` — 仅在测试中模拟失败时才抛出 `StoreException`。

- [ ] **步骤 4：最小同步 `run` 的 `FlowEngine`**

```java
package com.lee9213.behavior.engine;

import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.store.engine.ProcessInstanceStore;
import com.lee9213.behavior.tree.store.engine.StoreException;
import com.lee9213.behavior.engine.validation.FlowDefinitionValidator;
import com.lee9213.behavior.flow.FlowExecutionContext;

import java.util.HashMap;

public final class FlowEngine {

    private final FlowEngineConfig config;

    public FlowEngine(FlowEngineConfig config) {
        this.config = config;
    }

    public NodeResult run(String instanceId, FlowDefinition definition, FlowExecutionContext context) throws StoreException {
        FlowDefinitionValidator.validate(definition);
        context.setFlowInstanceId(instanceId);
        ProcessInstanceStore store = config.getStore();
        FlowInstanceSnapshot initial = FlowInstanceSnapshot.builder()
                .definitionId(definition.getId())
                .definitionVersion(definition.getVersion())
                .status("RUNNING")
                .retryCountByStepPath(new HashMap<>())
                .build();
        if (store != null) {
            store.save(instanceId, initial);
        }
        NodeResult result = definition.getBehaviorTree().execute(context);
        if (store != null) {
            store.save(instanceId, FlowInstanceSnapshot.builder()
                    .definitionId(definition.getId())
                    .definitionVersion(definition.getVersion())
                    .status(result.isSuccess() ? "SUCCESS" : "FAILURE")
                    .retryCountByStepPath(new HashMap<>())
                    .build());
        }
        return result;
    }
}
```

**说明：** `FlowExecutionContext` 上需有 `setFlowInstanceId`（任务 2 已用 Lombok `@Setter` 覆盖）。

**说明：** 将 `ConcurrentParallelNodeImpl` 接入需在**构建树**时使用 `buildConcurrentParallelNode` + `config.getParallelExecutor()`；该接线在**工厂/解析**或手工测试中完成，不必在 `FlowEngine` 内做 visitor 替换—**v1 YAGNI**：文档约定「由引擎管理的并行」须用 `buildConcurrentParallelNode` + 配置中的 executor。可选后续：`FlowDefinitionBuilder` 辅助类。

- [ ] **步骤 5：测试 `FlowEngine` 与 store 调用**

JUnit：Mockito mock `ProcessInstanceStore`，或内联记录 `save` 调用次数的实现类。

- [ ] **步骤 6：提交**

```bash
git add behavior-tree-engine
git commit -m "feat(engine): FlowEngine synchronous run and instance store"
```

---

### 任务 6：基于 Redis 的 `ProcessInstanceStore`（放在 engine 或 starter）

**涉及文件：**

- 新建：`behavior-tree-spring-boot-starter/src/main/java/com/lee9213/behavior/spring/redis/RedisProcessInstanceStore.java`
- 修改：`behavior-tree-spring-boot-starter/pom.xml` — 增加 `spring-boot-starter-data-redis`、`jackson-databind`

使用 `StringRedisTemplate` 实现 `ProcessInstanceStore`，key 形如 `flow:instance:{instanceId}`，value 为 `FlowInstanceSnapshot` 的 JSON。

- [ ] **步骤 1：实现类**

使用 `ObjectMapper.writeValueAsString` / `readValue`，将 `JsonProcessingException` 包装为 `StoreException`。

- [ ] **步骤 2：集成测试（可选 profile）**

`@SpringBootTest` + Testcontainers Redis，**或** CI 跳过并在文档中说明手工验证。

- [ ] **步骤 3：提交**

```bash
git add behavior-tree-spring-boot-starter
git commit -m "feat(starter): Redis ProcessInstanceStore"
```

---

### 任务 7：Spring Boot 自动配置

**涉及文件：**

- 新建：`behavior-tree-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 新建：`behavior-tree-spring-boot-starter/.../BehaviorFlowAutoConfiguration.java`

Bean：

- `@Bean FlowEngineConfig` — 绑定 `behavior.flow.retry-enabled`（默认 true）
- `@Bean FlowEngine`
- `@Bean @ConditionalOnBean(StringRedisTemplate.class) ProcessInstanceStore redisStore(...)` — **禁止**静默降级；未配置 Redis 时，用户须显式配置 `InMemoryProcessInstanceStore`，或仅用无 starter 自动存储的 engine。

- [ ] **步骤 1：`BehaviorFlowAutoConfiguration`**

提供 Java 配置类，并 `@EnableConfigurationProperties(FlowEngineProperties.class)`。

- [ ] **步骤 2：配置属性类**

```java
@ConfigurationProperties(prefix = "behavior.flow")
public class FlowEngineProperties {
    private boolean retryEnabled = true;
    // getter / setter
}
```

- [ ] **步骤 3：提交**

```bash
git commit -am "feat(starter): BehaviorFlowAutoConfiguration"
```

---

### 任务 8：端到端单元测试（engine 模块）

**文件：**

- `behavior-tree-engine/src/test/java/com/lee9213/behavior/engine/RetryExecutorTest.java`
- `behavior-tree-engine/src/test/java/com/lee9213/behavior/engine/FlowEngineStoreTest.java`
- `behavior-tree-engine/src/test/java/com/lee9213/behavior/engine/FlowDefinitionValidatorTest.java`

每个测试类 2～3 个用例；执行：

```bash
mvn -pl behavior-tree-engine test
```

期望：全部通过。

---

## 自检（计划 vs 规格）

1. **规格覆盖：** §§2–7 由任务 2–8 覆盖；Redis 集成测试按规格 §7 标为可选。
2. **占位符：** 无 TBD 步骤；并行/上下文依赖通过将 `FlowExecutionContext` 放在 **core** 的 `com.lee9213.behavior.flow` 解决。
3. **一致性：** `FlowEngineConfig.retryEnabled` 与 `RetryExecutor` 一致；`StoreException` 表达 fail-closed；自动配置中无 Redis→内存静默降级（仅显式 Bean）。

**明确推迟的缺口：** 若需对**每个**叶子 `execute` 自动套 `RetryExecutor`，要么装饰 `INode`，要么在 `BehaviorNodeWrapper.getNode().execute` 外包一层代理或新增 `RetryingActionNode`—需要时在 **任务 9** 补充。

### 任务 9（可选）：`RetryingINodeDecorator`

**文件：**

- 新建：`behavior-tree-engine/.../retry/RetryingINodeDecorator.java`，实现 `INode`，委托给内层节点，并在 `RetryExecutor.execute(config.isRetryEnabled(), policy, delegate::execute)` 中调用。

**接入：** 仅在工厂构建包装器时应用—在 README 中说明。

---

## 执行交接

计划已保存至 `docs/superpowers/plans/2026-04-11-behavior-flow-engine.md`。两种执行方式：

1. **子代理驱动（推荐）** — 每任务新开子代理，任务间评审，迭代快。**必须子技能：** superpowers:subagent-driven-development。

2. **本会话内联执行** — 使用 superpowers:executing-plans，分批检查点执行。

**请选择其一。**
