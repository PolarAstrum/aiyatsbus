# Aiyatsbus Fluxon 函数用法

本文只描述 Aiyatsbus 附魔脚本可用的宿主函数。Fluxon 基础语法、标准库和 JVM 互操作分别见 `fluxon-language.md`、`fluxon-stdlib.md` 和 `fluxon-jvm-interop.md`。调用 Bukkit 或其他 Java API 时，必须同时遵循 `fluxon-bukkit-java-semantics.md`。

## API 优先级

本文已经提供的 Aiyatsbus 宿主函数优先于自行通过 JVM 互操作实现同一能力。编写脚本前应同时检索本文、`fluxon-stdlib.md`、`fluxon-modules.md`、`fluxon-language.md` 和 `fluxon-jvm-interop.md`；已有 API 能完成需求时不要重复造轮子。只有这些参考均没有对应能力时，才使用 `static`、反射或自行构造 Java 工具。

## 调用规则

Aiyatsbus 会自动导入本文涉及的 `aiy:*` 包，附魔 YAML 的脚本块中通常不需要手动 `import`。

普通模块使用上下文调用：

```fluxon
locations = block::getVein(&origin, 16)
ready = cooldown::isReady(&player, &enchant, 5.0)
```

也可以显式调用模块访问器，但通常没有必要：

```fluxon
locations = block()::getVein(&origin, 16)
```

必须遵循以下规则：

- 读取变量时使用 `&name`。裸标识符是字符串字面量，例如 `player` 表示字符串 `"player"`，`&player` 才表示玩家变量。
- 省略可选参数时只能从参数列表末尾开始省略。若要跳过中间参数，传入 `null`。
- 时间单位以具体函数说明为准。药水持续时间使用 tick，冷却使用秒，弓蓄力时间使用毫秒。
- Bukkit 对象可以继续通过 Fluxon 的 `::` 扩展或 `.` JVM 互操作访问；getter、setter、跨版本类型函数、`static` 和 `new` 的强制规则见 `fluxon-bukkit-java-semantics.md`。
- `FnBow` 没有 `bow()` 或 `bow::` 入口，只为弓蓄力事件对象提供扩展函数。

## `block`

### `getDrops`

```text
block::getDrops(block, item?, entity?) -> Collection<ItemStack>
```

- `block`：要计算掉落物的 `Block`，允许为 `null`。
- `item`：用于计算掉落的工具，可省略。
- `entity`：参与掉落计算的实体，可省略。
- 方块为 `null` 时返回空集合。
- 返回掉落物，但不会破坏方块，也不会把物品加入背包或掉落到世界。

```fluxon
drops = block::getDrops(&brokenBlock)
dropsWithTool = block::getDrops(&brokenBlock, &tool, &player)
dropsWithEntity = block::getDrops(&brokenBlock, null, &player)
```

### `getVein`

```text
block::getVein(block, amount?) -> List<Location>
```

- 查找与起始方块相连且材质相同的方块。
- `amount` 是最多返回的位置数量，省略时不限制。
- 返回值不包含起始方块本身。
- 搜索轴向相邻和二维对角相邻方块，不包含三轴同时偏移的角点。
- 对大型连续区域应主动设置 `amount`，避免一次搜索过多方块。

```fluxon
nearbyOre = block::getVein(&origin, 16)
allConnected = block::getVein(&origin)
```

## 弓蓄力事件扩展

这些函数直接调用在事件对象或 `ChargeInfo` 对象上。可用的监听器 ID 包括：

```yaml
aiyatsbus-bow-charge-prepare
aiyatsbus-bow-charge-released
aiyatsbus-bow-charge-break
```

### `Prepare`

准备事件用于决定是否允许玩家开始自定义蓄力。

| 调用 | 返回值 | 用途 |
| --- | --- | --- |
| `&event::player()` | `Player` | 获取蓄力玩家 |
| `&event::itemStack()` | `ItemStack` | 获取开始蓄力时使用的物品 |
| `&event::hand()` | `EquipmentSlot` | 获取使用的手 |
| `&event::isAllowed()` | `Boolean` | 查询是否允许开始蓄力 |
| `&event::setAllowed(allowed)` | `void` | 设置是否允许开始蓄力 |
| `&event::fire()` | `Prepare` | 再次分发该事件并返回事件自身 |
| `&event::release()` | `void` | 立即触发释放并结束当前蓄力 |
| `&event::interrupt()` | `void` | 以 `DAMAGED` 原因中断当前蓄力 |

`isAllowed` 初始为 `false`。允许本次蓄力时必须显式设置：

```fluxon
player = &event::player()
item = &event::itemStack()
&event::setAllowed(true)
```

不要在当前 `Prepare` 事件的监听脚本中直接调用 `fire()`，否则会再次分发同一个事件，可能造成递归或重复执行。

### `Released`

| 调用 | 返回值 | 用途 |
| --- | --- | --- |
| `&event::player()` | `Player` | 获取玩家 |
| `&event::itemStack()` | `ItemStack` | 获取蓄力物品 |
| `&event::hand()` | `EquipmentSlot` | 获取使用的手 |
| `&event::chargeInfo()` | `ChargeInfo` | 获取完整蓄力信息 |
| `&event::startTime()` | `Long` | 获取开始时间戳，单位为毫秒 |
| `&event::chargeTime()` | `Long` | 获取本次蓄力时长，单位为毫秒 |

```fluxon
player = &event::player()
chargeMillis = &event::chargeTime()
if &chargeMillis >= 1000L {
    // 至少蓄力 1 秒
}
```

### `Break`

| 调用 | 返回值 | 用途 |
| --- | --- | --- |
| `&event::player()` | `Player` | 获取玩家 |
| `&event::chargeInfo()` | `ChargeInfo` | 获取完整蓄力信息 |
| `&event::reason()` | `Break.Reason` | 获取中断原因 |
| `&event::source()` | `Event?` | 获取导致中断的 Bukkit 事件，可能为 `null` |
| `&event::startTime()` | `Long` | 获取开始时间戳，单位为毫秒 |
| `&event::chargeTime()` | `Long` | 获取中断前的蓄力时长，单位为毫秒 |

中断原因可能为 `DAMAGED`、`SKILL` 或 `PLUGIN`。

```fluxon
reason = &event::reason()::toString()
if &reason == "DAMAGED" {
    player = &event::player()
}
```

### `ChargeInfo`

| 调用 | 返回值 | 用途 |
| --- | --- | --- |
| `&info::player()` | `Player` | 获取玩家 |
| `&info::itemStack()` | `ItemStack` | 获取开始蓄力时保存的物品 |
| `&info::hand()` | `EquipmentSlot` | 获取使用的手 |
| `&info::startTime()` | `Long` | 获取开始时间戳，单位为毫秒 |
| `&info::chargeTime()` | `Long` | 获取蓄力时长，单位为毫秒 |
| `&info::stopTime()` | `Long` | 获取停止时间；未停止时为 `-1L` |
| `&info::setStopTime(time)` | `void` | 设置停止时间戳 |

```fluxon
info = &event::chargeInfo()
elapsed = &info::chargeTime()
```

蓄力尚未停止时，`chargeTime()` 会随当前时间增长。`setStopTime(...)` 只修改时间字段，不会触发释放或中断事件，也不会清理玩家的蓄力状态；正常脚本通常不需要调用它。

## `cooldown`

### `isReady`

```text
cooldown::isReady(player, enchant, seconds, broadcast?, broadcastInActionBar?) -> Boolean
```

- `player`：检查冷却的玩家。
- `enchant`：当前附魔对象；冷却以附魔 ID 为键。
- `seconds`：冷却长度，单位为秒。
- `broadcast`：冷却未结束时是否提示玩家，默认 `true`。
- `broadcastInActionBar`：是否使用 Action Bar 提示；省略时读取全局配置。
- 该函数只检查冷却，不会自动开始冷却。

```fluxon
if !cooldown::isReady(&player, &enchant, 5.0) {
    return
}

cooldown::addCooldown(&player, &enchant)
// 执行技能效果
```

静默检查：

```fluxon
if !cooldown::isReady(&player, &enchant, 5.0, false) {
    return
}
```

同一玩家、同一附魔的所有脚本分支共享同一个冷却键。当前 API 不支持传入自定义字符串冷却键。

### `addCooldown`

```text
cooldown::addCooldown(player, enchant) -> void
```

以当前时间开始或重置指定玩家、指定附魔的冷却。

```fluxon
cooldown::addCooldown(&player, &enchant)
```

### `removeCooldown`

```text
cooldown::removeCooldown(player, enchant) -> void
```

删除指定玩家、指定附魔的冷却记录。

```fluxon
cooldown::removeCooldown(&player, &enchant)
```

### `clearCooldown`

```text
cooldown::clearCooldown(player) -> void
```

清除该玩家的全部 Aiyatsbus 冷却记录，不限于当前附魔。只重置当前附魔时应使用 `removeCooldown`。

```fluxon
cooldown::clearCooldown(&player)
```

## `entity`

### `equippedItems`

```text
entity::equippedItems(entity) -> Map<EquipmentSlot, ItemStack>
```

返回生物各装备槽位对应的物品。空槽使用 `AIR` 物品表示，不是 `null`。

```fluxon
equipment = entity::equippedItems(&target)
```

### `realDamage`

```text
entity::realDamage(entity, damage, by?) -> void
```

- `entity`：受到伤害的 `LivingEntity`。
- `damage`：伤害数值。
- `by`：伤害来源实体，可省略。
- 该伤害通过直接扣减生命值并补发一次 Bukkit 伤害流程实现。
- 只应传入正数伤害。

```fluxon
entity::realDamage(&target, 6.0, &player)
```

### `entityName`

```text
entity::entityName(entity, player?) -> String
```

玩家返回账号名；其他实体优先返回自定义名称，否则返回本地化实体名称。可选的 `player` 用作本地化观察者。

```fluxon
name = entity::entityName(&target, &player)
```

### `addSafetyVelocity`

```text
entity::addSafetyVelocity(entity, vector, checkKnockback?) -> void
```

- 设置经过安全处理的速度向量。
- `checkKnockback` 默认 `false`；设为 `true` 时考虑实体抗击退属性。
- 函数名中的 `add` 不表示与原速度相加，最终会设置实体速度。
- 开启抗击退检查时，传入的 `Vector` 可能被缩放；之后仍需使用原值时应先克隆。

```fluxon
velocity = new org.bukkit.util.Vector(0.0, 1.0, 0.0)
entity::addSafetyVelocity(&target, &velocity, true)
```

### `isBehind`

```text
entity::isBehind(entity1, entity2) -> Boolean
```

判断 `entity1` 是否位于 `entity2` 身后。计算忽略 Y 轴；两个实体不在同一世界时返回 `false`。

```fluxon
if entity::isBehind(&attacker, &victim) {
    entity::realDamage(&victim, 4.0, &attacker)
}
```

参数顺序不能互换：第一个参数是被判断位置的实体，第二个参数是朝向基准实体。

### `addPotionEffect`

```text
entity::addPotionEffect(entity, type, duration, amplifier, ambient?, particles?, icon?) -> void
```

- `type`：药水效果名称，例如 `"SLOWNESS"`、`"GLOWING"`。
- `duration`：持续 tick；`20` tick 约为 1 秒。
- `amplifier`：从 0 开始，`0` 表示效果 I，`1` 表示效果 II。
- `ambient`、`particles`、`icon` 均默认 `true`。
- 无法识别的药水效果名称会抛出错误。

```fluxon
entity::addPotionEffect(&target, "SLOWNESS", 60, 1)
entity::addPotionEffect(&target, "GLOWING", 40, 0, false, true, true)
```

### `getActivePotionEffect`

```text
entity::getActivePotionEffect(entity, type) -> PotionEffect?
```

返回指定类型的活动药水效果，不存在时返回 `null`。

```fluxon
effect = entity::getActivePotionEffect(&target, "SLOWNESS")
if &effect != null {
    duration = &effect::duration()
}
```

### `hasPotionEffect`

```text
entity::hasPotionEffect(entity, type) -> Boolean
```

```fluxon
if entity::hasPotionEffect(&target, "POISON") {
    // 目标正处于中毒状态
}
```

### `removePotionEffect`

```text
entity::removePotionEffect(entity, type) -> void
```

移除指定类型的活动药水效果。

```fluxon
entity::removePotionEffect(&player, "SLOWNESS")
```

### `isNPC`

```text
entity::isNPC(entity) -> Boolean
```

根据当前启用的 NPC 兼容实现判断实体是否为 NPC。传入 `null` 时返回 `false`。

```fluxon
if entity::isNPC(&target) {
    return
}
```

### `isLivingEntity`

```text
entity::isLivingEntity(entity) -> Boolean
```

判断对象是否为 Bukkit `LivingEntity`。传入 `null` 时返回 `false`。

```fluxon
if !entity::isLivingEntity(&target) {
    return
}
```

### `isPlayer`

```text
entity::isPlayer(entity) -> Boolean
```

判断对象是否为 Bukkit `Player`。传入 `null` 时返回 `false`。

该函数不是 Listener 的默认前置检查。执行者可以是任意 `LivingEntity` 且效果能正常运行时，不要调用它；只有效果明确要求玩家，或后续必须使用 Player 专属 API 时才判断。

```fluxon
if entity::isPlayer(&target) {
    targetPlayer = &target
}
```

## `guard`

保护检查只判断操作是否允许，不会执行破坏、伤害或拾取。

### `canBreak`

```text
guard::canBreak(player, location) -> Boolean
```

询问当前启用的领地或反破坏兼容插件，玩家是否可以破坏指定位置。

```fluxon
if !guard::canBreak(&player, &location) {
    return
}
```

### `canDamage`

```text
guard::canDamage(player, entity) -> Boolean
```

询问当前保护兼容插件，玩家是否可以伤害指定实体。

```fluxon
if guard::canDamage(&player, &target) {
    entity::realDamage(&target, 4.0, &player)
}
```

### `isGuardItem`

```text
guard::isGuardItem(item, player?) -> Boolean
```

- `item` 必须是世界中的掉落物实体 `org.bukkit.entity.Item`，不是 `ItemStack`。
- 返回该掉落物是否应被保护、不能由当前操作拾取。
- 传入 `player` 时会同步触发 Bukkit 拾取事件进行检查，因此该函数不是纯查询。

```fluxon
if guard::isGuardItem(&droppedItem, &player) {
    return
}
```

## `inventory`

### `hasItem`

```text
inventory::hasItem(player, amount, predicate) -> Boolean
```

检查玩家背包中是否有足够数量满足 Lambda 条件的物品。Lambda 的隐式参数 `&it` 是当前候选 `ItemStack`，必须返回布尔值。

```fluxon
materialType = &seedItem::type()
hasSeed = inventory::hasItem(&player, 1, || &it::type() == &materialType)
```

### `takeItem`

```text
inventory::takeItem(player, amount, predicate) -> Boolean
```

从玩家背包扣除指定数量、满足条件的物品，并返回是否成功。该函数会直接修改背包，应检查返回值。

```fluxon
materialType = &seedItem::type()
if !inventory::takeItem(&player, 1, || &it::type() == &materialType) {
    return
}
```

## `item`

### `belongingTargetsId`

```text
item::belongingTargetsId(type: Material) -> List<String>
item::belongingTargetsId(item: ItemStack) -> List<String>
```

返回材质或物品所属的全部 Aiyatsbus 附魔目标 ID。同一物品可能属于多个目标。

```fluxon
idsFromItem = item::belongingTargetsId(&itemStack)
idsFromType = item::belongingTargetsId(&itemStack::type())
```

返回的是配置使用的目标 ID，不是本地化显示名称。不要传入 `null`。

### `isUnbreakable`

```text
item::isUnbreakable(item) -> Boolean
```

判断物品是否具有不可破坏属性。

```fluxon
if item::isUnbreakable(&itemStack) {
    return
}
```

## `player`

### `placeBlock`

```text
player::placeBlock(player, placedBlock, itemInHand?) -> Boolean
```

- 模拟触发一次 `BlockPlaceEvent`，返回该事件是否允许放置。
- `itemInHand` 省略时使用玩家当前手持物品。
- 该函数不会真正修改方块，也不会扣除物品。
- 事件使用的装备槽固定为主手 `HAND`。

正确用法是先检查事件，再由脚本完成方块修改和物品扣除：

```fluxon
if !player::placeBlock(&player, &targetBlock, &seedItem) {
    return
}
if !inventory::takeItem(&player, 1, || &it::type() == &seedItem::type()) {
    return
}
&targetBlock::setType(&cropMaterial)
```

## `world`

### `isDay`

```text
world::isDay(world) -> Boolean
```

判断世界当前是否为白天。

```fluxon
currentWorld = &player::world()
if world::isDay(&currentWorld) {
    // 白天效果
}
```

### `isNight`

```text
world::isNight(world) -> Boolean
```

判断世界当前是否为夜晚。

```fluxon
currentWorld = &player::world()
if world::isNight(&currentWorld) {
    // 夜晚效果
}
```

在时间边界 `12300` 和 `23850` 上，`isDay` 与 `isNight` 都返回 `false`，不要假设二者始终互为相反值。

### `spawnCircleParticles`

```text
world::spawnCircleParticles(particle, location, amount, option, range, factor?) -> void
```

- `particle`：Bukkit `Particle`。
- `location`：圆心位置。
- `amount`：每个采样点生成的粒子数。
- `option`：粒子的额外数据，必须匹配该粒子的 Bukkit 数据类型。
- `range`：圆半径。
- `factor`：圆周采样点数量，默认 `10`，应为正数。
- 总粒子量约为 `amount * factor`。

```fluxon
world::spawnCircleParticles(
    &particle,
    &location,
    1,
    &option,
    2.0,
    20
)
```

### `spawnRNAParticles`

```text
world::spawnRNAParticles(particle, location, amount, option, height, range, factor?, circle?) -> void
```

- 生成从 `-height` 到 `+height` 的螺旋粒子。
- `height`：中心上下各自延伸的高度。
- `range`：螺旋半径。
- `factor`：每圈采样数，默认 `10`，必须大于 `0`。
- `circle`：圈数，默认 `1`，必须大于 `0`。
- 通常生成 `factor * circle + 1` 个采样点。
- `option` 必须匹配粒子的 Bukkit 数据类型。

```fluxon
world::spawnRNAParticles(
    &particle,
    &location,
    1,
    &option,
    2.0,
    1.5,
    20,
    2
)
```

## `common`

### `debug`

```text
common::debug(text) -> void
```

将字符串解析为聊天组件，构建颜色后输出到 Aiyatsbus 调试日志。该函数只用于调试，不向玩家发送消息，也不返回结果。

```fluxon
common::debug("附魔触发，等级: ${&level}")
common::debug("目标: ${entity::entityName(&target)}")
```

需要输出变量时，先在 Fluxon 字符串插值中使用 `&变量`；裸标识符不会读取变量。`debug` 的参数必须是字符串，数值或 Bukkit 对象应先使用 `string(...)`，或放入字符串插值中。

## `variables`

### `ordinary`

```text
variables::ordinary(enchant, name) -> Any?
```

读取附魔的普通变量。变量不存在时返回 `null`。

```fluxon
value = variables::ordinary(&enchant, "最大深度")
```

### `leveled`

```text
variables::leveled(enchant, name, level, withUnit) -> Any
```

- 按指定等级计算 LEVELED 变量。
- `withUnit = false` 时返回 `Int` 或 `Double`，适合参与计算。
- `withUnit = true` 时返回带单位的 `String`，适合显示。
- 四个参数都必须提供。
- 变量不存在，或当前等级没有可用配置时会抛出错误。

```fluxon
damage = variables::leveled(&enchant, "伤害", &level, false)
displayDamage = variables::leveled(&enchant, "伤害", &level, true)
```

当前触发器通常已经把当前等级的变量注入脚本，直接读取时优先使用 `&伤害`。需要动态变量名、不同等级或带单位文本时再调用 `leveled`。

### `modifiable`

```text
variables::modifiable(enchant, item, name) -> Any
```

读取物品上的 MODIFIABLE 变量。变量可以存储在 PDC 或配置的 NBT 深层路径中；物品没有自定义值时返回变量默认值。

```fluxon
total = int(variables::modifiable(&enchant, &itemStack, "当前累计"))
```

PDC 存储值通常以字符串返回。参与数值运算前应使用 `int(...)` 或 `double(...)` 显式转换。变量名必须存在，物品不能为 `null`。

### `setModifiable`

```text
variables::setModifiable(enchant, item, name, value) -> ItemStack
```

把 MODIFIABLE 变量写入物品，并返回修改后的物品。传入值会先转换成字符串；传入 `null` 会写入字符串 `"null"`，不会删除变量。

```fluxon
itemStack = variables::setModifiable(
    &enchant,
    &itemStack,
    "当前累计",
    &newTotal
)
```

应接收返回的 `ItemStack`，不要依赖底层是否原地修改对象。变量名必须已经在附魔配置中定义。

## 常见组合

### 冷却技能

```fluxon
if !cooldown::isReady(&player, &enchant, 8.0) {
    return
}
cooldown::addCooldown(&player, &enchant)

entity::addPotionEffect(&player, "SPEED", 100, 1)
```

### 安全伤害实体

```fluxon
if !entity::isLivingEntity(&target) || entity::isNPC(&target) {
    return
}
if !guard::canDamage(&player, &target) {
    return
}

entity::realDamage(&target, &damage, &player)
```

### 消耗物品后模拟放置

```fluxon
materialType = &requiredItem::type()
if !inventory::hasItem(&player, 1, || &it::type() == &materialType) {
    return
}
if !player::placeBlock(&player, &targetBlock, &requiredItem) {
    return
}
if !inventory::takeItem(&player, 1, || &it::type() == &materialType) {
    return
}

&targetBlock::setType(&placedMaterial)
```
