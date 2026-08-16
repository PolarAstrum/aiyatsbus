# 附魔字段速查

本文件同时适用于纯代码附魔和 YAML 附魔。只配置实现需求所必需的字段。

## 基础信息与展示

### 规范优先级

如果开发者在当前请求、项目规范、已有代码或已有配置中提供了自定义的描述格式规范，应优先遵循开发者规范。本节规则是 Aiyatsbus 附魔描述的默认规范，仅在没有更高优先级的自定义规范时生效。

### 描述格式规范

技能文档的说明文字需要遵循盘古之白，但附魔的 `general` 和 `specific` 描述字符串是明确的例外，两者都不遵循盘古之白。生成描述时，不要在中文与数字、变量占位符、颜色符号或相邻文本之间为了排版而插入空格。描述应保持游戏内自然、紧凑的连续文本。

例如，正确的 `specific` 描述是：

```yaml
specific: "&7剑的伤害在&a3秒&7内提升&a{伤害}&7并且造成减速"
```

不要写成：

```yaml
specific: "&7剑的伤害在 &a3 秒 &7内提升 &a{伤害} &7并且造成减速"
```

在此基础上，`general` 和 `specific` 的颜色规则不同：

- 可读性永远优先于描述长度。描述应保持简短，但不能为了缩短文本而省略理解效果所需的信息。
- 优先用一句话清楚概括触发条件和核心效果，并保留必要的作用对象、持续时间、范围、概率或关键数值。
- 如果压缩后会产生歧义、指代不明或无法判断效果如何触发，应使用更完整的句子。
- 不要在描述中解释代码实现、计算过程、内部机制或使用方法。
- 不要重复附魔名称、等级信息或已经由变量表达的内容。
- 一个简短句子能够说明效果时，不要使用多行描述；确有多个独立效果时才使用 `\n` 分行。
- `general` 默认不包含任何颜色符号，也不包含变量占位符。它主要用于附魔搜索和普通语义概括；除非开发者明确要求，否则不要在 `general` 中放变量。具体数值和等级变化放在 `specific`。
- `specific` 默认以 `&7` 作为颜色开头，用于物品或附魔信息中的实际展示。
- 描述中的变量默认使用 `&a` 颜色。固定描述文本使用 `&7`，变量后需要恢复普通描述颜色时再次使用 `&7`。
- 如果使用 `LEVELED` 类型变量，并且变量明确提供了单位，单位会在变量展示值中自动追加并跟随变量颜色。描述中不要再次手写这个单位。没有明确提供单位时，变量占位符后应按实际语义补充单位，单位与变量保持同色。
- `ORDINARY` 和 `MODIFIABLE` 不能配置展示单位。如果它们表示时间、距离、数量、概率、伤害等有单位数值，`specific` 中应尽量手写单位，且单位必须与变量数值保持同色；单位之后再用 `&7` 恢复固定文本。状态、名称、布尔值等确实不需要单位的变量不要强行添加。

正确示例：

```yaml
display:
  description:
    general: "每击杀若干个生物后，下一次攻击伤害增加"
    specific: "&7每击杀&a{击杀数量}&7个生物后，下一次攻击伤害增加&a{伤害增加百分比}&7"

variables:
  leveled:
    伤害增加百分比: "%:15.0*{level}"
```

这里的 `{伤害增加百分比}` 已经会显示 `%`，不要写成 `{伤害增加百分比}%`。同理，如果变量配置为 `点:2.5*{level}`，正确写法是 `&a{damage}&7伤害`，不要写成 `&a{damage}点&7伤害`。

`ORDINARY` 或 `MODIFIABLE` 单位示例：

```yaml
display:
  description:
    general: "效果持续一段时间后停止"
    specific: "&7持续&a{test}秒&7才停止"

variables:
  ordinary:
    test: 10
```

正确片段是 `&a{test}秒&7`。以下写法错误：

```yaml
specific: "&7持续&a{test}&7秒才停止" # 单位没有与数值同色
specific: "&7持续&a{test}&7才停止"   # 有单位数值却遗漏单位
```

### 如何判断是否指定单位

只有明确提供非空单位时，才算指定了单位：

- 纯代码方案中，`.addVariable(VariableType.LEVELED, "test", "{level}*10")` 没有指定单位。
- 纯代码方案中，`.addVariable(VariableType.LEVELED, "test", "{level}*10", "")` 也没有指定单位。
- 纯代码方案中，`.addVariable(VariableType.LEVELED, "test", "{level}*10", "个")` 指定了单位 `个`。
- YAML 配置中，`damage: "点:2.5*{level}"` 指定了单位 `点`。
- YAML 配置中，`damage: "xxxxx:2.5*{level}"` 指定了单位 `xxxxx`。单位内容不需要是预设枚举，冒号前的非空文本都视为单位。
- YAML 配置中，`damage: ":2.5*{level}"` 没有指定单位，因为冒号前为空。

因此，只有 LEVELED 的单位非空时才禁止在描述中重复书写单位。不要仅因为变量值中存在冒号，就把空单位判断为已指定单位。没有配置单位的 LEVELED，以及无法配置单位的 ORDINARY/MODIFIABLE，如果语义需要单位，都应在 `specific` 中紧跟占位符补充，并保持变量颜色。

错误示例：

```yaml
display:
  description:
    general: "&7带有颜色的搜索描述"
    specific: "等级 {level} 的描述"
```

上例同时违反了多项默认规范：`general` 包含颜色，`specific` 没有以 `&7` 开头、变量没有使用 `&a`，并且描述中人为插入了空格。

```yaml
basic:
  enable: true
  disable-worlds: [world_the_end]
  id: custom_enchant
  name: "自定义附魔"
  max-level: 3

rarity: 史诗
targets: [剑, 斧]

display:
  display: true
  format:
    previous: "{default_previous}"
    subsequent: "{default_subsequent}"
  description:
    general: "用于搜索的普通描述"
    specific: "&7等级&a{level}&7的描述"
```

`previous` 和 `subsequent` 支持 Aiyatsbus 默认占位符与换行转义。省略 `specific` 时会使用 `general`；如果 `general` 没有颜色符号，回退后的实际展示可能需要由调用方或更高优先级规范处理。展示文本可以使用变量和占位符，等级变量在展示时会包含单位。

常用展示占位符包括：

- `{description}`：当前等级的具体描述。
- `{level}`：数字等级。
- `{roman_level}`：罗马数字等级。
- `{enchant_display}`：默认附魔名称展示。
- `{enchant_display_roman}`：罗马数字等级展示。
- `{enchant_display_number}`：阿拉伯数字等级展示。
- `{enchant_display_tag}`：等级标签展示。

## 附魔对象

附魔对象通过 Aiyatsbus 对象注册表解析。常用值包括 `剑`、`斧`、`镐`、`弓`、`弩`、`三叉戟`、`头盔`、`胸甲`、`护腿`、`靴子`、`盾牌`、`鞘翅`、`damageable` 和 `all`。实际名称可由对象注册表配置；名称无法解析时，检查 `enchants/target.yml`。

## 限制

```yaml
limitations:
  - "CONFLICT_ENCHANT:锋利"
  - "CONFLICT_GROUP:原版增伤类附魔"
  - "DEPENDENCE_ENCHANT:无限"
  - "DEPENDENCE_GROUP:原版增伤类附魔"
  - "PERMISSION:example.enchant.use"
```

每项格式为 `TYPE:value`。实际适合在列表中配置值的类型是 `PERMISSION`、`CONFLICT_ENCHANT`、`CONFLICT_GROUP`、`DEPENDENCE_ENCHANT` 和 `DEPENDENCE_GROUP`。

源码会自动添加并执行 `TARGET`、`MAX_CAPABILITY`、`DISABLE_WORLD` 和 `SLOT`，但不会使用限制列表中为这些类型填写的值。禁用世界来自全局设置和 `basic.disable-worlds`，槽位来自对象配置的 `active-slots`。`PAPI_EXPRESSION` 虽然存在于枚举中，但当前检查函数直接返回 `true`，不能用于实际权限或条件控制。

## 变量

### 玩法数值必须变量化

伤害、伤害倍率、概率、持续时间、冷却、范围、半径、速度、数量、资源消耗等可供服主或附魔设计者调整的玩法数值，能做成附魔变量时必须尽量变量化。不要在 Fluxon 脚本、Java Builtin 或纯代码回调中直接写死这些数值。

数值变量默认优先使用 `LEVELED`。`LEVELED` 表示使用等级变量系统、支持统一注入和展示单位，不代表表达式必须包含 `{level}`。没有指定随等级变化时，仍使用固定 LEVELED 表达式：

```yaml
variables:
  leveled:
    伤害提升: "%:30"
    持续时间: "秒:10"
    作用半径: "格:5"
```

需要随等级变化时再使用 `{level}`：

```yaml
variables:
  leveled:
    伤害提升: "%:20+10*{level}"
    持续时间: "秒:5+2*{level}"
```

变量类型按数据语义选择：

| 数据需求 | 变量类型 |
| --- | --- |
| 可调玩法数值，无论是否随等级变化 | `LEVELED` |
| 需要写入物品 NBT/PDC，并在运行时修改的累计值、充能值或状态 | `MODIFIABLE` |
| 布尔开关、字符串、枚举名、列表和其他非数值固定配置 | `ORDINARY` |

即使累计值是数字，只要它需要随物品持久化并在运行时改变，也应使用 MODIFIABLE，而不是 LEVELED。布尔值、状态名称等不应为了满足“优先 LEVELED”而错误建模成数值变量。

这条规则针对可调玩法参数，不要求把每一个数字都变量化。坐标分量中的 `0.0`、集合索引、算法边界、Bukkit API 协议常量，以及 Ticker `interval`、Artifact `amount` 等机制 schema 自身要求的字段可以保留为结构性字面量。

效果实现和 `specific` 必须读取同一变量，不要在脚本中写 `1.3`、在描述中再写 `30%`。例如：

```yaml
display:
  description:
    general: "攻击时提高造成的伤害"
    specific: "&7攻击时造成的伤害提高&a{伤害提升}&7"

variables:
  leveled:
    伤害提升: "%:30"
```

`{伤害提升}` 已包含 `%` 单位，描述中不重复书写。

```yaml
variables:
  leveled:
    damage: "点:2.5*{level}"
    chance:
      1: 10
      2: 25
      3: 40
      unit: "%"
  modifiable:
    kills: enchant_kills=0
    state: (NBT)custom.state=ready
  ordinary:
    damage-type: magic
    enabled: true
```

`leveled` 公式使用 `level`，也可以为不同等级显式配置不同数值。显式等级表会选择“不高于当前附魔等级的最高配置等级”，不会进行插值。例如只配置了 `1`、`3`、`5` 级时，附魔等级 `4` 使用 `3` 级的值；至少配置 `1` 级，否则低于最小配置等级时可能无法取值。双层大括号只用于等级变量公式，可以引用另一个等级变量，但禁止循环引用；`ordinary` 和 `modifiable` 不会参与这种公式递归。`modifiable` 默认使用 PDC；存储键以 `(NBT)` 开头时使用 NBT；等号用于分隔存储键和默认值。`ordinary` 会保留 YAML 原始值类型，可以保存字符串、数字、布尔值或列表；Java 读取时应按实际类型处理。

## 粒子机制

只需要粒子效果时，可以使用 Artifact：

```yaml
mechanisms:
  artifact:
    particle: ANGER_VILLAGER
    amount: 1
    specialized: false
```

方块数据、灰尘颜色、专精形态和实际触发范围见 `artifact-trigger.md`。效果需要条件判断或修改状态时，改用 Listener、Ticker、Skill 或 Java Builtin。
