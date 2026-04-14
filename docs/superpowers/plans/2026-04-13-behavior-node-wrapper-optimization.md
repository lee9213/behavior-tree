# BehaviorNodeWrapper 优化实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 移除 BehaviorNodeWrapper 类，增强 INode 接口，使节点直接实现必要的方法，简化代码结构。

**架构：** 增强 INode 接口，添加节点名称和步骤标签相关方法；修改所有节点实现类，直接实现这些方法；修改控制节点，使用 List<INode> 管理子节点；修改 BaseContext 和 BehaviorTree，使用 INode 替代 BehaviorNodeWrapper；创建 NodeFactory 类提供工厂方法。

**技术栈：** Java, Spring Boot, Behavior Tree

---

## 文件结构

### 核心文件

| 文件路径 | 职责 |
|---------|------|
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/INode.java` | 增强接口，添加节点名称和步骤标签相关方法 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/AbstractActionNode.java` | 修改抽象类，实现新增的接口方法 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/AbstractControlNode.java` | 修改抽象类，实现新增的接口方法，使用 List<INode> 管理子节点 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/BaseContext.java` | 修改上下文类，使用 INode 替代 BehaviorNodeWrapper |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorTree.java` | 修改行为树类，使用 INode 替代 BehaviorNodeWrapper |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/NodeFactory.java` | 新建工厂类，提供节点创建方法 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorNodeWrapper.java` | 删除此类 |

### 节点实现文件

| 文件路径 | 职责 |
|---------|------|
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SequenceNodeImpl.java` | 修改实现类，使用 INode 管理子节点 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SelectorNodeImpl.java` | 修改实现类，使用 INode 管理子节点 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/ParallelNodeImpl.java` | 修改实现类，使用 INode 管理子节点 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/RandomNodeImpl.java` | 修改实现类，使用 INode 管理子节点 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/StrategyNodeImpl.java` | 修改实现类，使用 INode 管理子节点 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SuccessActionNodeImpl.java` | 修改实现类，实现新增的接口方法 |
| `behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/FailureActionNodeImpl.java` | 修改实现类，实现新增的接口方法 |

### 其他文件

| 文件路径 | 职责 |
|---------|------|
| `behavior-tree-core/src/main/java/com/lee9213/behavior/definition/assemble/DefinitionAssembler.java` | 修改实现类，使用 INode 替代 BehaviorNodeWrapper |
| `behavior-tree-core/src/test/java/com/lee9213/behavior/BehaviorTreeTest.java` | 修改测试类，使用 INode 替代 BehaviorNodeWrapper |

---

## 任务分解

### 任务 1：增强 INode 接口

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/INode.java`

- [ ] **步骤 1：修改 INode 接口，添加新方法**

```java
package com.lee9213.behavior.node;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:11
 */
public interface INode<Result extends NodeResult,Context extends BaseContext> {

    /**
     * 执行具体业务
     *
     * @param context 业务上下文
     * @return 执行结果
     */
    Result execute(Context context);
    
    /**
     * 获取节点名称
     *
     * @return 节点名称
     */
    String getNodeName();
    
    /**
     * 获取步骤标签
     *
     * @return 步骤标签
     */
    String getStepTag();
    
    /**
     * 设置步骤标签
     *
     * @param stepTag 步骤标签
     */
    void setStepTag(String stepTag);
}
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/INode.java
git commit -m "feat: enhance INode interface with node name and step tag methods"
```

### 任务 2：修改 AbstractActionNode

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/AbstractActionNode.java`

- [ ] **步骤 1：修改 AbstractActionNode，添加节点名称属性和方法实现**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.IActionNode;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:36
 */
public abstract class AbstractActionNode<Result extends NodeResult, Context extends BaseContext> implements IActionNode<Result, Context> {
    protected String nodeName;
    protected String stepTag;
    
    public AbstractActionNode(String nodeName) {
        this.nodeName = nodeName;
    }
    
    @Override
    public String getNodeName() {
        return nodeName;
    }
    
    @Override
    public String getStepTag() {
        return stepTag;
    }
    
    @Override
    public void setStepTag(String stepTag) {
        this.stepTag = stepTag;
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/AbstractActionNode.java
git commit -m "feat: modify AbstractActionNode to implement new INode methods"
```

### 任务 3：修改 AbstractControlNode

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/AbstractControlNode.java`

- [ ] **步骤 1：修改 AbstractControlNode，使用 List<INode> 管理子节点**

```java
package com.lee9213.behavior.node.impl;

import com.google.common.collect.Lists;
import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.exception.BehaviorNodeExecuteException;
import com.lee9213.behavior.tree.node.IControlNode;
import com.lee9213.behavior.tree.node.INode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:36
 */
public abstract class AbstractControlNode<Result extends NodeResult, Context extends BaseContext> implements IControlNode<Result, Context> {
    protected String nodeName;
    protected String stepTag;
    protected List<INode<Result, Context>> childNodeList;

    public AbstractControlNode(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public INode<Result, Context> addChild(INode<Result, Context> childNode) {
        if (childNodeList == null) {
            childNodeList = Lists.newArrayList();
        }
        childNodeList.add(childNode);
        return null;
    }

    public void checkNodeResult(Result nodeResult) {
        if (nodeResult == null) {
            throw new BehaviorNodeExecuteException("节点执行结果为空");
        }
    }

    /**
     * Structural child nodes for validation and tooling. Empty when there are no children.
     */
    public List<INode<Result, Context>> getChildNodes() {
        if (childNodeList == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(childNodeList));
    }
    
    @Override
    public String getNodeName() {
        return nodeName;
    }
    
    @Override
    public String getStepTag() {
        return stepTag;
    }
    
    @Override
    public void setStepTag(String stepTag) {
        this.stepTag = stepTag;
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/AbstractControlNode.java
git commit -m "feat: modify AbstractControlNode to use List<INode> and implement new methods"
```

### 任务 4：修改控制节点实现类

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SequenceNodeImpl.java`
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SelectorNodeImpl.java`
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/ParallelNodeImpl.java`
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/RandomNodeImpl.java`
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/StrategyNodeImpl.java`

- [ ] **步骤 1：修改 SequenceNodeImpl**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.node.ISequenceNode;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * 顺序节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
@Log4j2
public final class SequenceNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements ISequenceNode<Result, Context> {
    public SequenceNodeImpl(String nodeName, List<INode<Result, Context>> childNodeList) {
        super(nodeName);
        this.childNodeList = childNodeList;
    }
    @Override
    public Result execute(Context context) {
        for (INode<Result, Context> node : childNodeList) {
            context.setCurrentNode(node);
            Result nodeResult = node.execute(context);
            checkNodeResult(nodeResult);
            if (nodeResult.isSuccess()) {
                log.info("节点{}执行结果：{}", node.getNodeName(), nodeResult);
                continue;
            }
            log.info("节点{}执行结果：{}，流程终止。", node.getNodeName(), nodeResult);
            // 如果有一个节点执行结果非成功，则直接返回执行结果
            return nodeResult;
        }
        // 如果所有节点都执行成功，则返回成功
        return (Result) NodeResult.SUCCESS;
    }
}
```

- [ ] **步骤 2：修改 SelectorNodeImpl**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.node.ISelectorNode;
import lombok.extern.log4j.Log4j2;

import java.util.List;

/**
 * 选择节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
@Log4j2
public final class SelectorNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements ISelectorNode<Result, Context> {
    public SelectorNodeImpl(String nodeName, List<INode<Result, Context>> childNodeList) {
        super(nodeName);
        this.childNodeList = childNodeList;
    }
    @Override
    public Result execute(Context context) {
        for (INode<Result, Context> node : childNodeList) {
            context.setCurrentNode(node);
            Result nodeResult = node.execute(context);
            checkNodeResult(nodeResult);
            if (!nodeResult.isSuccess()) {
                log.info("节点{}执行结果：{}", node.getNodeName(), nodeResult);
                continue;
            }
            log.info("节点{}执行结果：{}，流程终止。", node.getNodeName(), nodeResult);
            // 如果有一个节点执行结果成功，则直接返回执行结果
            return nodeResult;
        }
        // 如果所有节点都执行失败，则返回失败
        return (Result) NodeResult.FAILURE;
    }
}
```

- [ ] **步骤 3：修改 ParallelNodeImpl**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.node.IParallelNode;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 并行节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
@Log4j2
public final class ParallelNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements IParallelNode<Result, Context> {
    private Executor executor;

    public ParallelNodeImpl(String nodeName, List<INode<Result, Context>> childNodeList, Executor executor) {
        super(nodeName);
        this.childNodeList = childNodeList;
        this.executor = executor;
    }

    @Override
    public Result execute(Context context) {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(childNodeList.size());
        }
        Map<INode<Result, Context>, Future<Result>> futureMap = new ConcurrentHashMap<>();
        for (INode<Result, Context> node : childNodeList) {
            futureMap.put(node, executor.submit(() -> {
                Context childContext = (Context) context.clone();
                childContext.setCurrentNode(node);
                return node.execute(childContext);
            }));
        }
        for (Map.Entry<INode<Result, Context>, Future<Result>> entry : futureMap.entrySet()) {
            try {
                Result nodeResult = entry.getValue().get();
                checkNodeResult(nodeResult);
                if (!nodeResult.isSuccess()) {
                    log.info("节点{}执行结果：{}", entry.getKey().getNodeName(), nodeResult);
                    // 如果有一个节点执行结果失败，则直接返回失败
                    return nodeResult;
                }
                log.info("节点{}执行结果：{}", entry.getKey().getNodeName(), nodeResult);
            } catch (Exception e) {
                log.error("并行执行节点失败", e);
                return (Result) NodeResult.FAILURE;
            }
        }
        // 如果所有节点都执行成功，则返回成功
        return (Result) NodeResult.SUCCESS;
    }
}
```

- [ ] **步骤 4：修改 RandomNodeImpl**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.node.IRandomNode;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 随机节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
@Log4j2
public final class RandomNodeImpl<Result extends NodeResult,Context extends BaseContext> extends AbstractControlNode<Result, Context> implements IRandomNode<Result, Context> {
    private Random random = new Random();

    public RandomNodeImpl(String nodeName, List<INode<Result, Context>> childNodeList) {
        super(nodeName);
        this.childNodeList = childNodeList;
    }

    @Override
    public Result execute(Context context) {
        // 随机打乱子节点顺序
        Collections.shuffle(childNodeList, random);
        for (INode<Result, Context> node : childNodeList) {
            context.setCurrentNode(node);
            Result nodeResult = node.execute(context);
            checkNodeResult(nodeResult);
            log.info("节点{}执行结果：{}", node.getNodeName(), nodeResult);
        }
        // 随机节点不关心执行结果，直接返回成功
        return (Result) NodeResult.SUCCESS;
    }
}
```

- [ ] **步骤 5：修改 StrategyNodeImpl**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;
import com.lee9213.behavior.tree.node.INode;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * 策略节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
@Log4j2
public final class StrategyNodeImpl<Result extends NodeResult, Context extends BaseContext> extends AbstractControlNode<Result, Context> {
    private INode<Result, Context> conditionNode;
    private Map<Result, INode<Result, Context>> strategyMap;

    public StrategyNodeImpl(String nodeName, INode<Result, Context> conditionNode, Map<Result, INode<Result, Context>> strategyMap) {
        super(nodeName);
        this.conditionNode = conditionNode;
        this.strategyMap = strategyMap;
    }

    @Override
    public Result execute(Context context) {
        // 执行条件节点
        context.setCurrentNode(conditionNode);
        Result conditionResult = conditionNode.execute(context);
        checkNodeResult(conditionResult);
        log.info("条件节点{}执行结果：{}", conditionNode.getNodeName(), conditionResult);
        
        // 根据条件结果执行对应的策略节点
        INode<Result, Context> strategyNode = strategyMap.get(conditionResult);
        if (strategyNode == null) {
            log.info("未找到对应的策略节点，返回默认结果");
            return conditionResult;
        }
        
        context.setCurrentNode(strategyNode);
        Result strategyResult = strategyNode.execute(context);
        checkNodeResult(strategyResult);
        log.info("策略节点{}执行结果：{}", strategyNode.getNodeName(), strategyResult);
        
        return strategyResult;
    }
}
```

- [ ] **步骤 6：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SequenceNodeImpl.java
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SelectorNodeImpl.java
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/ParallelNodeImpl.java
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/RandomNodeImpl.java
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/StrategyNodeImpl.java
git commit -m "feat: modify control node implementations to use INode"
```

### 任务 5：修改动作节点实现类

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SuccessActionNodeImpl.java`
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/FailureActionNodeImpl.java`

- [ ] **步骤 1：修改 SuccessActionNodeImpl**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;

/**
 * 成功动作节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
public final class SuccessActionNodeImpl<Result extends NodeResult, Context extends BaseContext> extends AbstractActionNode<Result, Context> {
    public SuccessActionNodeImpl(String nodeName) {
        super(nodeName);
    }

    @Override
    public Result execute(Context context) {
        return (Result) NodeResult.SUCCESS;
    }
}
```

- [ ] **步骤 2：修改 FailureActionNodeImpl**

```java
package com.lee9213.behavior.node.impl;

import com.lee9213.behavior.tree.BaseContext;
import com.lee9213.behavior.tree.NodeResult;

/**
 * 失败动作节点的实现
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:14
 */
public final class FailureActionNodeImpl<Result extends NodeResult, Context extends BaseContext> extends AbstractActionNode<Result, Context> {
    public FailureActionNodeImpl(String nodeName) {
        super(nodeName);
    }

    @Override
    public Result execute(Context context) {
        return (Result) NodeResult.FAILURE;
    }
}
```

- [ ] **步骤 3：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/SuccessActionNodeImpl.java
git add behavior-tree-core/src/main/java/com/lee9213/behavior/node/impl/FailureActionNodeImpl.java
git commit -m "feat: modify action node implementations to use AbstractActionNode"
```

### 任务 6：修改 BaseContext

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/BaseContext.java`

- [ ] **步骤 1：修改 BaseContext，使用 INode 替代 BehaviorNodeWrapper**

```java
package com.lee9213.behavior;

import com.lee9213.behavior.tree.node.INode;
import lombok.Data;

import java.io.Serializable;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:27
 */
@Data
public class BaseContext implements Serializable {

    protected INode currentNode;
}
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/BaseContext.java
git commit -m "feat: modify BaseContext to use INode instead of BehaviorNodeWrapper"
```

### 任务 7：修改 BehaviorTree

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorTree.java`

- [ ] **步骤 1：修改 BehaviorTree，使用 INode 替代 BehaviorNodeWrapper**

```java
package com.lee9213.behavior;

import com.lee9213.behavior.tree.node.INode;
import lombok.Data;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 14:12
 */
@Data
public class BehaviorTree<Result extends NodeResult, Context extends BaseContext> {
    private INode<Result, Context> rootNode;
    public BehaviorTree() { }
    public BehaviorTree(INode<Result, Context> rootNode) {
        this.rootNode = rootNode;
    }

    public Result execute(Context context) {
        context.setCurrentNode(rootNode);
        return rootNode.execute(context);
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorTree.java
git commit -m "feat: modify BehaviorTree to use INode instead of BehaviorNodeWrapper"
```

### 任务 8：创建 NodeFactory

**文件：**
- 创建：`behavior-tree-core/src/main/java/com/lee9213/behavior/NodeFactory.java`

- [ ] **步骤 1：创建 NodeFactory 类，提供工厂方法**

```java
package com.lee9213.behavior;

import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.node.impl.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 节点工厂类
 *
 * @author lee9213@163.com
 * @date 2024/5/30 14:15
 */
public class NodeFactory {
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createSequenceNode(String nodeName, List<INode<Result, Context>> childNodeList) {
        return new SequenceNodeImpl<>(nodeName, childNodeList);
    }
    
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createSelectorNode(String nodeName, List<INode<Result, Context>> childNodeList) {
        return new SelectorNodeImpl<>(nodeName, childNodeList);
    }
    
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createParallelNode(String nodeName, List<INode<Result, Context>> childNodeList) {
        return new ParallelNodeImpl<>(nodeName, childNodeList, null);
    }
    
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createParallelNode(String nodeName, List<INode<Result, Context>> childNodeList, Executor executor) {
        return new ParallelNodeImpl<>(nodeName, childNodeList, executor);
    }
    
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createRandomNode(String nodeName, List<INode<Result, Context>> childNodeList) {
        return new RandomNodeImpl<>(nodeName, childNodeList);
    }
    
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createStrategyNode(String nodeName, INode<Result, Context> conditionNode, Map<Result, INode<Result, Context>> strategyMap) {
        return new StrategyNodeImpl<>(nodeName, conditionNode, strategyMap);
    }
    
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createSuccessNode() {
        return new SuccessActionNodeImpl<>("DefaultSuccessNode");
    }
    
    public static <Result extends NodeResult, Context extends BaseContext> INode<Result, Context> createFailureNode() {
        return new FailureActionNodeImpl<>("DefaultFailureNode");
    }
}
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/NodeFactory.java
git commit -m "feat: create NodeFactory class to provide node creation methods"
```

### 任务 9：修改 BehaviorTreeDefinitionLoader

**文件：**
- 修改：`behavior-tree-core/src/main/java/com/lee9213/behavior/definition/assemble/DefinitionAssembler.java`

- [ ] **步骤 1：修改 DefinitionAssembler，使用 INode 替代 BehaviorNodeWrapper**

```java
// 修改文件，将所有 BehaviorNodeWrapper 替换为 INode
// 具体修改内容根据实际代码而定
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/main/java/com/lee9213/behavior/definition/assemble/DefinitionAssembler.java
git commit -m "feat: modify DefinitionAssembler to use INode instead of BehaviorNodeWrapper"
```

### 任务 10：修改测试文件

**文件：**
- 修改：`behavior-tree-core/src/test/java/com/lee9213/behavior/BehaviorTreeTest.java`

- [ ] **步骤 1：修改 BehaviorTreeTest，使用 INode 替代 BehaviorNodeWrapper**

```java
package com.lee9213.behavior;

import com.lee9213.behavior.tree.definition.BehaviorTreeDefinitionLoader;
import com.lee9213.behavior.tree.definition.DefinitionFormat;
import com.lee9213.behavior.tree.definition.resolve.CompositeActionNodeResolver;
import com.lee9213.behavior.tree.definition.resolve.ReflectionActionNodeResolver;
import com.lee9213.behavior.tree.node.INode;
import com.lee9213.behavior.tree.spring.annotation.EnableBehavior;
import com.lee9213.behavior.tree.definition.resolve.SpringBeanActionNodeResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author lee9213@163.com
 * @date 2024/5/30 15:09
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class BehaviorTreeTest {

    @Test
    public void execute() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/definitions/golden.json")) {
            CompositeActionNodeResolver resolver = new CompositeActionNodeResolver(
                    new SpringBeanActionNodeResolver(),
                    new ReflectionActionNodeResolver());
            BehaviorTreeDefinitionLoader loader = new BehaviorTreeDefinitionLoader(resolver);
            INode<TestNodeResult, BaseContext> root =
                    loader.parse(in, StandardCharsets.UTF_8, DefinitionFormat.JSON, TestNodeResult.class);
            TestContext testContext = new TestContext();
            BehaviorTree<TestNodeResult, BaseContext> behaviorTree = new BehaviorTree<>(root);
            behaviorTree.execute(testContext);
        }
    }
}

@Configuration
@EnableBehavior
@ComponentScan("com.lee9213.behavior")
class TestConfiguration {

}
```

- [ ] **步骤 2：Commit**

```bash
git add behavior-tree-core/src/test/java/com/lee9213/behavior/BehaviorTreeTest.java
git commit -m "feat: modify BehaviorTreeTest to use INode instead of BehaviorNodeWrapper"
```

### 任务 11：删除 BehaviorNodeWrapper

**文件：**
- 删除：`behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorNodeWrapper.java`

- [ ] **步骤 1：删除 BehaviorNodeWrapper 类**

```bash
rm behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorNodeWrapper.java
```

- [ ] **步骤 2：Commit**

```bash
git rm behavior-tree-core/src/main/java/com/lee9213/behavior/BehaviorNodeWrapper.java
git commit -m "feat: remove BehaviorNodeWrapper class"
```

### 任务 12：运行测试

**文件：**
- 测试：`behavior-tree-core/src/test/java/com/lee9213/behavior/BehaviorTreeTest.java`

- [ ] **步骤 1：运行测试，确保所有功能正常**

```bash
cd behavior-tree
mvn test -Dtest=BehaviorTreeTest
```

- [ ] **步骤 2：Commit**

```bash
git add .
git commit -m "feat: run tests to verify functionality"
```

---

## 执行交接

**计划已完成并保存到 `docs/superpowers/plans/2026-04-13-behavior-node-wrapper-optimization.md`。两种执行方式：**

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

**选哪种方式？**