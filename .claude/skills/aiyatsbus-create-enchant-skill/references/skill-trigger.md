# Skill 触发器

Skill 由玩家主动动作触发，内置动作判断、潜行条件、冷却、声音和粒子处理。项目当前没有自带的 Skill 附魔实例，以下结构根据 `Skill` 和 `DefaultAiyatsbusSkillHandler` 源码整理，生成后应在服务器中验证。

Skill 只使用 Fluxon。`handle` 必须遵循 `fluxon-language.md`；读取 `player`、`cooldown` 等变量时必须写成 `&player`、`&cooldown`，扩展函数优先使用 `::`。脚本必须直接写在 YAML 的 `handle: |-` 中，不能创建独立 `.fs` 文件。通常省略 `type`，不要生成冗余的 `type: FLUXON`。

## 配置结构

```yaml
variables:
  leveled:
    冷却: "秒:10-{level}"

mechanisms:
  priority-skill: 0
  skills:
    active-skill:
      action: RIGHT_CLICK
      shift-needed: false
      shift-ignored: true
      cooldown:
        name: 冷却
        enable: true
      sound: ENTITY_EXPERIENCE_ORB_PICKUP
      particle:
        type: HAPPY_VILLAGER
        amount: 5
      priority: 0
      handle: |-
        # 技能脚本
```

## 动作

`action` 支持：

```text
RIGHT_CLICK
LEFT_CLICK
SWAP
```

省略 `action` 时读取全局 `enchants/skill.yml`，默认值通常为 `RIGHT_CLICK`。

动作与底层事件的对应关系：

| action | Bukkit 事件 | 处理范围 | 原事件取消行为 |
| --- | --- | --- | --- |
| `RIGHT_CLICK` | `PlayerInteractEvent` | 只处理主手交互 | 即使事件已被其他插件取消仍可能进入 Skill；成功触发不会自动取消交互 |
| `LEFT_CLICK` | `PlayerInteractEvent` | 只处理主手交互 | 即使事件已被其他插件取消仍可能进入 Skill；成功触发不会自动取消交互 |
| `SWAP` | `PlayerSwapHandItemsEvent` | 始终读取事件处理时的主手物品 | 成功触发不会自动取消主副手交换 |

三种动作都从玩家当前主手物品解析附魔。`event` 的实际类型随 action 变化；需要阻止原版交互或交换时，脚本必须显式取消这个可取消事件，并遵循 `fluxon-bukkit-java-semantics.md` 的 setter 规则：

```fluxon
&event::setCancelled(true)
```

## 字段

- `type`：历史兼容字段。当前唯一脚本系统是 Fluxon，通常省略，不要主动生成 `type: FLUXON`。
- `handle`：技能触发后执行的脚本。
- `action`：触发动作；省略时读取全局设置。
- `shift-needed`：是否必须潜行；省略时读取全局设置。
- `shift-ignored`：潜行时是否忽略触发；省略时读取全局设置。
- `cooldown.name`：用于计算冷却时间的附魔变量名称；省略时读取全局设置。
- `cooldown.enable`：冷却未完成时是否发送提示；它不会关闭冷却检查。
- `sound`：成功触发时播放的 XSound 名称。
- `particle.type`：成功触发时播放的粒子类型。
- `particle.amount`：粒子数量，默认 `1`。
- `priority`：同一附魔内多个 Skill 的排序，默认 `0`。
- `priority-skill`：不同附魔之间的 Skill 排序，默认 `0`。

## 执行过程

1. 从玩家主手物品解析拥有 Skill 的附魔。
2. 排除 NPC，并执行 `CheckType.USE` 限制检查。
3. 筛选动作相同的 Skill，并按 `priority` 排序。
4. 检查潜行条件。
5. 从 `cooldown.name` 指定的附魔变量读取冷却值。
6. 冷却未完成时停止执行；`cooldown.enable` 只决定是否发送提示。
7. 记录冷却，播放声音和粒子，再执行 `handle`。

`shift-needed: true` 和 `shift-ignored: true` 会让非潜行和潜行状态都无法通过，应避免同时启用。

## 短路行为

当前实现中，潜行条件不满足或冷却未结束时使用的是整个事件处理函数的 `return`，不是“跳过当前 Skill 后继续尝试下一个”。因此：

- 高优先级 Skill 的潜行条件不匹配会终止本次动作的整个 Skill 分发。
- 高优先级 Skill 仍在冷却时，会终止当前附魔的后续 Skill，也可能阻止当前物品上后续附魔的 Skill。
- 不要依赖“第一个 Skill 不匹配后自动尝试第二个 Skill”。
- 多个 Skill 或多个带 Skill 的附魔共存时，必须谨慎设计 `priority-skill`、`priority`、action、潜行条件和冷却。

这是当前实现限制，不是推荐的组合控制流。若必须让多个候选技能互不阻塞，应避免让它们在同一动作上竞争，或改用 Listener/自定义 Java 逻辑统一分发。

## 冷却键

Skill 自动冷却以附魔的 `basic.id` 为键，不以 Skill ID 为键。同一附魔内的多个 Skill 共享一条自动冷却记录。

`cooldown::*` 宿主函数也以相同的附魔 ID 为键，因此 Skill 自动冷却与以下调用操作的是同一条记录：

```fluxon
cooldown::isReady(&player, &enchant, 5.0)
cooldown::addCooldown(&player, &enchant)
cooldown::removeCooldown(&player, &enchant)
```

Skill 已经自动检查并添加冷却，普通 Skill `handle` 不应再次调用 `addCooldown`，否则只会把冷却起点重置为稍后的时间。

## 副作用顺序

成功通过条件和冷却检查后，执行顺序固定为：

1. 写入附魔 ID 冷却。
2. 播放配置的声音。
3. 生成配置的粒子。
4. 调用 `AiyatsbusEnchantmentExecuteEvent`。
5. 事件未取消时执行 `handle`。

因此，取消 `AiyatsbusEnchantmentExecuteEvent` 只会阻止 `handle`，不会撤销已经写入的冷却，也不会回滚已经播放的声音和粒子。该事件不能用于“无副作用地完全否决技能”。脚本自身抛错同样不会自动回滚这些前置副作用。

## 脚本变量

执行 `handle` 时会提供：

```text
event
player
item
enchant
level
cooldown
maxLevel
container
globalContainer
附魔 variables 中的变量
```

冷却变量必须存在并能转换为数值。冷却计算读取不带单位的变量值，因此 `LEVELED` 冷却变量可以配置展示单位。

`container` 是当前附魔所有 Skill 共享的私有运行时 Map，不按 Skill ID 分开；`globalContainer` 是所有附魔共享的全局 Map。只有一个用途时直接使用整个 `container`，多个用途时才在其中嵌套 Map。需要物品或跨重启持久化时不要使用容器，详见 `fluxon-containers.md`。

全局 `enchants/skill.yml` 的 `privilege` 冷却减免列表必须至少保留一项。当前实现直接对该映射调用 `minOf`；将列表配置为空会在首次计算 Skill 冷却时抛出 `NoSuchElementException`。不需要权限减免时也应保留一个不会授予玩家、表达式原样返回 `{cooldown}` 的兜底项，而不是使用空列表。

`event` 为 `PlayerInteractEvent` 或 `PlayerSwapHandItemsEvent`；`item` 始终是处理时解析出的主手物品。Skill 不提供 `triggerSlot`，因为当前实现固定检查 `EquipmentSlot.HAND`。

## 排错

- Skill 完全不触发时，确认动作是三个合法枚举之一、事件来自主手、主手物品拥有附魔，并通过 `CheckType.USE`。
- 后续 Skill 不触发时，优先检查前一个 Skill 的潜行条件和冷却是否触发了全局短路。
- `cooldown.enable: false` 只关闭冷却提示，不会关闭冷却检查或记录。
- 同一附魔的另一个 Skill 意外进入冷却时，检查共享的 `basic.id` 冷却键。
- 冷却计算直接报错时，确认全局 `enchants/skill.yml` 的 `privilege` 至少包含一项且每项都采用 `permission:expression` 格式。
- 原交互或交换仍发生是正常行为；需要阻止时显式调用 `&event::setCancelled(true)`。
- `handle` 被取消或编译失败时，冷却、声音和粒子可能已经发生。

需要 Fluxon 内置函数和扩展函数时读取 `fluxon-stdlib.md`；确实需要 JVM 互操作或模块时，再读取 `fluxon-jvm-interop.md` 或 `fluxon-modules.md`。非必要不要使用 `.` 反射访问。
