# Listener 触发器

Listener 通过 Aiyatsbus 事件映射监听 Bukkit 事件，并执行配置中的脚本。Java 效果应使用 `Builtin`，不要把 Listener 当作 Java 事件监听器。

Listener 只使用 Fluxon。`handle` 必须遵循 `fluxon-language.md`；读取上下文变量必须使用 `&name`，调用扩展函数优先使用 `::`。脚本必须直接写在 `handle: |-` 中，不能创建独立 `.fs` 文件。通常省略 `type`，不要生成冗余的 `type: FLUXON`。

```fluxon
damage = event::damage()   // 错误：event 是字符串字面量
damage = &event::damage()  // 正确：读取 event 变量并调用扩展函数
```

## 最小配置

```yaml
mechanisms:
  listeners:
    on-damage:
      listen: "entity-damage-other"
      handle: |-
        # 在这里编写对应脚本类型的处理逻辑
```

完整字段结构：

```yaml
mechanisms:
  priority-listener: 0
  listeners:
    on-damage:
      listen: "entity-damage-other"
      priority: 0
      handle: |-
        # 脚本
```

## 字段

- `on-damage`：触发器 ID，可自定义；同一附魔内应保持唯一。
- `type`：历史兼容字段。当前唯一脚本系统是 Fluxon，通常省略，不要主动生成 `type: FLUXON`。
- `listen`：事件映射 ID，必填；它不是 Bukkit 事件类名。
- `priority`：同一附魔内多个 Listener 的执行顺序，默认 `0`，数值越小越早。
- `handle`：事件触发后执行的脚本。
- `priority-listener`：不同附魔之间的 Listener 排序，默认 `0`。

## 常用事件映射

默认 `core/event-mapping.yml` 提供以下映射：

```text
block-break
player-interact
entity-damage
player-toggle-sneak
entity-damage-other
entity-damaged-by-other
entity-death
player-item-damage
player-move
projectile-hit
block-damage
entity-shoot-bow
entity-target-living-entity
aiyatsbus-bow-charge-prepare
aiyatsbus-bow-charge-released
aiyatsbus-bow-charge-break
```

事件映射决定 Bukkit 事件类、检查槽位、触发实体引用、物品引用、Bukkit 事件优先级和是否忽略已取消事件。纯 YAML 附魔只能使用运行时已经注册的映射，通常就是 `core/event-mapping.yml` 中的默认项；不要在 `listen` 中填写任意 Bukkit 类名或猜测 ID。Java 附属插件可以通过事件执行器 API 注册 external mapping 和 resolver，但这不代表 YAML 能直接填写任意事件类。

## 默认映射语义

`player` 是历史变量名，实际保存事件解析出的 `LivingEntity`，并不保证是 Bukkit `Player`。这通常不是问题：效果可作用于任意 `LivingEntity` 时，直接使用生物通用 API，不要额外调用 `entity::isPlayer`。只有效果明确要求玩家，或后续必须调用 Player 专属 API 时才检查：

```fluxon
if !entity::isPlayer(&player) {
    return
}
```

默认映射的参与者和槽位如下。`全部装备槽` 指 `HAND`、`OFF_HAND`、`HEAD`、`CHEST`、`LEGS` 和 `FEET`。

| 映射 ID | Bukkit 事件 | `player` 的来源 | 检查槽位 | 特殊情况 |
| --- | --- | --- | --- | --- |
| `block-break` | `BlockBreakEvent` | 破坏方块的玩家 | 主手、副手 | 每个槽位分别解析装备物品 |
| `player-interact` | `PlayerInteractEvent` | 交互玩家 | 全部装备槽 | 不按事件手自动缩减槽位 |
| `entity-damage` | `EntityDamageEvent` | 受伤的 `LivingEntity` | 全部装备槽 | 可能是非玩家生物 |
| `player-toggle-sneak` | `PlayerToggleSneakEvent` | 切换潜行的玩家 | 全部装备槽 | 每个有效槽位都可能执行 |
| `entity-damage-other` | `EntityDamageByEntityEvent` | 攻击者 | 全部装备槽 | 只解析玩家攻击者，或投射物的 `LivingEntity` shooter；普通生物近战攻击者不会触发 |
| `entity-damaged-by-other` | `EntityDamageByEntityEvent` | 受害者 | 全部装备槽 | 受害者必须是 `LivingEntity` |
| `entity-death` | `EntityDeathEvent` | killer | 全部装备槽 | 没有 killer 时不触发 |
| `player-item-damage` | `PlayerItemDamageEvent` | 物品受损玩家 | 全部装备槽 | 默认按槽位取装备物品，不保证直接使用事件的 `item` |
| `player-move` | `PlayerMoveEvent` | 移动玩家 | 全部装备槽 | 视角变化也可能进入处理，不应假定只在跨方块移动时触发 |
| `projectile-hit` | `ProjectileHitEvent` | 投射物的 `LivingEntity` shooter | 主手、副手 | shooter 不是 `LivingEntity` 时不触发；三叉戟使用投射物保存的三叉戟物品 |
| `block-damage` | `BlockDamageEvent` | 损坏方块的玩家 | 主手、副手 | 每个槽位分别解析装备物品 |
| `entity-shoot-bow` | `EntityShootBowEvent` | 射击的 `LivingEntity` | 主手 | 可能是非玩家生物 |
| `entity-target-living-entity` | `EntityTargetLivingEntityEvent` | 发起索敌的 `LivingEntity` | 全部装备槽 | `player` 通常是索敌生物，不是被锁定目标 |
| `aiyatsbus-bow-charge-prepare` | `AiyatsbusBowChargeEvent.Prepare` | 蓄力玩家 | 主手、副手 | 弓事件扩展见 `aiyatsbus-fluxon-functions.md` |
| `aiyatsbus-bow-charge-released` | `AiyatsbusBowChargeEvent.Released` | 蓄力玩家 | 主手、副手 | 弓事件扩展见 `aiyatsbus-fluxon-functions.md` |
| `aiyatsbus-bow-charge-break` | `AiyatsbusBowChargeEvent.Break` | 蓄力玩家 | 主手、副手 | 弓事件扩展见 `aiyatsbus-fluxon-functions.md` |

伤害 resolver 的源码意图是任一方为 NPC 时停止，但当前双方检查分支没有 `return`，实际只会可靠排除最终解析成 `player` 变量的触发实体为 NPC。由此，`entity-damage-other` 中非 NPC 玩家攻击 NPC 受害者仍可能触发，`entity-damaged-by-other` 中 NPC 攻击非 NPC 受害者也仍可能触发。脚本需要排除另一方 NPC 时，必须从 `event` 取得攻击者或受害者并显式调用 `entity::isNPC(...)`。其他映射解析出的触发实体是 NPC 时会直接结束。

映射列出的每个槽位都会独立解析物品、检查附魔和执行限制。因此同一个 Bukkit 事件可能因多个有效槽位执行同一 Listener 多次。默认应通过附魔对象的 active slots 和 `Limitations` 控制生效槽位，不要例行判断 `triggerSlot`。只有效果明确区分槽位，或确实需要在多个合法槽位中选择一个时才判断它。

默认映射没有单独声明事件优先级和取消策略时，使用 `HIGHEST` 且 `ignoreCancelled = true`。自定义映射可以改变这两个值；Listener 是否接收已取消事件由映射决定，不由 Listener 节点决定。

## 执行条件

Listener 执行前会：

1. 根据事件映射解析触发实体、物品和槽位。
2. 确认物品拥有当前附魔。
3. 执行 `CheckType.USE` 限制检查。
4. 筛选 `listen` 与当前映射 ID 相同的 Listener。
5. 按 Listener 的 `priority` 执行脚本。

## 脚本变量

执行 `handle` 时会提供：

```text
event
player
item
enchant
level
maxLevel
triggerSlot: String?
trigger-slot: String?
container
globalContainer
附魔 variables 中的变量
```

`triggerSlot` 和 `trigger-slot` 是同一个值的两个名称，类型是可空槽位名称字符串，不是 `EquipmentSlot` 对象。常见值为 `"HAND"`、`"OFF_HAND"`、`"HEAD"`、`"CHEST"`、`"LEGS"` 和 `"FEET"`；无槽位映射时可能为 `null`。

`container` 是当前附魔私有的运行时 `HashMap<Any?, Any?>`，`globalContainer` 是所有附魔共享的全局运行时 Map。单一用途时直接使用整个 `container`，多种用途时才在其中嵌套 Map。完整结构、生命周期和持久化边界见 `fluxon-containers.md`。

特殊需求示例：只有效果明确限定主手时才写 `if &triggerSlot != "HAND" { return }`。普通附魔不要添加该判断。

脚本必须按所监听事件的实际 Bukkit 类型使用 `event`。调用事件和 Bukkit API 时同时遵循 `fluxon-bukkit-java-semantics.md`。

## 排错

- `listen` ID 没有在运行时注册时，Listener 永远不会执行。
- 映射指定的事件类在当前服务端版本不存在时，当前注册实现会跳过该映射，并且可能没有明确错误日志。
- 事件实体、killer 或 projectile shooter 无法解析成 `LivingEntity` 时不会执行。
- 映射槽位中的物品为空、没有当前附魔或未通过 `CheckType.USE` 时不会执行。
- `handle` 为空是合法配置，但触发后不会产生效果。
- 脚本预热失败不一定阻止 Listener 注册；还要检查首次触发时的编译错误。
- Java 附属插件排查 external mapping 时，应检查运行时事件执行器的默认映射和 external mapping，而不只核对 YAML 拼写。

需要内置函数或扩展 API 时读取 `fluxon-stdlib.md`；只有确实需要 JVM 反射、构造或模块能力时，再读取 `fluxon-jvm-interop.md` 或 `fluxon-modules.md`。非必要不要使用 `.` 反射访问。
