# 附魔机制总览

Aiyatsbus 支持 5 类附魔机制。先根据效果来源选择机制，再阅读对应参考文档。

| 类型 | YAML 节点 | 适用场景 | 参考文档 |
| --- | --- | --- | --- |
| Listener | `mechanisms.listeners` | 监听 Bukkit 事件并执行脚本 | `listener-trigger.md` |
| Ticker | `mechanisms.tickers` | 按固定 tick 间隔执行脚本 | `ticker-trigger.md` |
| Skill | `mechanisms.skills` | 由左键、右键或切换主副手主动触发脚本 | `skill-trigger.md` |
| Builtin | `mechanisms.builtin` | 使用 Java 编写附魔效果 | `builtin-trigger.md` |
| Artifact | `mechanisms.artifact` | 快速配置粒子附魔 | `artifact-trigger.md` |

## 选择规则

- 用户要求使用 Java 编写效果时，使用 `Builtin`，不要生成脚本触发器。
- 用户明确要求脚本，或现有附魔已经使用脚本时，再选择 Listener、Ticker 或 Skill。
- 效果由 Bukkit 事件触发时使用 Listener。
- 效果需要持续检查装备或周期执行时使用 Ticker。
- 效果由玩家主动按键触发并需要冷却时使用 Skill。
- 效果只有粒子展示时优先使用 Artifact。

## 配置结构

```yaml
mechanisms:
  priority-listener: 0
  priority-ticker: 0
  priority-skill: 0

  listeners: { }
  tickers: { }
  skills: { }
  builtin: "com.example.enchant.CustomBuiltin"
  artifact: { }
```

不要为了展示结构而把全部节点同时写入实际附魔。只保留效果需要的机制。

## 数量与组合

- `listeners`、`tickers` 和 `skills` 下可以分别定义多个具名触发器。
- `builtin` 和 `artifact` 是唯一类型，同一附魔中每类只能存在一个。
- 不同类型可以共存，例如一个附魔可以同时拥有 Listener 和 Ticker。
- 关闭或重载附魔时，`Mechanism.close()` 会关闭并移除已经注册的触发器。

## 优先级

`priority-listener`、`priority-ticker` 和 `priority-skill` 是处理器实际使用的机制类型级优先级，默认值为 `0`。数值越小，附魔之间越早参与对应类型的处理。

通用 `Mechanism.priority(...)` 虽然可以读取其他类型的优先级键，但当前 Builtin 和 Artifact 执行器没有使用 `priority-builtin` 或 `priority-artifact` 排序，不要把它们作为有效配置生成。

Listener 和 Skill 还支持各自的 `priority`，用于排列同一附魔内的多个同类触发器。Ticker 当前没有单独的 `priority` 字段，使用 `priority-ticker` 排列附魔。

## 唯一脚本系统

当前插件只使用 Fluxon 一种脚本系统。Listener、Ticker 和 Skill 通常省略 `type`，不要主动生成冗余的 `type: FLUXON`，也不要根据底层历史字段或枚举生成 Kether、JavaScript 脚本。

脚本触发器会在初始化时预热脚本，并在执行前调用 `AiyatsbusEnchantmentExecuteEvent`。事件被取消时，对应脚本不会继续执行。

`AiyatsbusEnchantmentExecuteEvent` 只包围对应的脚本块，不代表整个触发流程都能回滚。特别是 Skill 会先记录冷却、播放声音和生成粒子，然后才进入 `handle` 的执行事件；取消该事件只阻止脚本，不会撤销这些先行副作用。Builtin 和 Artifact 不通过脚本块执行，也不使用这个脚本执行事件。

## 自动门控矩阵

不同机制的自动检查不相同，不要因为某个字段存在就假定所有机制都会使用它。

| 检查或行为 | Listener | Ticker | Skill | Builtin | Artifact |
| --- | --- | --- | --- | --- | --- |
| `basic.enable` / 常规可用性 | 通过 `CheckType.USE` | 通过 `CheckType.USE` | 通过 `CheckType.USE` | 显式检查并执行 `CheckType.USE` | 与 Builtin 共用 |
| active slots | 由事件映射槽位和限制检查决定 | 遍历目标的 active slots | 固定检查主手 | 由事件分发槽位决定 | 与 Builtin 共用 |
| NPC 排除 | 事件实体解析阶段 | 在线玩家任务 | 显式排除 | 取决于上游事件分发 | 与 Builtin 共用 |
| `chance` / `概率` 自动随机 | 否 | 否 | 否 | 是 | 是 |
| 自动冷却 | 否 | 否 | 是 | 否 | 否 |
| `AiyatsbusEnchantmentExecuteEvent` | 每次 `handle` | 每个 pre/handle/post | 仅 `handle` | 否 | 否 |

Builtin 和 Artifact 会按以下顺序读取概率变量：优先使用名为 `chance` 的 LEVELED 变量，其次使用 `概率`，都不存在时按 `100.0` 处理。Listener、Ticker 和 Skill 不会自动读取这两个变量；需要概率时必须在脚本中显式判断。概率值位于 `0.0..1.0` 时按比例解释，例如 `0.5` 是 50%；大于 `1.0` 时按百分数解释，例如 `25.5` 是 25.5%。当前执行器还会强制把概率结果转换为 `Double`，但 LEVELED 的整数结果会返回 `Int`；具体限制和示例见 `artifact-trigger.md`。

## 加载失败模型

机制没有统一的 YAML schema 预校验。构造器会直接读取必填字段和枚举，因此生成配置时必须主动保证：

- Listener 的 `listen` 存在且对应运行时已注册的事件映射。
- Listener、Ticker 和 Skill 省略 `type`。如果维护历史配置时显式保留该字段，它必须为 `FLUXON`；不要填写其他脚本类型。
- Skill 的 `action` 若显式填写，取值集合只能是 `RIGHT_CLICK`、`LEFT_CLICK` 或 `SWAP`。解析时忽略大小写，但生成配置统一使用这里的大写形式。
- `mechanisms.builtin` 指定的类可以加载、继承 `Builtin`，并具有 `(AiyatsbusEnchantment)` 构造器。
- Ticker 的 `interval` 至少为 `1`。

Listener、Ticker、Skill、Builtin、Artifact 在同一个初始化保护块中按此顺序构造。任一机制构造或初始化抛出异常后，本次 `Mechanism.init()` 会立即离开保护块，同一附魔后面的机制一定不会继续初始化。空 `handle`、`pre-handle` 或 `post-handle` 会成为合法的空脚本；缺少必填字段或填写非法枚举则属于构造失败。

失败后果取决于当前生命周期：

- 在服务器启动、尚未进入 `LifeCycle.ACTIVE` 时，保护器会打印严重错误，等待约 5 秒并调用 `Runtime.halt(-1)` 强制终止进程，以避免数据损坏。
- 在已经进入 `LifeCycle.ACTIVE` 的热重载阶段，异常会被保护器捕获，不会走强制停服分支；但本次机制初始化仍已中断，后续机制不会注册。

因此机制字段错误不是普通的“该效果不触发”。启动前必须严格验证配置，尤其是 `listen`、历史配置中显式存在的 `type`、`action`、`interval` 和 Builtin 类。

脚本预热失败会记录警告，但不一定阻止触发器注册。实际触发时脚本处理器可能再次尝试编译，因此“附魔已加载”不等于每个脚本块都已成功编译。排错时必须同时检查加载日志、预热异常和首次触发时的脚本异常。

## 触发器 ID

Listener、Ticker 和 Skill 的配置 ID 会参与脚本缓存 ID。内部会把 `-` 替换为 `_`，因此同一附魔、同一机制类型中的 `on-hit` 和 `on_hit` 会发生内部 ID 碰撞。ID 在该归一化后仍必须唯一；最简单的规则是统一只使用一种分隔符，不混用 `-` 和 `_`。

## Fluxon 脚本载体

所有脚本必须遵循 `fluxon-language.md`。调用 Bukkit 或其他 Java API 时还必须遵循 `fluxon-bukkit-java-semantics.md`。需要函数、扩展 API、JVM 互操作或模块时，再分别读取 `fluxon-stdlib.md`、`fluxon-jvm-interop.md` 和 `fluxon-modules.md`。

Fluxon 只能写在以下 YAML 脚本块中：

- Listener 的 `handle: |-`
- Ticker 的 `pre-handle: |-`
- Ticker 的 `handle: |-`
- Ticker 的 `post-handle: |-`
- Skill 的 `handle: |-`

不能为 Aiyatsbus 附魔创建独立 `.fs` 文件，也不能把脚本块拆分到外部 `.fs` 文件。Builtin 使用 Java，Artifact 只使用数据配置，两者都不写 Fluxon。

## 默认资源的语法优先级

项目内默认附魔 YAML 包含部分历史 Fluxon 写法，例如裸枚举名称或旧式成员链。它们可以用于理解机制结构和业务流程，但不能作为新脚本的语法规范。

生成或修改 Fluxon 脚本时，语法优先级固定为：

1. 当前开发者明确提供的项目规范。
2. `fluxon-language.md` 和 `fluxon-bukkit-java-semantics.md`。
3. `aiyatsbus-fluxon-functions.md`。
4. 默认附魔 YAML 中的历史示例。

默认 YAML 与严格语义冲突时，不要照抄历史写法。无参数 getter 必须去掉 `get`，有参数 getter 必须保留 `get`；十个跨版本兼容函数优先于 `static`，其他 Bukkit 或 Java 枚举使用 `static`；Java 嵌套类必须用 `$` 连接外部类和嵌套类。
