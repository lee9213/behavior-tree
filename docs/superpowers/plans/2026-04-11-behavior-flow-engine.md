# Behavior Flow Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a flow engine layer on top of the existing behavior-tree library: synchronous blocking `run`, optional Redis-backed or in-memory `ProcessInstanceStore`, engine-level retry toggle, tag-based retry policies with exponential backoff and jitter, and concurrent Parallel execution via an injected `Executor` without cancelling sibling tasks on failure—while keeping v1 definitions as pure trees with defensive acyclicity validation.

**Architecture:** Convert the repository to a **multi-module Maven reactor** (`behavior-tree-core` = current library on **JDK 17**, `behavior-tree-engine` = `FlowDefinition`, `FlowEngine`, retry, store SPI + memory, `ConcurrentParallelNodeImpl`, validation; `behavior-tree-spring-boot-starter` = Spring Boot 3 / Spring 6 auto-configuration for `Executor`, Redis-backed store, and engine beans). Execution stays **behavior-tree–driven**: the engine wraps `BehaviorTree.execute` with a **`FlowExecutionContext`** that supports **branch-local context copies** for safe parallel execution.

**Tech Stack:** Maven 3.9+, JDK 17, JUnit 5, Lombok (align versions in parent POM), existing Guava/log4j/fastjson in core as today; engine adds minimal deps; Spring Boot 3.2.x (Spring Framework 6.x) + `spring-boot-starter-data-redis` (Lettuce) for the starter module; JSON serialization for Redis payloads via **Jackson** (add in starter + engine test scope if needed).

**Context:** Prefer implementing in a **dedicated git worktree** (see superpowers:using-git-worktrees) so the reactor refactor stays isolated until green.

---

## File structure (target)

| Path | Responsibility |
|------|----------------|
| `pom.xml` | Parent aggregator: `dependencyManagement`, JDK 17, plugin versions, `<modules>` |
| `behavior-tree-core/pom.xml` | Existing behavior-tree artifact (coordinates may become `behavior-tree-core`—see Task 1 note) |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/**` | Current code **unchanged** except small, listed edits (`BehaviorNodeWrapper`, new parallel impl) |
| `behavior-tree-engine/pom.xml` | Depends on `behavior-tree-core` |
| `behavior-tree-engine/src/main/java/com/lee9213/behavior/engine/FlowDefinition.java` | Immutable: id, version, `BehaviorTree` root |
| `behavior-tree-engine/.../FlowInstanceSnapshot.java` | Serializable state for store |
| `behavior-tree-engine/.../FlowEngineConfig.java` | `retryEnabled`, `Executor`, `ProcessInstanceStore`, `RetryPolicyRegistry`, timeouts |
| `behavior-tree-core/.../flow/FlowExecutionContext.java` | Extends `BaseContext`; `copyForParallelBranch()` for thread isolation (**must live in core** so `ConcurrentParallelNodeImpl` does not depend on engine) |
| `behavior-tree-engine/.../FlowEngine.java` | `run(instanceId, FlowDefinition, FlowExecutionContext initialContext)` |
| `behavior-tree-engine/.../retry/RetryPolicy.java` | max attempts, base delay, multiplier, jitter ratio |
| `behavior-tree-engine/.../retry/RetryPolicyRegistry.java` | resolve by step tag / type key |
| `behavior-tree-engine/.../retry/RetryExecutor.java` | applies policy + sleep; respects `retryEnabled` |
| `behavior-tree-engine/.../store/ProcessInstanceStore.java` | load/save/delete |
| `behavior-tree-engine/.../store/InMemoryProcessInstanceStore.java` | `ConcurrentHashMap` |
| `behavior-tree-engine/.../store/StoreException.java` | unchecked or checked—pick one and use consistently |
| `behavior-tree-engine/.../validation/FlowDefinitionValidator.java` | structural checks + `ExecutionGraphSanityChecker` (tree ⇒ DAG) |
| `behavior-tree-core/.../ParallelNodeImpl.java` | Keep as-is for backward compatibility |
| `behavior-tree-core/.../ConcurrentParallelNodeImpl.java` | New: `Executor` + children; join semantics match spec §4 |
| `behavior-tree-spring-boot-starter/pom.xml` | Depends on engine + spring-boot-starter-data-redis |
| `behavior-tree-spring-boot-starter/.../BehaviorFlowAutoConfiguration.java` | `@AutoConfiguration`, beans |
| `behavior-tree-engine/src/test/java/**` | JUnit 5 tests |

---

## Spec coverage map

| Spec section | Tasks |
|--------------|-------|
| §2 Retry 总开关 | Task 4 (`FlowEngineConfig.retryEnabled`, `RetryExecutor`) |
| §3 Loader + 校验 | Task 3 (`FlowDefinitionValidator`, `FlowDefinition`) |
| §3 Engine 同步 run + 持久化边界 | Task 5 (`FlowEngine`, snapshot save points) |
| §3–4 Parallel 真并发、不取消 | Task 2 (`ConcurrentParallelNodeImpl`, `FlowExecutionContext.copyForParallelBranch`) |
| §5 重试策略 | Task 4 (`RetryPolicy`, registry, backoff+jitter) |
| §6 存储 fail-closed | Task 6 + 7 (no silent fallback; throw `StoreException`) |
| §7 测试 | Task 8 (required), Task 9 optional decorator |
| §8 多模块 JDK17 / Spring6 | Tasks 1 + 7 |

---

### Task 1: Multi-module reactor + JDK 17 for `behavior-tree-core`

**Files:**

- Modify: **repo root** `pom.xml` (replace current content with **parent** aggregator)
- Create: `behavior-tree-core/pom.xml` (relocate current project metadata + dependencies from old root)
- Move: `src/` → `behavior-tree-core/src/` (main + test)
- Create: `behavior-tree-engine/pom.xml` (minimal skeleton, filled in later tasks)
- Create: `behavior-tree-spring-boot-starter/pom.xml` (skeleton)

**Note:** Publishing: if the world depends on `com.lee9213.behavior:behavior-tree`, either keep **artifactId** `behavior-tree` on the core module only, or publish a **relocation POM**—pick one and document in `README`. This plan uses **`behavior-tree-core`** as module directory and artifactId for clarity; adjust to `behavior-tree` if you must preserve the exact coordinate.

- [ ] **Step 1: Write new parent `pom.xml` at repo root**

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

- [ ] **Step 2: Move existing project into `behavior-tree-core`**

Run from repo root (adjust if paths differ):

```bash
mkdir -p behavior-tree-core
git mv src behavior-tree-core/src
```

- [ ] **Step 3: Create `behavior-tree-core/pom.xml`** (copy dependencies from old root `pom.xml`, set parent + `artifactId` `behavior-tree-core`, `java.version` 17, reuse same dependency versions or bump conservatively)

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
    <!-- copy: lombok, guava, log4j-core, fastjson, spring-webmvc (or drop if unused), junit-jupiter, spring-test -->
  </dependencies>
</project>
```

Fill `<dependencies>` by copying lines 29–70 from the pre-refactor `pom.xml`, bump Spring only if you must compile on 17; core can temporarily keep Spring 5 for compilation **only inside core**—but prefer removing unused `spring-webmvc` from core if engine/starter own Spring 6.

- [ ] **Step 3b: Add minimal `behavior-tree-engine` and `behavior-tree-spring-boot-starter` POMs**

So `mvn` from the parent can resolve all `<module>` entries. Example `behavior-tree-engine/pom.xml`:

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

Example `behavior-tree-spring-boot-starter/pom.xml` (Spring Boot version pinned when you implement Task 6–7):

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

Add `behavior-tree-engine` to parent `dependencyManagement` if not already listed (Task 1 parent snippet already includes it). Add `behavior-tree-spring-boot-starter` to `dependencyManagement` when you introduce its GAV.

- [ ] **Step 4: Verify core compiles**

Run:

```bash
mvn -pl behavior-tree-core -am test
```

Expected: `BUILD SUCCESS` (fix any Java 17 migration warnings/errors in core sources).

- [ ] **Step 5: Commit**

```bash
git add pom.xml behavior-tree-core
git commit -m "build: multi-module parent and behavior-tree-core on JDK 17"
```

---

### Task 2: `FlowExecutionContext` + `ConcurrentParallelNodeImpl`

**Files:**

- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/flow/FlowExecutionContext.java` (**core**, not engine—avoids core→engine dependency)
- Create: `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/ConcurrentParallelNodeImpl.java`
- Modify: `behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorNodeWrapper.java` — add `buildConcurrentParallelNode(String nodeName, List<...> children, Executor executor)`

**Rationale:** Parallel children must not share one mutable `BaseContext` across threads. `FlowExecutionContext` provides `copyForParallelBranch()`; `ConcurrentParallelNodeImpl` calls it per child **before** `execute`.

- [ ] **Step 1: Add engine module dependency on core**

In `behavior-tree-engine/pom.xml`:

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

Register `<module>behavior-tree-engine</module>` already in parent (Task 1).

- [ ] **Step 2: Implement `FlowExecutionContext` (in core)**

Create `behavior-tree-core/src/main/java/com/lee9213/behavior/flow/FlowExecutionContext.java`:

```java
package com.lee9213.behavior.flow;

import com.lee9213.behavior.BaseContext;
import lombok.Getter;
import lombok.Setter;

/**
 * Execution context for the flow engine. Parallel branches must use {@link #copyForParallelBranch()}.
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
     * Override in application code if the context carries mutable domain state that must be isolated per branch.
     */
    public FlowExecutionContext copyForParallelBranch() {
        return new FlowExecutionContext(this);
    }
}
```

- [ ] **Step 3: Implement `ConcurrentParallelNodeImpl`**

Create `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/ConcurrentParallelNodeImpl.java`:

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BaseContext;
import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
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

- [ ] **Step 4: Add factory method on `BehaviorNodeWrapper`**

In `BehaviorNodeWrapper.java`, add:

```java
import java.util.concurrent.Executor;

public BehaviorNodeWrapper<Result, Context> buildConcurrentParallelNode(
        String nodeName,
        List<BehaviorNodeWrapper<Result, Context>> childNodeList,
        Executor executor) {
    return new BehaviorNodeWrapper<>(nodeName, new ConcurrentParallelNodeImpl<>(childNodeList, executor));
}
```

- [ ] **Step 5: Write failing test `ConcurrentParallelNodeImplTest`**

Create `behavior-tree-core/src/test/java/com/lee9213/behavior/node/impl/ConcurrentParallelNodeImplTest.java`:

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.BehaviorNodeWrapper;
import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.node.INode;
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

- [ ] **Step 6: Run test**

```bash
mvn -pl behavior-tree-core -Dtest=ConcurrentParallelNodeImplTest test
```

Expected: PASS after fixing compilation and test doubles.

- [ ] **Step 7: Commit**

```bash
git add behavior-tree-core behavior-tree-engine/pom.xml
git commit -m "feat(core): concurrent parallel node and FlowExecutionContext"
```

---

### Task 3: `FlowDefinition`, structural validation, acyclic execution graph check

**Files:**

- Create: `behavior-tree-engine/src/main/java/com/lee9213/behavior/engine/FlowDefinition.java`
- Create: `behavior-tree-engine/src/main/java/com/lee9213/behavior/engine/validation/FlowDefinitionValidator.java`
- Create: `behavior-tree-engine/src/main/java/com/lee9213/behavior/engine/validation/InvalidFlowDefinitionException.java`

- [ ] **Step 1: `FlowDefinition`**

```java
package com.lee9213.behavior.engine;

import com.lee9213.behavior.BehaviorTree;
import com.lee9213.behavior.NodeResult;
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

- [ ] **Step 2: Validator — structure + DAG sanity**

`FlowDefinitionValidator.validate(FlowDefinition def)`:

- `id` / `version` non-blank
- `behaviorTree.getRootNode()` non-null
- Build a graph: each `BehaviorNodeWrapper` gets a synthetic id (BFS index path); edges parent→child; run **Kahn topological** or **DFS visited**—for a **tree** there are no back-edges; still implement `detectCycle(List<Edge>)` using standard **DFS coloring** on the directed graph built from the tree. If cycle found → `InvalidFlowDefinitionException` (should not happen for pure tree).

Graph builder sketch (pseudocode in real Java in file):

```java
void validateAcyclic(BehaviorNodeWrapper<?, ?> root) {
    Map<String, List<String>> adj = new HashMap<>();
    buildEdges(root, "0", adj);
    if (hasCycle(adj)) {
        throw new InvalidFlowDefinitionException("execution graph has a cycle");
    }
}
```

- [ ] **Step 3: Unit test `FlowDefinitionValidatorTest`**

Assert valid tree passes; for cycle case, **synthesize** a malformed adjacency in a package-private test double **or** skip impossible case and test validator only on blank id.

Minimum: **test blank id fails**.

- [ ] **Step 4: Run + commit**

```bash
mvn -pl behavior-tree-engine test
git add behavior-tree-engine
git commit -m "feat(engine): FlowDefinition and validator"
```

---

### Task 4: Retry policies + engine-level toggle

**Files:**

- Create: `behavior-tree-engine/.../retry/RetryPolicy.java`
- Create: `behavior-tree-engine/.../retry/RetryPolicyRegistry.java`
- Create: `behavior-tree-engine/.../retry/RetryExecutor.java`
- Create: `behavior-tree-engine/.../FlowEngineConfig.java`
- Modify: `behavior-tree-core/.../BehaviorNodeWrapper.java` — add optional `String stepTag` field + accessor (used by engine to resolve policy)

- [ ] **Step 1: `RetryPolicy` record**

```java
package com.lee9213.behavior.engine.retry;

public record RetryPolicy(
        int maxAttempts,
        long baseDelayMillis,
        double multiplier,
        double jitterRatio // 0..1 relative to computed delay
) {
    public RetryPolicy {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts");
        if (jitterRatio < 0 || jitterRatio > 1) throw new IllegalArgumentException("jitterRatio");
    }
}
```

- [ ] **Step 2: `RetryPolicyRegistry`**

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

- [ ] **Step 3: `RetryExecutor` with backoff + jitter**

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

- [ ] **Step 4: `FlowEngineConfig`**

```java
package com.lee9213.behavior.engine;

import com.lee9213.behavior.engine.retry.RetryPolicyRegistry;
import com.lee9213.behavior.engine.store.ProcessInstanceStore;

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

Wire `RetryExecutor` in Task 5 when executing tagged leaf nodes.

- [ ] **Step 5: Add `stepTag` to `BehaviorNodeWrapper`**

```java
private String stepTag;

public String getStepTag() { return stepTag; }
public void setStepTag(String stepTag) { this.stepTag = stepTag; }
```

- [ ] **Step 6: Test retry toggle**

In `behavior-tree-engine`, test that when `retryEnabled=false`, `RetryExecutor` runs exactly once (use a throwing attempt + counter).

- [ ] **Step 7: Commit**

```bash
git commit -am "feat(engine): retry policies and engine-level retry toggle"
```

---

### Task 5: `FlowInstanceSnapshot`, `ProcessInstanceStore`, `FlowEngine.run`

**Files:**

- Create: `behavior-tree-engine/.../FlowInstanceSnapshot.java`
- Create: `behavior-tree-engine/.../store/ProcessInstanceStore.java`
- Create: `behavior-tree-engine/.../store/InMemoryProcessInstanceStore.java`
- Create: `behavior-tree-engine/.../store/StoreException.java`
- Create: `behavior-tree-engine/.../FlowEngine.java`

- [ ] **Step 1: Snapshot**

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

- [ ] **Step 2: Store interface**

```java
package com.lee9213.behavior.engine.store;

import com.lee9213.behavior.engine.FlowInstanceSnapshot;

import java.util.Optional;

public interface ProcessInstanceStore {
    Optional<FlowInstanceSnapshot> load(String instanceId) throws StoreException;
    void save(String instanceId, FlowInstanceSnapshot snapshot) throws StoreException;
    void delete(String instanceId) throws StoreException;
}
```

- [ ] **Step 3: In-memory implementation**

`ConcurrentHashMap<String, FlowInstanceSnapshot>` — throws `StoreException` only if you choose to simulate failures in tests.

- [ ] **Step 4: `FlowEngine` minimal synchronous `run`**

```java
package com.lee9213.behavior.engine;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.engine.store.ProcessInstanceStore;
import com.lee9213.behavior.engine.store.StoreException;
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

**Note:** Add `setFlowInstanceId` on `FlowExecutionContext` if not present; field already in Task 2 class.

**Note:** Integrate `ConcurrentParallelNodeImpl` by building trees that use `buildConcurrentParallelNode` with `config.getParallelExecutor()`—that wiring belongs in **factory** code paths (parser or manual tests), not inside `FlowEngine` unless you add a **tree visitor** to replace parallel nodes—**YAGNI for v1**: document that **engine-managed parallel** requires building the tree with `buildConcurrentParallelNode` + executor from config. Optional follow-up task: `FlowDefinitionBuilder` helper.

- [ ] **Step 5: Test `FlowEngine` + store called**

JUnit: mock `ProcessInstanceStore` with Mockito or simple recording impl counting `save` invocations (implement inline class in test).

- [ ] **Step 6: Commit**

```bash
git add behavior-tree-engine
git commit -m "feat(engine): FlowEngine synchronous run and instance store"
```

---

### Task 6: Redis-backed `ProcessInstanceStore` (engine or starter)

**Files:**

- Create: `behavior-tree-spring-boot-starter/src/main/java/com/lee9213/behavior/spring/redis/RedisProcessInstanceStore.java`
- Modify: `behavior-tree-spring-boot-starter/pom.xml` — `spring-boot-starter-data-redis`, `jackson-databind`

Implement `ProcessInstanceStore` using `StringRedisTemplate`, key pattern `flow:instance:{instanceId}`, value = JSON from `FlowInstanceSnapshot`.

- [ ] **Step 1: Implement class**

Use `ObjectMapper.writeValueAsString` / `readValue` with `StoreException` wrapping `JsonProcessingException`.

- [ ] **Step 2: Integration test (optional profile)**

`@SpringBootTest` + Testcontainers Redis **or** skip CI and document manual run.

- [ ] **Step 3: Commit**

```bash
git add behavior-tree-spring-boot-starter
git commit -m "feat(starter): Redis ProcessInstanceStore"
```

---

### Task 7: Spring Boot auto-configuration

**Files:**

- Create: `behavior-tree-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `behavior-tree-spring-boot-starter/.../BehaviorFlowAutoConfiguration.java`

Beans:

- `@Bean FlowEngineConfig` — bind `behavior.flow.retry-enabled` (default true)
- `@Bean FlowEngine`
- `@Bean @ConditionalOnBean(StringRedisTemplate.class) ProcessInstanceStore redisStore(...)` — **do not** silently fall back; if Redis not configured, user must set `InMemoryProcessInstanceStore` explicitly or depend on core-only engine without starter auto-store.

- [ ] **Step 1: `BehaviorFlowAutoConfiguration`**

Provide Java configuration with `@EnableConfigurationProperties(FlowEngineProperties.class)`.

- [ ] **Step 2: Properties class**

```java
@ConfigurationProperties(prefix = "behavior.flow")
public class FlowEngineProperties {
    private boolean retryEnabled = true;
    // getters/setters
}
```

- [ ] **Step 3: Commit**

```bash
git commit -am "feat(starter): BehaviorFlowAutoConfiguration"
```

---

### Task 8: End-to-end unit tests (engine module)

**Files:**

- `behavior-tree-engine/src/test/java/com/lee9213/behavior/engine/RetryExecutorTest.java`
- `behavior-tree-engine/src/test/java/com/lee9213/behavior/engine/FlowEngineStoreTest.java`
- `behavior-tree-engine/src/test/java/com/lee9213/behavior/engine/FlowDefinitionValidatorTest.java`

Each test class: 2–3 cases with assertions; run:

```bash
mvn -pl behavior-tree-engine test
```

Expected: all pass.

---

## Self-review (plan vs spec)

1. **Spec coverage:** All §§2–7 addressed by Tasks 2–8; optional Redis integration test marked optional per spec §7.
2. **Placeholder scan:** No TBD steps; parallel/context dependency resolved by placing `FlowExecutionContext` in **core** under `com.lee9213.behavior.flow`.
3. **Consistency:** `FlowEngineConfig.retryEnabled` matches `RetryExecutor`; `StoreException` used for fail-closed behavior; no silent Redis→memory fallback in auto-config (explicit beans only).

**Gap explicitly deferred:** Wrapping **every** leaf `execute` with `RetryExecutor` requires either decorating `INode` instances or a **proxy** around `BehaviorNodeWrapper.getNode().execute` inside a new `RetryingActionNode`—add **Task 9** if you need automatic retry without manual wrapping:

### Task 9 (optional): `RetryingINodeDecorator`

**Files:**

- Create: `behavior-tree-engine/.../retry/RetryingINodeDecorator.java` implementing `INode` delegating to delegate with `RetryExecutor.execute(config.isRetryEnabled(), policy, delegate::execute)`.

**Integration:** Applied only when building wrappers in factory—document in README.

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-11-behavior-flow-engine.md`. Two execution options:

1. **Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration. **REQUIRED SUB-SKILL:** superpowers:subagent-driven-development.

2. **Inline Execution** — Execute tasks in this session using superpowers:executing-plans with batch checkpoints.

**Which approach?**
