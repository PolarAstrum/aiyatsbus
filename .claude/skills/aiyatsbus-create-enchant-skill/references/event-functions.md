# EventFunctions 回调索引

以下 Java 回调由 `cc.polarastrum.aiyatsbus.core.data.trigger.builtin.EventFunctions` 定义。使用时导入对应的 Bukkit 事件类。

```java
void attackEntity(AiyatsbusEnchantment enchant, int level, EntityDamageByEntityEvent event);
void damagedByEntity(AiyatsbusEnchantment enchant, int level, EntityDamageByEntityEvent event);
void damagedByBlock(AiyatsbusEnchantment enchant, int level, EntityDamageByBlockEvent event);
void damaged(AiyatsbusEnchantment enchant, int level, EntityDamageEvent event);
void blockBreak(AiyatsbusEnchantment enchant, int level, BlockBreakEvent event);
void blockDamage(AiyatsbusEnchantment enchant, int level, BlockDamageEvent event);
void blockPlace(AiyatsbusEnchantment enchant, int level, BlockPlaceEvent event);
void interactLeft(AiyatsbusEnchantment enchant, int level, PlayerInteractEvent event);
void interactRight(AiyatsbusEnchantment enchant, int level, PlayerInteractEvent event);
void itemBreak(AiyatsbusEnchantment enchant, int level, PlayerItemBreakEvent event);
void itemConsume(AiyatsbusEnchantment enchant, int level, PlayerItemConsumeEvent event);
void itemDamage(AiyatsbusEnchantment enchant, int level, PlayerItemDamageEvent event);
void itemHeld(AiyatsbusEnchantment enchant, int level, PlayerItemHeldEvent event);
void move(AiyatsbusEnchantment enchant, int level, PlayerMoveEvent event);
void toggleSneak(AiyatsbusEnchantment enchant, int level, PlayerToggleSneakEvent event);
void toggleSprint(AiyatsbusEnchantment enchant, int level, PlayerToggleSprintEvent event);
void shootBow(AiyatsbusEnchantment enchant, int level, EntityShootBowEvent event);
void projectileLaunch(AiyatsbusEnchantment enchant, int level, ProjectileLaunchEvent event);
void projectileHitBlock(AiyatsbusEnchantment enchant, int level, ProjectileHitEvent event);
void projectileHitEntity(AiyatsbusEnchantment enchant, int level, ProjectileHitEvent event);
void death(AiyatsbusEnchantment enchant, int level, EntityDeathEvent event);
void kill(AiyatsbusEnchantment enchant, int level, EntityDeathEvent event);
void hunger(AiyatsbusEnchantment enchant, int level, FoodLevelChangeEvent event);
void regainHealth(AiyatsbusEnchantment enchant, int level, EntityRegainHealthEvent event);
void beTargeted(AiyatsbusEnchantment enchant, int level, EntityTargetLivingEntityEvent event);
void tickTask(AiyatsbusEnchantment enchant, int level, EquipmentSlot slot, Player player, int stamp);
```

接口还包含交互细分、物品、桶、附魔、传送和天气等回调。上方没有需要的事件时，直接检查 `EventFunctions.kt` 的准确方法名和参数类型，不要根据 Bukkit 事件名猜测。

## `tickTask` 调度语义

`tickTask` 不是 Bukkit 事件。Aiyatsbus 每 20 tick 遍历在线玩家和支持的装备槽位，并为拥有附魔且通过使用限制的物品调用该方法。它的 5 个参数分别表示：

- `enchant`：当前触发的 Aiyatsbus 附魔，可以用于读取变量和配置。
- `level`：物品上的附魔等级。
- `slot`：本次检查的装备槽位。
- `player`：持有或穿戴该物品的玩家。
- `stamp`：全局 tick 时间戳，每轮增加 20，通常为 `20、40、60...`。

使用取模控制执行间隔：

```java
@Override
public void tickTask(
        AiyatsbusEnchantment enchant,
        int level,
        EquipmentSlot slot,
        Player player,
        int stamp
) {
    int repeatTicks = 20;
    if (stamp % repeatTicks == 0) {
        player.setFoodLevel(Math.max(player.getFoodLevel() - 1, 0));
    }
}
```

`repeatTicks = 20` 表示约每 1 秒执行一次，`40` 表示约每 2 秒一次，`100` 表示约每 5 秒一次。间隔应为 20 的正整数倍，因为此回调每 20 tick 才调用一次，无法提供更高精度。

同一玩家可能因多个匹配槽位在同一轮触发多次。会修改玩家状态、产生伤害或消耗资源的效果必须考虑 `slot`，或者通过对象配置的 `active-slots` 限制生效槽位。
