---
name: aiyatsbus-create-enchant-skill
description: >
  用于创建或修改 Aiyatsbus 附魔，包括通过
  BuiltinAiyatsbusEnchantment.builder() 编写 Java 纯代码附魔、通过 YAML
  定义附魔信息并使用 Java Builtin 编写触发器，以及编写 Fluxon 附魔脚本。
---

# 创建 Aiyatsbus 附魔

仅在创建或修改 Aiyatsbus 附魔时使用本技能。Aiyatsbus 提供三种附魔开发方式，开始实现前必须先确定使用哪一种。三种开发方式回答“附魔信息和效果由什么定义”，五类运行机制回答“效果如何触发”，不要混为一谈。

## 三种开发方式

| 开发方式 | 附魔基础信息 | 效果实现 | 是否依赖附魔 YAML | 必须阅读 |
| --- | --- | --- | --- | --- |
| 纯代码附魔 | Java Builder | Java `EventFunctions` 回调 | 否 | `references/code-defined.md` |
| YAML + Builtin | YAML | Java `Builtin` 类 | 是 | `references/config-defined.md` 和 `references/builtin-trigger.md` |
| YAML + Fluxon | YAML | Fluxon Listener、Ticker、Skill | 是 | `references/config-defined.md`、`references/mechanisms.md` 和对应脚本触发器文档 |

## 按需求阅读

| 需求 | 需要阅读 |
| --- | --- |
| 使用 YAML 和 Bukkit 事件脚本 | `references/listener-trigger.md` |
| 使用 YAML 编写周期脚本 | `references/ticker-trigger.md` |
| 使用 YAML 编写主动技能 | `references/skill-trigger.md` |
| 只需要配置粒子附魔 | `references/artifact-trigger.md`；Artifact 是 YAML 纯配置辅助机制，不是第四种开发方式 |
| 使用 Fluxon 游戏函数、冷却、变量或弓蓄力事件 | `references/aiyatsbus-fluxon-functions.md` |
| 在 Fluxon 中调用 Bukkit 或其他 Java API | `references/fluxon-bukkit-java-semantics.md` 和 `references/fluxon-jvm-interop.md` |
| 不清楚应该选择哪种触发器 | `references/mechanisms.md` |
| 不清楚字段、变量、对象、限制或回调 | 对应参考文档，然后核对项目源码 |

## 必须遵循的事实

- 代码定义和 Builtin 触发器示例使用 Java；YAML 脚本块使用对应的 Fluxon 语法。Aiyatsbus 源码使用 Kotlin，但本技能涉及的公开 Java API 均可由 Java 调用。`BuiltinAiyatsbusEnchantment.builder()` 已通过 `@JvmStatic` 暴露为 Java 静态方法，`addVariable(...)` 已通过 `@JvmOverloads` 提供 Java 重载。`Aiyatsbus` 是 Kotlin `object`，Java 必须通过 `Aiyatsbus.INSTANCE.api()` 获取 API。
- `BuiltinAiyatsbusEnchantment` 是接口，不要继承它，也不要直接实例化抽象类 `AiyatsbusEnchantmentBase`。
- 纯代码附魔从 `BuiltinAiyatsbusEnchantment.builder()` 开始创建。
- `mechanisms.builtin` 指向继承 `cc.polarastrum.aiyatsbus.core.data.trigger.builtin.Builtin` 的 Java 触发器类，不是附魔类。
- YAML + Fluxon 使用 Listener、Ticker 和 Skill；它不需要额外 Java 触发器类。Artifact 可以与它们共存，但 Artifact 自身不执行 Fluxon。
- Aiyatsbus 有 Listener、Ticker、Skill、Builtin 和 Artifact 共 5 类运行机制。用户要求 Java 效果时使用纯代码附魔或 YAML + Builtin，不要改写成脚本机制。
- 当前插件只使用 Fluxon 一种脚本系统。Listener、Ticker 和 Skill 通常省略 `type`；不要生成冗余的 `type: FLUXON`，也不要生成 Kether 或 JavaScript 脚本。
- YAML 附魔由 Aiyatsbus 从附魔目录自动加载。除非用户明确要求独立托管系统，否则不要重复实现 YAML 加载器。
- `basic.id` 是运行时唯一标识，必须能够构造 Bukkit `NamespacedKey`。YAML 文件名可以与它不同。
- `rarity`、`targets`、`limitations`、`display` 和 `mechanisms` 均为根节点，不属于 `basic`。
- 技能文档正文遵循盘古之白，但生成的附魔 `general` 和 `specific` 描述字符串不遵循盘古之白。不要在描述中的中文、数字、变量占位符或颜色符号之间人为插入空格。附魔描述应简短，但可读性永远优先于长度；必须保留理解效果所需的触发条件、作用对象和关键数值。
- `general` 默认使用不含颜色和变量占位符的自然语言概括；除非开发者明确要求，否则不要在 `general` 中放变量。具体数值和变量放在 `specific`。
- `ORDINARY` 和 `MODIFIABLE` 无法配置展示单位。变量表示有单位的数值时，`specific` 必须尽量补出单位，并让单位与变量保持同色，例如 `&7持续&a{test}秒&7才停止`。不要写成 `&a{test}&7秒`，也不要遗漏必要单位。`LEVELED` 已配置非空单位时不要重复手写单位。
- 伤害、倍率、概率、持续时间、冷却、范围、半径、速度、数量和消耗等可调玩法数值，能做成附魔变量时必须尽量变量化，不要直接硬编码在 Java 或 Fluxon 效果中。数值变量默认优先使用 `LEVELED`；没有要求随等级变化时也使用固定表达式，例如 `伤害提升: "%:30"`，不因数值固定而改用 ORDINARY。
- 只有需要写入物品 NBT/PDC、会在运行时变化的累计值或状态才使用 `MODIFIABLE`；布尔开关、字符串、枚举名、列表和其他非数值固定配置使用 `ORDINARY`。结构性字面量、坐标分量、集合索引、协议常量和机制 schema 自身要求的数字不强制变量化。
- 复制而来的旧网站文档与源码冲突时，以源码为准。权威源码入口见 `references/overview.md`。
- 项目默认附魔 YAML 可能包含历史 Fluxon 写法，只能用于参考机制结构和业务逻辑。新脚本必须遵循 `references/fluxon-language.md` 和 `references/fluxon-bukkit-java-semantics.md`；冲突时不得照抄默认资源中的裸枚举、旧 getter 或旧成员链。

## 规范优先级

如果开发者在当前请求、项目规范、已有代码或已有配置中明确提供了自定义的附魔描述格式规范，开发者提供的规范优先于本技能中的默认规范。

本技能的描述格式只在没有更高优先级的自定义规范时生效。不要在开发者明确要求其他格式时强行套用本技能的 `general`、`specific`、颜色或变量单位规则。

### Fluxon 脚本编写

1. 生成的所有 Fluxon 代码必须严格遵循 `references/fluxon-language.md` 中的语法规则。
2. 变量读取**必须**使用 `&name` 引用运算符，裸标识符是字符串字面量。
3. 优先使用 `::` 扩展函数，非必要不用 `.` 反射访问。
4. 参考 `references/fluxon-stdlib.md` 中的内置函数和扩展函数 API。
5. 参考 `references/fluxon-jvm-interop.md` 中的 JVM 互操作语法。
6. 参考 `references/fluxon-modules.md` 中的标准库模块 API。
7. 参考 `references/aiyatsbus-fluxon-functions.md` 中的 Aiyatsbus 游戏函数、冷却、变量和弓蓄力事件扩展。
8. 调用 Bukkit 或其他 Java API 时，必须遵循 `references/fluxon-bukkit-java-semantics.md`：无参数 getter 去掉 `get`，有参数 getter 保留 `get`，setter 保留 `set`；十个跨版本兼容函数优先于 `static`，其余枚举和静态成员使用 `static`，构造器使用 `new`；Java 嵌套类必须用 `$` 连接外部类和嵌套类。
9. Fluxon 脚本只能写在附魔 YAML 的 `handle: |-`、`pre-handle: |-` 或 `post-handle: |-` 脚本块中，不能创建独立的 `.fs` 文件。
10. 复杂触发器必须核对对应文档中的实际参与者、槽位类型、短路行为、自动门控、副作用顺序、重载限制和加载失败模型，不能只根据字段名推测运行语义。
11. 优先使用上述 Fluxon 参考中已经提供的语言能力、标准库、模块、Aiyatsbus 函数和跨版本 Bukkit 函数。`fs:time`、`fs:crypto`、`fs:reflect`、`fs:io`、`fs:jvm` 在 Aiyatsbus 附魔脚本中已经自动导入，通常不需要手动 `import`；例如获取当前毫秒时间戳直接调用 `now()`，不要调用 `static java.lang.System.currentTimeMillis()`。
12. 默认依赖附魔对象的 active slots 和 `Limitations` 完成槽位检查；没有明确的槽位差异需求时不要判断 `triggerSlot`。执行者允许为任意 `LivingEntity` 时不要额外调用 `entity::isPlayer`；只有效果明确要求玩家或后续必须调用 Player 专属 API 时才判断。
13. 写效果前先提取可调玩法数值：默认定义为 LEVELED，未要求等级成长时使用固定 LEVELED 表达式；需要物品持久化时才改用 MODIFIABLE，布尔或非数值配置才使用 ORDINARY。脚本、Java 效果和 `specific` 必须读取同一变量，不要在不同位置重复硬编码数值。
14. Fluxon 触发器会提供 `container` 和 `globalContainer`。单一用途时直接使用整个 `container`，多个用途时才在其中嵌套 Map；容器只保存运行时内存状态，不替代 `MODIFIABLE`、PDC、NBT、数据库或其他持久化系统。

通用 Fluxon 参考中关于 `.fs` 文件、独立脚本入口和脚本库文件的能力不适用于 Aiyatsbus 附魔。不要将 YAML 中的脚本块拆分到外部文件，也不要调用加载外部 `.fs` 文件的 API。

### Fluxon 脚本语言

- `references/fluxon-language.md` -- 语法规则、变量引用、常量、解构赋值、运算符优先级、函数定义、Lambda、控制流、上下文调用 `::`
- `references/fluxon-stdlib.md` -- 全局内置函数（系统/类型转换/数学）、扩展函数速查表（String/Collection/Iterable/List/Map）、Domain 表达式
- `references/fluxon-jvm-interop.md` -- JVM 互操作：`.` 反射访问、`static` 静态成员、`new` 构造、嵌套类 `$` 名称、`impl` 匿名实现、并发异步（async/sync/await/scope）、注解系统
- `references/fluxon-bukkit-java-semantics.md` -- Bukkit/Java 调用语义：getter、setter、十个跨版本类型函数、其他枚举与静态成员、嵌套类 `$` 名称、构造器
- `references/fluxon-modules.md` -- 自动导入模块：fs:time（时间）、fs:io（文件/路径）、fs:crypto（加密/编码）、fs:jvm（字节码注入）、fs:reflect（反射）
- `references/aiyatsbus-fluxon-functions.md` -- Aiyatsbus 自动导入模块：block、cooldown、entity、guard、inventory、item、player、world、common（包括 debug）、variables，以及弓蓄力事件扩展
- `references/fluxon-platform-functions.md` -- Fluxon 平台函数：String 扩展、任务调度、任务取消和 Folia 区域调度
- `references/fluxon-containers.md` -- Fluxon 触发器运行时容器：附魔私有 `container`、插件全局 `globalContainer`、单用途与多用途结构、生命周期和持久化边界

## 工作流程

1. 先在纯代码附魔、YAML + Builtin、YAML + Fluxon 中选择一种开发方式；只需要预设粒子时仍属于 YAML 配置方式，使用 Artifact。
2. 再读取 `references/mechanisms.md` 选择运行机制，并读取所选开发方式需要的参考文档。
3. 纯代码附魔读取 `references/code-defined.md`；YAML + Builtin 读取 `references/config-defined.md` 和 `references/builtin-trigger.md`；YAML + Fluxon 读取 `references/config-defined.md` 和对应的 Listener、Ticker、Skill 文档。
4. 使用 Fluxon 时，先读取 `references/fluxon-language.md`；调用 Bukkit 或其他 Java API 时读取 `references/fluxon-bukkit-java-semantics.md`；涉及游戏逻辑、冷却、变量、背包或弓蓄力事件时读取 `references/aiyatsbus-fluxon-functions.md`；涉及 String 扩展、任务调度或 Folia 区域调度时读取 `references/fluxon-platform-functions.md`；需要跨触发器保存运行时状态时读取 `references/fluxon-containers.md`，再按需读取标准库、JVM 互操作或模块参考。
5. Listener 核对映射参与者、物品来源、槽位和取消策略；Ticker 核对 interval、进入/退出状态和重载；Skill 核对事件类型、全局短路、共享冷却和副作用顺序；Artifact 核对 Builtin 门控、概率类型和粒子 data type。除非效果有特殊要求，不额外判断 `triggerSlot` 或强制执行者为 Player。
6. 提取伤害、倍率、概率、持续时间、冷却、范围、速度、数量和消耗等可调玩法数值，按 LEVELED 优先规则建模；固定数值也使用固定 LEVELED 表达式。
7. 生成最小实现，只包含必要信息、一个明确效果和用户要求的选项。
8. 根据 `references/event-functions.md` 或 `EventFunctions.kt` 核对 Java 回调签名。
9. 纯代码附魔应在 Bukkit `JavaPlugin#onLoad()` 生命周期进入注册队列，早于 Aiyatsbus 在 `JavaPlugin#onEnable()` 阶段加载配置并冻结现代注册表。如果附属插件已经使用 TabooLib，也可以使用对应的 `LifeCycle.LOAD` 生命周期。
10. 没有更高优先级的自定义规范时，按照 `references/enchantment-fields.md` 检查可读性、描述长度、盘古之白例外、`general` 默认无变量、`specific` 变量颜色、LEVELED 自动单位，以及 ORDINARY/MODIFIABLE 手写单位与数值同色规则。

不要把 Fluxon 监听器 ID 当作 Java 方法名，不要编造 Builder 方法，也不要直接操作 Bukkit 或 NMS 注册表。
