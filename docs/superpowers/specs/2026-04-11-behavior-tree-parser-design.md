# 行为树定义解析 — 全量重构规格

**日期**：2026-04-11  
**状态**：已确认方向（全新分层、不锚定旧 `parser` 实现；允许主版本破坏性变更；程序化 API + Spring 配置并存）

---

## 1. 背景与目标

旧版 `com.lee9213.behavior.parser`（`JsonNodeParser` + 若干 `*NodeParser`）已移除；原 JSON → `BehaviorNodeWrapper` 的职责由新规格中的 **IR + 语法 codec + DefinitionAssembler** 承接。

本规格要求 **整体重构**：以 **领域中间表示（IR）** 为中心，**JSON / XML 仅作为语法前端**，经 **统一语义装配层** 产出 `BehaviorNodeWrapper`，并同时支持 **无 Spring 的 API** 与 **Spring Boot 配置驱动加载**。

**成功标准**

- 提供 **统一入口**：由字符串或流 + **格式枚举** 解析为运行时树；错误类型可区分「语法层」与「语义层」。
- **JSON 与 XML 绑定到同构 IR**，对等价定义得到一致装配结果（可用黄金用例校验）。
- Spring 下可通过 **属性** 指定资源位置、格式、编码等，与 `@EnableBehavior` / 动作 Bean 解析 **正交**（互不替代）。
- 旧 `parser` 包 **不作兼容期保留**，与主版本一并删除；迁移说明见 §8。

**非目标（本版本）**

- 除 JSON/XML 外的第三格式（YAML 等）可预留扩展点，不强制实现。
- 不在本规格中承诺替换 Fastjson 的全库迁移；**新解析栈**推荐在实现计划中选用 **Jackson**（JSON + XML 数据绑定）以降低双栈维护成本，最终以技术选型小节为准。

---

## 2. 架构总览

单向依赖关系如下（仅概念，包名实现时可微调）：

```text
语法层 (JSON | XML)  →  TreeDefinition（IR）  →  DefinitionAssembler  →  BehaviorNodeWrapper
                              ↑
                    ActionNodeResolver（可选，对接 Spring Bean）
```

| 层级 | 职责 | 禁止事项 |
|------|------|----------|
| **语法层** | 将文本/字节流解析为 **不可变 IR** | 不出现 `BehaviorNodeWrapper`、不调用控制节点具体实现类 |
| **IR** | 表达节点类型、名称、动作引用、子节点、策略映射等 | 不包含执行语义，仅数据结构 |
| **语义装配层** | IR → `BehaviorNodeWrapper`，按 `NodeType` 构建 `Sequence`/`Parallel`/… | 不解析 JSON/XML 字符串 |
| **动作解析** | 将 IR 中的「动作引用」解析为 `IActionNode` 实例 | 通过接口注入，核心可带「反射类名」默认实现，Spring 提供基于 `ApplicationContext` 的实现 |

---

## 3. 领域模型（IR）

- 使用 **不可变** 结构为主（Java `record` 或 Builder + `final` 字段），避免与旧 `Node` Lombok 可变对象混用。
- 字段语义需覆盖现有能力：`nodeType`、`nodeName`、动作引用（`beanName` / `container` 等与现有一致或显式重命名并在迁移文档列出）、`children`、`condition`、`strategyMap`（策略键与 `NodeResult` 编码的映射规则需在 IR 中明确类型，例如 `String` → 由装配层调用 `resultClazz` 转为具体 `Result`）。
- **命名**：实现时可采用 `TreeDefinitionNode` / `BehaviorDefinition` 等名称，规格只约束「单一 IR 树、无执行状态」。

---

## 4. 语法层

### 4.1 JSON

- 输入：`String` 或 `InputStream` + `Charset`（默认 UTF-8）。
- 输出：`TreeDefinition`（根节点）。
- 与 XML **同构**：同一套 IR 字段，仅序列化形式不同。

### 4.2 XML

- 输入：同上。
- 输出：同构 IR。
- **元素/属性约定**（实现计划需给出 XSD 或示例片段，规格层要求）：
  - 根元素表示一棵树；子节点通过嵌套元素或统一子元素列表表达，**只能选一种风格**并在文档中写死，避免歧义。
  - `NodeType`、动作引用等与 JSON 字段 **一一对应**。

### 4.3 格式枚举

- 例如 `DefinitionFormat { JSON, XML }`，供 API 与 Spring 配置共用。

---

## 5. 语义装配层

- **DefinitionAssembler**（名称可调整）：输入 **根 IR 节点** + **`Class<? extends NodeResult> resultClazz`**，输出根 `BehaviorNodeWrapper`。
- 按 `NodeType` **注册表** 分派到各子构建器（Sequence / Parallel / Selector / Random / Strategy / Action），逻辑与旧 `*NodeParser` **行为等价**，但 **不继承**旧类、不复用旧类名。
- **Parallel**：若 IR 支持可选 `executor` 引用（Bean 名或占位），与现有 `ParallelNodeImpl(children, Executor)` 对齐；未配置则顺序执行（与当前默认一致）。若首版 IR 不表达 executor，规格允许 **首版仅顺序**，executor 作为后续增强项（实现计划必须显式写清）。

---

## 6. 对外 API（无 Spring）

- **BehaviorTreeDefinitionLoader**（或等价名称）：
  - `parse(String content, DefinitionFormat format, Class<?> resultClazz)`
  - `parse(InputStream in, Charset charset, DefinitionFormat format, Class<?> resultClazz)`
- 内部顺序：**语法 codec** → **Assembler**。
- **异常**：
  - **DefinitionSyntaxException**（或同类名）：JSON/XML 无法读或无法映射到 IR。
  - **DefinitionAssemblyException**：IR 合法但无法构建树（缺字段、未知类型、动作解析失败等）。

---

## 7. Spring 集成

- **配置属性**（前缀示例 `behavior.definition`，实现可微调）：
  - `format`：`json` | `xml`
  - `location`：Spring `Resource` 语义（如 `classpath:behavior/tree.xml`）
  - `charset`：可选，默认 UTF-8
- **Bean**：提供 `BehaviorTreeDefinitionLoader` 或按需暴露「已解析的根 `BehaviorNodeWrapper`」之一；规格推荐 **Loader Bean** 更通用。
- **条件装配**：仅在存在配置或资源时创建，避免无定义时启动失败；具体条件在实现计划中列出。
- **与 `@EnableBehavior`**：保留（包名 **`com.lee9213.behavior.spring`**，导入 `SpringNodeUtil`）；动作用 `IActionNode` Bean 名解析的逻辑由 **ActionNodeResolver** 的 Spring 实现提供，**不**与定义加载混在一个类中。

---

## 8. 迁移与废弃

- 旧包 **`com.lee9213.behavior.parser.*` 已删除**，不设 **@Deprecated** 过渡期；调用方迁移至 **Loader API + 新包**（实现阶段落地）。
- **README** 与 **CHANGELOG**（若有）需列出破坏性变更与示例替换代码。

---

## 9. 测试策略

- **单元测试**：各 `NodeType` 装配；动作解析 Mock。
- **契约测试**：同构 IR 下 JSON 与 XML 文本解析后装配结果一致（或树结构等价断言）。
- **Spring 测试**（可选）：`@SpringBootTest` 加载 `location` 与 `format`。

---

## 10. 实现计划衔接

本规格通过后，使用 **writing-plans** 编写实现计划：含包结构重构清单、依赖选型（Jackson 等）、任务拆分与顺序、删除旧代码的提交节点。

---

## 11. 修订记录

| 日期 | 变更 |
|------|------|
| 2026-04-11 | 初稿：全量重构、IR 中心、JSON/XML、API + Spring、废弃旧 parser |
| 2026-04-11 | 修订：旧 `parser` 包直接删除；`@EnableBehavior` 迁至 `com.lee9213.behavior.spring` |
