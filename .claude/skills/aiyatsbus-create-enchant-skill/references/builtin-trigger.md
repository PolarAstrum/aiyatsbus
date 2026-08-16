# YAML Builtin 触发器

附魔信息保存在 YAML 中，但效果需要使用 Java 和 Bukkit API 时，编写 Java `Builtin` 触发器。

`Builtin` 是唯一机制，同一附魔只能配置一个 `mechanisms.builtin`。它和 Artifact 都由共用的 Builtin 事件执行器调度，但拥有不同的触发器类型，可以分别与 Listener、Ticker 或 Skill 共存。

## 触发器类

```java
package com.example.enchant;

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment;
import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.Builtin;
import org.bukkit.event.block.BlockBreakEvent;

public final class CustomBuiltin extends Builtin {

    public CustomBuiltin(AiyatsbusEnchantment enchant) {
        super(enchant);
    }

    @Override
    public void blockBreak(
            AiyatsbusEnchantment enchant,
            int level,
            BlockBreakEvent event
    ) {
        event.getPlayer().sendMessage("挖掘触发，等级 " + level);
    }
}
```

在 YAML 中填写类的全限定名称：

```yaml
mechanisms:
  builtin: "com.example.enchant.CustomBuiltin"
```

`Mechanism.init()` 会加载指定类，并调用接收一个 `AiyatsbusEnchantment` 的构造器。构造器必须为 `public`，且该类必须继承 `Builtin`。

## 回调规则

`Builtin` 实现了 `EventFunctions`。普通事件回调具有以下形式：

```java
void callback(AiyatsbusEnchantment enchant, int level, BukkitEvent event)
```

只覆写效果需要的回调。父类实现为空，通常不需要调用 `super`。不要把 `entity-damage-other` 等 Fluxon 监听器 ID 当作 Java 方法名，它们只属于 YAML 脚本监听器。

共用的 `Builtin` 执行器已经检查物品是否拥有附魔、附魔是否启用、触发概率是否成功，以及 `CheckType.USE` 限制是否通过。回调应专注于效果，没有具体理由时不要重复这些检查。

## 常见回调示例

```java
@Override
public void attackEntity(
        AiyatsbusEnchantment enchant,
        int level,
        EntityDamageByEntityEvent event
) {
    double bonusDamage = ((Number) enchant.getVariables()
        .leveled("额外伤害", level, false))
        .doubleValue();
    event.setDamage(event.getDamage() + bonusDamage);
}

@Override
public void tickTask(
        AiyatsbusEnchantment enchant,
        int level,
        EquipmentSlot slot,
        Player player,
        int stamp
) {
    int repeatTicks = ((Number) enchant.getVariables()
        .leveled("执行间隔", level, false))
        .intValue();
    int foodCost = ((Number) enchant.getVariables()
        .leveled("饥饿消耗", level, false))
        .intValue();
    if (repeatTicks > 0 && stamp % repeatTicks == 0) {
        player.setFoodLevel(Math.max(player.getFoodLevel() - foodCost, 0));
    }
}
```

对应的 YAML 数值配置为：

```yaml
variables:
  leveled:
    额外伤害: "点:1.0"
    执行间隔: "tick:20"
    饥饿消耗: "点:1"
```

`执行间隔` 没有随等级变化，但仍使用固定 LEVELED 表达式，而不是 ordinary。伤害、倍率、概率、持续时间、冷却、范围、速度、数量和消耗等可调玩法数值都应遵循该规则。只有布尔、字符串等非数值配置使用 ordinary；需要在物品 NBT/PDC 中持续修改的数据使用 modifiable。

`tickTask` 由 Aiyatsbus 每 20 tick 调用一次，`stamp` 每次增加 20，常见值为 `20、40、60...`。因此：

- `stamp % 20 == 0`：每次回调执行，约每 1 秒一次。
- `stamp % 40 == 0`：约每 2 秒一次。
- `stamp % 100 == 0`：约每 5 秒一次。

`执行间隔` 应为 20 的正整数倍。`tickTask` 的调用精度只有 20 tick，不能用它实现低于 1 秒的周期。若使用不是 20 倍数的值，实际周期由 `stamp` 何时同时整除该值决定，例如 `30` 会在 `stamp` 为 `60` 时首次满足条件，实际约每 3 秒执行一次，而不是每 1.5 秒执行一次。

按照 `event-functions.md` 使用准确的导入和签名。旧版本示例可能没有 `AiyatsbusEnchantment enchant` 参数，当前源码要求保留该参数。同一个回调可能为每个匹配的物品槽位各执行一次；如果玩家在多个匹配槽位上装备了该附魔，上述扣除饥饿值的效果也可能在同一轮执行多次。应通过附魔对象的 `active-slots` 限定生效槽位，或者在代码中明确处理 `slot`。不要在回调中重复注册全局监听器。
