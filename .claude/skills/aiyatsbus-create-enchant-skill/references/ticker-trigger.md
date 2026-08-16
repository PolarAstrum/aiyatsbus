# Ticker 触发器

Ticker 按固定 tick 间隔检查玩家装备并执行脚本，适合持续效果、周期消耗和进入或离开生效状态时的处理。

Ticker 只使用 Fluxon。`pre-handle`、`handle` 和 `post-handle` 必须遵循 `fluxon-language.md`；变量读取必须使用 `&name`，扩展函数优先使用 `::`。这 3 个脚本块必须直接嵌入 YAML，不能拆分为独立 `.fs` 文件。通常省略 `type`，不要生成冗余的 `type: FLUXON`。

```fluxon
if &player::isFlying() {
    &item::setDurability(&item::durability() + &消耗耐久)
}
```

## 配置示例

以下结构来自项目内飞行附魔的 Ticker 用法，脚本内容已缩减：

```yaml
mechanisms:
  priority-ticker: 0
  tickers:
    durability:
      interval: 40
      pre-handle: |-
        # 首次进入生效状态时执行
      handle: |-
        # 每个有效槽位按间隔执行
      post-handle: |-
        # 所有槽位均不再生效时执行
```

## 字段

- `durability`：Ticker ID，可自定义；同一附魔内应保持唯一。
- `type`：历史兼容字段。当前唯一脚本系统是 Fluxon，通常省略，不要主动生成 `type: FLUXON`。
- `interval`：执行间隔，单位为 tick，默认 `20`。`20` 约为 1 秒，`40` 约为 2 秒。
- `pre-handle`：玩家从未生效状态进入生效状态时执行一次。
- `handle`：每到间隔时，为每个有效装备槽位执行。
- `post-handle`：玩家不再有任何有效装备槽位时执行一次。
- `priority-ticker`：不同附魔之间的 Ticker 排序，默认 `0`。

`interval` 必须是正整数，不能配置为 `0`。加载器不会校验这个前置条件，调度器会直接计算 `counter % interval`；`0` 会导致调度异常，负数虽可被读取但不属于受支持配置。生成 YAML 时必须主动保证 `interval >= 1`。服务器卡顿会影响 tick 对应的现实时间，不要把 `interval` 描述成绝对准确的秒数。

## 执行过程

Ticker 会遍历附魔对象配置中的 `active-slots`。每个槽位中的物品必须拥有附魔，并通过 `CheckType.USE` 限制检查。

- 同一玩家有多个有效槽位时，`handle` 在一轮中可能执行多次。
- `pre-handle` 以玩家和 Ticker ID 记录状态，首次出现任意有效槽位时执行一次，不是每个槽位执行一次。
- 所有有效槽位消失后，`post-handle` 执行一次。
- 附魔卸载时，Ticker 会从 Aiyatsbus 调度表中移除。

当前状态记录只保存 Ticker ID，不包含附魔 ID。不同附魔不要复用过于通用的 Ticker ID；推荐加入附魔标识前缀，例如使用 `wings-durability`，而不是所有附魔都使用 `task`。

状态碰撞会同时影响进入和离开：一个附魔记录了共享 ID 后，另一个附魔可能跳过 `pre-handle`；其中一个附魔移除共享 ID 后，另一个仍有效的附魔又可能在后续轮次重新执行 `pre-handle`。触发器内部脚本 ID 还会把 `-` 转成 `_`，因此不要同时使用归一化后相同的 ID，例如 `wings-task` 与 `wings_task`。

## 重载与关闭限制

Ticker 的调度表与玩家活跃状态 recorder 是两套状态。当前 `Ticker.close()` 和处理器 `reset()` 会移除或清空调度表，但不会清空 recorder，也不会主动执行 `post-handle`。

这会产生以下行为：

- 相同 Ticker ID 重载后，玩家若始终装备有效物品，新的 `pre-handle` 可能不会重新执行。
- 关闭、禁用或重载附魔时，不保证为仍处于有效状态的玩家执行 `post-handle`。
- 重载后 recorder 中的旧 ID 只有在对应新 Ticker 再次调度且检测到无有效槽位时才可能移除。
- 仅依赖 `post-handle` 清理飞行、属性、元数据或药水效果是不安全的。

`pre-handle` 和 `post-handle` 应设计为幂等：重复执行不会叠加错误，缺失一次也不会让玩家永久保留危险状态。需要严格重载清理时，应由 Java 生命周期代码或独立管理逻辑完成，不要假定 Ticker 关闭回调等价于退出状态。

## 执行事件取消语义

Ticker 的 recorder 状态变更发生在各阶段脚本执行事件之外：

- 首次进入时，处理器先把 Ticker ID 加入 recorder，再调用 `pre-handle`，随后在同一轮继续调用 `handle`。
- 即使 `pre-handle` 对应的 `AiyatsbusEnchantmentExecuteEvent` 被取消，玩家仍被记录为活跃，本轮 `handle` 仍会尝试执行，后续轮次也不会自动重试 `pre-handle`。
- 离开时，处理器先从 recorder 移除 Ticker ID，再调用 `post-handle`。
- 即使 `post-handle` 对应的执行事件被取消，状态也已经移除，后续不会自动重试该 `post-handle`。

三个阶段不是事务性的进入、周期、退出回调。取消某个脚本块只阻止该脚本块本身，不会回滚 recorder，也不会联动取消同一轮的其他阶段。

## 脚本变量

`pre-handle` 和 `handle` 可使用：

```text
player
enchant
maxLevel
item
level
triggerSlot: String
trigger-slot: String
container
globalContainer
附魔 variables 中的变量
```

`triggerSlot` 和 `trigger-slot` 是槽位名称字符串，不是 `EquipmentSlot` 对象，常见值为 `"HAND"`、`"OFF_HAND"`、`"HEAD"`、`"CHEST"`、`"LEGS"` 和 `"FEET"`。

active slots 和 `Limitations` 已完成常规槽位过滤，没有特殊需求时不要再判断 `triggerSlot`。只有同一 Ticker 对不同合法槽位执行不同逻辑时，才使用类似 `if &triggerSlot == "FEET" { ... }` 的分支。

`post-handle` 执行时已经没有有效槽位，只保证提供 `player`、`enchant`、`maxLevel` 和 ordinary 变量。不要在 `post-handle` 中假定存在当前 `item`、`level`、LEVELED/MODIFIABLE 变量或 `triggerSlot`。

`container` 和 `globalContainer` 在 `pre-handle`、`handle`、`post-handle` 中都可用。它们适合传递运行时状态，但不能替代 `MODIFIABLE` 或数据库持久化。单一用途时直接使用整个 `container`；多个用途时才按用途嵌套 Map。重载和关闭不保证 `post-handle` 执行，清理逻辑必须允许缺失或重复，详见 `fluxon-containers.md`。

不要假定 `pre-handle`、`handle` 和 `post-handle` 的普通局部变量会自动共享。需要跨阶段保存状态时，使用明确的宿主上下文、物品变量或其他持久化机制。需要 Fluxon API 时按需读取 `fluxon-stdlib.md`、`fluxon-jvm-interop.md` 和 `fluxon-modules.md`。

## 排错

- Ticker 没有执行时，先确认 `interval >= 1`、Ticker 已进入调度表、目标 active slots 非空且物品通过 `CheckType.USE`。
- `handle` 一轮执行多次通常表示多个 active slots 同时有效，不是调度器重复注册。
- `pre-handle` 在重载后没有执行时，检查 recorder 残留和 Ticker ID 是否与旧配置相同。
- `post-handle` 只有已记录为活跃后又检测到所有槽位失效才会执行；从未进入活跃状态、直接关闭机制或重载都不保证执行。
- 空脚本块合法但没有效果；预热失败后还要检查首次调度时的编译日志。
