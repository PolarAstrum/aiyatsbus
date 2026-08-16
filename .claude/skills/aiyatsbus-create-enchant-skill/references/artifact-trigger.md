# Artifact 触发器

Artifact 用于快速创建粒子附魔，不需要编写事件脚本或 Java `Builtin`。需要条件判断、状态修改或其他游戏逻辑时，应改用对应的 Listener、Ticker、Skill 或 Builtin。

Artifact 虽然不需要编写 Java 类，但底层继承 `Builtin`，并与 Builtin 共用事件分发和自动门控。理解其触发条件时必须阅读下方“Builtin 共用门控”，但配置 Artifact 时不需要创建 `mechanisms.builtin`。

## 基础粒子

```yaml
mechanisms:
  artifact:
    particle: ANGER_VILLAGER
    amount: 1
    specialized: false
```

## 方块数据粒子

```yaml
mechanisms:
  artifact:
    particle: BLOCK_MARKER
    amount: 1
    options:
      type: LIGHT
```

`options.type` 会解析为方块材质并创建 `BlockData`，适用于要求方块数据的粒子。

## 灰尘粒子

```yaml
mechanisms:
  artifact:
    particle: DUST
    amount: 1
    options:
      red: 3
      green: 252
      blue: 140
      size: 1.0
```

同时存在 `red`、`green`、`blue` 和 `size` 时，会创建 Bukkit `Particle.DustOptions`。

## 字段

- `particle`：XParticle 名称。无法解析时不会产生粒子。
- `amount`：粒子数量。源码未提供非零默认值，应显式填写正整数。
- `specialized`：是否使用按装备槽位配置的专精粒子形态，默认 `false`。
- `options.type`：方块数据粒子的材质。
- `options.red`、`green`、`blue`、`size`：灰尘粒子的颜色和大小。

源码不会统一验证这些参数。生成配置时必须主动保证：

- `amount` 是大于 `0` 的整数。
- `red`、`green` 和 `blue` 位于 `0..255`。
- `size` 是 Bukkit `Particle.DustOptions` 接受的正数。
- `options` 产生的数据类型与粒子的 Bukkit data type 一致。

`options.type` 无法解析时会得到 `null`。对于不要求额外数据的粒子，省略 `options`；对于要求 `BlockData`、`DustOptions` 或其他特定数据的粒子，缺失或类型不匹配可能在生成粒子时抛出运行时错误。当前 Artifact 只会自动构造 `BlockData` 和 `DustOptions`，不能用该配置表达所有 Bukkit 粒子数据类型。

## Builtin 共用门控

Artifact 与 Builtin 一起由 Builtin 执行器分发。每次触发前会：

1. 从事件或 20-tick 任务提供的槽位解析物品。
2. 确认物品拥有当前附魔。
3. 找到该附魔的 Builtin 和 Artifact，并读取、计算和强制转换概率变量。
4. 确认 `basic.enable` 为 `true`。
5. 执行自动概率判断。
6. 执行 `CheckType.USE` 和 active slot 限制检查。
7. 条件通过后依次调用 Builtin 和 Artifact。

自动概率只适用于 Builtin 和 Artifact。概率按以下顺序读取：

1. 名为 `chance` 的 LEVELED 变量。
2. 名为 `概率` 的 LEVELED 变量。
3. 都不存在时使用 `100.0`。

```yaml
variables:
  leveled:
    chance: "%:25.5+5*{level}"
```

配置这个变量会让每个槽位的每次 Builtin 分发进行一次概率判断。Listener、Ticker 和 Skill 不会自动使用它。多槽位事件或周期任务可能为不同槽位分别进行门控和粒子调用。

当前 Builtin 执行器把概率计算结果强制转换为 `Double`，而 LEVELED 变量系统会把数学上的整数结果返回为 `Int`。因此概率表达式必须保证每个有效等级都计算出非整数 Double，例如上例始终保留 `.5`；`"%:25+5*{level}"` 在整数等级上可能得到 `Int` 并触发类型转换异常。这是当前实现限制。

概率函数有两段不同语义：`0.0..1.0` 按比例处理，因此 `0.5` 是 50%、`1.0` 是 100%；大于 `1.0` 才按百分数处理，因此 `25.5` 是 25.5%。`1.0` 与略大于 `1.0` 的含义会突变，不要把 `0.5` 当作 0.5%。在当前强制 Double 限制下，建议使用明确大于 `1.0` 且保持小数的百分数，例如 `25.5`。

同一附魔同时配置 Builtin 和 Artifact 时，两者会合并后共用同一次概率和限制门控；它们在同一槽位、同一次分发中共同通过或共同失败，不会各自独立随机。概率变量的读取和强制转换发生在 `basic.enable` 判断之前，因此即使附魔已禁用，错误的整数概率结果仍可能先触发类型转换异常。

## 触发行为

- 普通和专精 Artifact 都会在破坏匹配方块时产生简单粒子。方块范围由全局 `enchants/artifact.yml` 的 `blocks` 决定，`*` 表示全部方块。
- 普通和专精 Artifact 都会在有效攻击生物时产生双螺旋粒子；玩家攻击冷却低于 `0.9` 时不触发。
- `specialized: false` 时，玩家使用胸甲槽物品滑翔会产生简单粒子，脚部槽位会产生环形粒子；这两种行为同样由每 20 tick 一次的 Builtin 任务检查，不是每个游戏 tick 生成。
- `specialized: true` 时，每 20 tick 按匹配装备槽位播放专精粒子。
- 同一玩家有多个匹配槽位时，一轮可能产生多次粒子。

玩家方块破坏是多槽位规则的例外。Builtin 对 `BLOCK_BREAK` 的非锄物品使用玩家 UUID 共享的 150ms baffle，且 baffle 在逐槽位检查中提前消耗；一次事件通常只有第一个非空、非锄槽位能继续，后续槽位会被拦截，即使第一个槽位没有目标附魔。锄会绕过这个 baffle。该限制也会影响 150ms 内连续方块破坏的 Builtin/Artifact 分发。

专精粒子的 `CIRCLE`、`RNA`、`SIMPLE` 形态、半径和高度来自全局 `enchants/artifact.yml`，不是单个附魔文件中的字段。

## 排错

- 完全没有粒子时，确认 `particle` 能被 XParticle 解析、`amount > 0`、附魔已启用，并且物品槽位通过 `CheckType.USE`。
- 偶尔不产生粒子时，检查附魔是否定义了 `chance` 或 `概率` LEVELED 变量。
- 粒子数据类型异常时，核对 Bukkit 粒子的 data type 与 `options` 构造出的 `BlockData` 或 `DustOptions`。
- 周期粒子频率约为每 20 tick 一次，服务器卡顿时现实时间间隔会变长。
- 一轮出现多次粒子通常表示多个匹配槽位分别通过了 Builtin 门控。
- 方块破坏只检查到前面的非锄槽位时，检查 150ms baffle 和槽位遍历顺序；不要按周期任务的多槽位行为推断方块破坏。
- `specialized: true` 但某槽位没有粒子时，检查全局 `enchants/artifact.yml` 是否为该槽位配置了形态和参数。
