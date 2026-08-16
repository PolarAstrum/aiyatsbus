# Aiyatsbus 附魔模型

## 三种附魔开发方式

三种开发方式描述附魔的基础信息和效果分别由 Java、YAML 或 Fluxon 中的哪一部分负责。它们与 Listener、Ticker、Skill、Builtin、Artifact 五类运行机制不是同一层概念。

| 开发方式 | 基础信息 | 效果 | 注册与加载 |
| --- | --- | --- | --- |
| 纯代码附魔 | Java Builder | Java `EventFunctions` 回调 | 附属插件在 LOAD 阶段进入注册队列 |
| YAML + Builtin | YAML | YAML 绑定的 Java `Builtin` 类 | Aiyatsbus 扫描 YAML 并初始化 Builtin |
| YAML + Fluxon | YAML | YAML 内嵌的 Fluxon Listener、Ticker、Skill | Aiyatsbus 扫描 YAML、预热并执行脚本 |

### 纯代码附魔

当 Java 需要同时管理附魔信息和效果时，使用 `BuiltinAiyatsbusEnchantment.builder()`。`build()` 创建 `BuiltinAiyatsbusEnchantmentBase`，`register()` 还会调用附魔管理器完成注册。Java 手动获取管理器时使用 `Aiyatsbus.INSTANCE.api().getEnchantmentManager()`。

`builder()` 通过 `@JvmStatic` 暴露，因此 Java 可以直接调用 `BuiltinAiyatsbusEnchantment.builder()`。获取 Aiyatsbus API 时，Java 必须通过 Kotlin 对象的 `INSTANCE` 字段调用 `Aiyatsbus.INSTANCE.api()`。

Builder 会将传入的信息序列化为内部配置，并自动挂载一个由 `EventFunctions` 实现驱动的 `Builtin` 触发器。

### YAML + Builtin

当附魔基础信息需要由 YAML 管理、效果需要 Java 和 Bukkit API 时，使用 YAML + Builtin。YAML 的 `mechanisms.builtin` 指向继承 `Builtin` 的 Java 类；这个类是触发器，不是附魔对象。

```yaml
mechanisms:
  builtin: "com.example.enchant.CustomBuiltin"
```

Aiyatsbus 创建 YAML 附魔对象，并通过 `(AiyatsbusEnchantment)` 构造器实例化 Builtin。附属插件不应自行实例化 YAML 附魔，也不应重复实现 YAML 加载器。

### YAML + Fluxon

当附魔基础信息和效果定义都希望保存在 YAML 中时，使用 YAML + Fluxon。效果写在 Listener、Ticker 或 Skill 的内嵌脚本块中，不需要 Java Builtin 类。

```yaml
mechanisms:
  listeners:
    on-break:
      listen: block-break
      handle: |-
        # Fluxon 脚本
```

Fluxon 只能嵌入 `handle`、`pre-handle` 或 `post-handle`，不能拆成独立 `.fs` 文件。只需要预设粒子效果时可以在 YAML 中使用 Artifact；Artifact 是纯配置辅助机制，不是第四种开发方式，也不执行 Fluxon。

### YAML 共同加载模型

YAML + Builtin 与 YAML + Fluxon 都由 Aiyatsbus 间接创建 `InternalAiyatsbusEnchantmentBase`。Aiyatsbus 会扫描：

```text
plugins/Aiyatsbus/enchants/<子目录>/**/*.yml
```

加载器会读取 `basic.id`，创建普通附魔或原版附魔实现，检查依赖，注册附魔并初始化 `mechanisms`。文件监听器可以重载单个附魔。Builtin、Listener、Ticker、Skill 和 Artifact 可以按需要共存，但混合机制不改变附魔仍属于 YAML 开发方式。

不要为普通 YAML 附魔编写 `new CustomEnchantment()`。YAML 附魔由 Aiyatsbus 管理器创建，不需要附属插件实例化抽象的 `AiyatsbusEnchantmentBase`。

## 注册生命周期

Aiyatsbus 会在自身 `JavaPlugin#onEnable()` 阶段加载 YAML。定义纯代码附魔的普通 Bukkit 插件应在自己的 `JavaPlugin#onLoad()` 中调用 Builder 的 `register()`。管理器会在 Aiyatsbus 的启用阶段完成外部附魔的实际注册，并在现代注册表冻结前完成这一步。

如果附属插件本身使用 TabooLib，可以使用与 Bukkit 生命周期对应的 `LifeCycle.LOAD` 注册纯代码附魔；Aiyatsbus 的 `LifeCycle.ENABLE` 对外对应 `JavaPlugin#onEnable()`。不要为了使用 Aiyatsbus 而给普通 Bukkit 插件额外引入 TabooLib。

不要把普通 Bukkit `onEnable()` 作为纯代码附魔的默认注册时机。根据插件启用顺序，Aiyatsbus 可能已经冻结现代注册表，而注册实现不会在每次延迟注册时自动解冻注册表。不要直接操作 Bukkit 或 NMS 注册表。

默认管理器会把纯代码附魔视为外部附魔。Aiyatsbus 重载时会保留并重新注册这些附魔，附属插件不需要仅因 YAML 重载而重新创建它们。

## 对象关系

- `AiyatsbusEnchantment` 是所有附魔共用的读取契约。
- `AiyatsbusEnchantmentBase` 是需要配置对象的抽象基础类。
- `InternalAiyatsbusEnchantmentBase` 为 YAML 附魔提供 `Mechanism`。
- `VanillaAiyatsbusEnchantmentBase` 用于包装原版附魔，不使用普通 `Mechanism`。
- `BuiltinAiyatsbusEnchantmentBase` 由纯代码 Builder 创建，并持有包含 `Builtin` 触发器的 `Mechanism`。

## 权威源码

不确定行为时，检查 Aiyatsbus 源码中的以下文件：

- `core/BuiltinAiyatsbusEnchantment.kt`
- `core/AiyatsbusEnchantmentBase.kt`
- `core/InternalAiyatsbusEnchantment.kt`
- `core/VanillaAiyatsbusEnchantment.kt`
- `core/data/trigger/Mechanism.kt`
- `core/data/trigger/builtin/Builtin.kt`
- `core/data/trigger/builtin/EventFunctions.kt`
- `core/data/trigger/artifact/Artifact.kt`
- `core/data/AlternativeData.kt`
- `core/data/Dependencies.kt`
- `core/data/Displayer.kt`
- `core/data/Variables.kt`
- `common-impl/.../DefaultAiyatsbusEnchantmentManager.kt`

项目明确拒绝 Minecraft 1.20.5 和 1.20.6。不要在未检查当前构建和注册模块时承诺版本支持。
