# YAML 附魔

YAML 附魔对应三种开发方式中的后两种：YAML + Builtin 和 YAML + Fluxon。两者都由 YAML 管理附魔基础信息，并由 Aiyatsbus 负责发现文件、创建对象、注册附魔、初始化机制和监听文件变化。

## 选择 YAML 实现方式

| 方式 | 效果实现 | 适用场景 | 需要阅读 |
| --- | --- | --- | --- |
| YAML + Builtin | Java `Builtin` 类 | 需要复杂 Java/Bukkit 逻辑，同时允许服主修改 YAML 参数 | `builtin-trigger.md` |
| YAML + Fluxon | Listener、Ticker、Skill 的 Fluxon 脚本 | 希望附魔信息和效果都由 YAML 管理，不增加 Java 触发器类 | `mechanisms.md` 和对应脚本触发器文档 |

两种方式可以在同一附魔中混合运行机制，但通常应选择一个主要效果实现方式，避免同一效果同时由 Java 和 Fluxon 重复执行。Artifact 是 YAML 下的纯配置粒子辅助机制，可以与两种方式共存；它不执行 Fluxon，也不是第四种开发方式。

## 最小 YAML + Builtin

```yaml
basic:
  enable: true
  id: custom_enchant
  name: "自定义附魔"
  max-level: 3

rarity: 普通
targets:
  - 镐

limitations: [ ]

display:
  description:
    general: "挖掘方块时触发"
    specific: "&7挖掘方块时触发"

mechanisms:
  builtin: "com.example.enchant.CustomBuiltin"
```

`display.description` 必须遵循 `enchantment-fields.md` 中的默认描述格式规范；如果开发者提供了自定义格式规范，则以开发者规范为准。

## 最小 YAML + Fluxon

```yaml
basic:
  enable: true
  id: custom_script_enchant
  name: "脚本附魔"
  max-level: 3

rarity: 普通
targets:
  - 镐

limitations: [ ]

display:
  description:
    general: "挖掘方块时触发"
    specific: "&7挖掘方块时触发"

mechanisms:
  listeners:
    on-break:
      listen: block-break
      handle: |-
        player = &event::player()
```

示例只展示内嵌脚本位置。实际 Bukkit getter、setter、枚举和变量用法必须遵循 `fluxon-language.md` 与 `fluxon-bukkit-java-semantics.md`，并根据 Listener 的实际事件变量编写效果。

把文件放在 Aiyatsbus 数据目录的子目录中：

```text
plugins/Aiyatsbus/enchants/MyAddon/custom_enchant.yml
```

文件名不必与 `basic.id` 相同，但 ID 必须唯一，并且必须能够作为 `NamespacedKey.minecraft(id)` 的键。不要沿用旧文档中“只能使用小写字母和下划线”的过窄限制；实际格式以 Bukkit `NamespacedKey` 的键规则为准。

## 根节点

- `basic`：包含 `enable`、`disable-worlds`、`id`、`name` 和 `max-level`。
- `rarity`：已配置的品质名称或 ID，位于根节点。
- `targets`：已注册的附魔对象名称，位于根节点。
- `alternative`：可选的获取方式和原版标记。
- `limitations`：由 `TYPE:value` 组成的列表。
- `dependencies`：支持版本、数据包和依赖插件。
- `display`：展示格式和描述。
- `variables`：包含 `leveled`、`modifiable` 和 `ordinary`。
- `mechanisms`：可包含 `listeners`、`tickers`、`skills`、`artifact` 或 `builtin`。

机制选择和公共字段见 `mechanisms.md`：

- Bukkit 事件脚本：`listener-trigger.md`
- 周期脚本：`ticker-trigger.md`
- 主动技能：`skill-trigger.md`
- Java 触发器：`builtin-trigger.md`
- 粒子附魔：`artifact-trigger.md`

玩法数值应优先配置为 LEVELED。即使数值不随等级变化，也使用固定表达式，例如：

```yaml
variables:
  leveled:
    伤害提升: "%:30"
    持续时间: "秒:10"
```

不要因为数值固定就放入 ordinary，更不要直接写死在 Fluxon 或 Java 效果中。只有需要物品 NBT/PDC 持久化并在运行时修改的数据才使用 modifiable；布尔、字符串、枚举名、列表等非数值配置使用 ordinary。完整选择规则见 `enchantment-fields.md`。

当前唯一脚本系统是 Fluxon，Listener、Ticker 和 Skill 通常省略 `type`，不要主动生成 `type: FLUXON`。生成脚本前必须阅读 `fluxon-language.md`。调用 Bukkit 或其他 Java API 时还必须阅读 `fluxon-bukkit-java-semantics.md`。需要内置函数、扩展函数、JVM 互操作或模块时，再按需阅读 `fluxon-stdlib.md`、`fluxon-jvm-interop.md` 和 `fluxon-modules.md`。Fluxon 只能直接嵌入机制的 YAML 脚本块，不能保存为独立 `.fs` 文件。

项目默认附魔 YAML 中存在部分历史 Fluxon 写法。它们只用于参考附魔结构和业务逻辑；新脚本必须以 `fluxon-language.md` 和 `fluxon-bukkit-java-semantics.md` 为准，不要从默认资源复制与严格语义冲突的裸枚举或 getter 写法。

`AiyatsbusEnchantmentBase` 要求配置中存在 `basic` 和 `display`。其他节点具有默认行为或可以为空。

`basic.enable: false` 会阻止 Builtin 效果执行，并使常规附魔可用性检查失败。`basic.max-level` 的源码默认值是 `1`，但创建附魔时应显式填写，避免生成代码依赖隐藏默认值。未知品质会回退到 Aiyatsbus 的默认品质；默认品质也无法解析时才会报错。未知附魔对象会被丢弃，全部对象均无法解析时，最终 `targets` 会为空。

## 可选数据

```yaml
alternative:
  weight: 100
  grindstoneable: true
  is-cursed: false
  is-treasure: false
  is-tradeable: true
  is-discoverable: true
  is-vanilla: false
  trade-max-level: -1
  enchant-max-level: -1
  loot-max-level: -1
  inaccessible: false
```

不需要改变默认值时，省略整个节点。`is-vanilla` 会选择 `VanillaAiyatsbusEnchantmentBase`，它不是普通自定义附魔的启用开关。原版附魔也不会使用普通机制触发器。

这些字段分别控制不同功能，不能互相推导：

- `is-cursed` 只控制 Bukkit 附魔的诅咒属性。
- `grindstoneable` 单独控制砂轮是否移除附魔。`is-cursed: true` 不代表不能被砂轮移除。
- `rarity: 诅咒` 只是选择一个品质，也不等于 `is-cursed: true`。
- `is-treasure: true` 会排除附魔台获取。
- `is-discoverable: false` 会排除战利品获取。
- `is-tradeable: false` 会排除村民交易获取。

`is-tradeable: true` 只表示该附魔允许参与交易，附魔仍必须属于交易模块配置的附魔组，并通过交易限制。`inaccessible` 的最终结果还会受到当前附魔的品质和所属附魔组的 `inaccessible` 设置影响；只要其中任一层不可获取，附魔最终就不可通过普通获取渠道获得。

三个获取渠道的等级上限遵循“自身配置优先、全局配置回退”的规则：

1. 使用 `trade-max-level`、`enchant-max-level` 或 `loot-max-level` 中对应的非 `-1` 值。
2. 如果附魔自身为 `-1`，使用对应功能模块的全局上限。
3. 如果全局上限也是 `-1`，使用该附魔的 `basic.max-level`。
4. 最终值不会超过该附魔的 `basic.max-level`。

例如附魔自身的 `enchant-max-level` 为 `4`、附魔台全局上限为 `3`、附魔最大等级为 `5` 时，结果是 `4`。全局值不是与自身值共同取最小值。

## 限制与依赖

```yaml
limitations:
  - "CONFLICT_ENCHANT:锋利"
  - "CONFLICT_GROUP:原版增伤类附魔"
  - "PERMISSION:example.enchant.use"

dependencies:
  supports: 11605-12004
  datapacks: [ ]
  plugins:
    - SomePlugin
```

系统会自动执行对象、最大词条数、禁用世界和槽位检查。不要从旧文档复制 `TARGET:value`、`MAX_CAPABILITY:value`、`DISABLE_WORLD:value` 或 `SLOT:value`；当前源码不会使用这些行中的值。单个附魔的禁用世界应配置在 `basic.disable-worlds`，生效槽位由 `enchants/target.yml` 中对应对象的 `active-slots` 决定。

版本使用紧凑形式，例如 Minecraft 1.20.4 对应 `12004`；连字符表示闭区间。Aiyatsbus 不支持 Minecraft 1.20.5 和 1.20.6，因此不要编写跨过这两个版本的连续支持区间。

`supports: 11802` 表示从 Minecraft 1.18.2 到当前更高版本；`supports: 11802-12004` 才表示包含两端的闭区间。`datapacks` 中的每一项都必须匹配一个已启用的数据包名称。`plugins` 当前检查插件管理器能否找到对应名称，不额外检查 `isEnabled()`。任一依赖不满足时，Aiyatsbus 会跳过该附魔，不注册它，也不会初始化它的机制。

## 加载行为

Aiyatsbus 会在启用阶段递归读取 YAML，检查依赖，注册可用附魔并调用 `mechanism.init()`。加载器通过 `(AiyatsbusEnchantment)` 构造器反射创建 `mechanisms.builtin` 指定的类。其他机制会按各自节点初始化，具体写法见对应触发器文档。

机制配置没有统一 schema 预校验。以下错误会在机制构造或初始化时失败：Listener 缺少 `listen`、Listener/Ticker/Skill 使用非法 `type`、Skill 使用非法 `action`、Builtin 类无法加载或缺少 `(AiyatsbusEnchantment)` 构造器。机制按 Listener、Ticker、Skill、Builtin、Artifact 的顺序在同一个保护块中初始化；任一机制失败后，本次初始化立即中断，后面的机制一定不会初始化。

在服务器启动、尚未进入 `LifeCycle.ACTIVE` 时，机制保护器遇到异常会打印严重错误，等待约 5 秒并通过 `Runtime.halt(-1)` 强制终止进程。进入 ACTIVE 后的热重载不会走强制停服分支，但仍会留下只初始化了前半部分机制的附魔。机制配置必须在投入启动流程前完成严格校验。

空脚本块或缺省 `handle` 会被当作空字符串，不会产生效果，但不等同于配置构造失败。脚本预热失败通常只记录警告，触发器仍可能存在并在首次触发时再次编译。排错时不要只确认附魔对象已注册，还要检查机制数量、预热日志和首次触发日志。

只提供 YAML 的附属插件不需要实现附魔加载器，也不需要调用 `loadFromFile()`。附属插件只需确保文件位于 Aiyatsbus 数据目录，并确保触发器类存在于运行时类路径中。文件必须在 Aiyatsbus 的 `ENABLE` 扫描前存在；运行中首次释放文件后，需要通过 Aiyatsbus 自身的重载流程加载它。

单文件重载会关闭旧机制并初始化新机制，但 Ticker 的玩家活跃状态 recorder 当前不会随关闭或全局 reset 一并清理。相同 Ticker ID 重载后，持续装备有效物品的玩家可能不会重新执行 `pre-handle`，关闭过程也不保证补发 `post-handle`。依赖进入或退出状态的脚本必须阅读 `ticker-trigger.md` 的重载限制。
