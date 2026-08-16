# Fluxon 调用 Bukkit 与 Java API 的语义

本文定义 Aiyatsbus Fluxon 脚本调用 Bukkit API 和其他 Java API 时必须遵循的方法名、枚举值、静态成员和构造器规则。

调用 Java API 前先检查 `aiyatsbus-fluxon-functions.md`、`fluxon-modules.md`、`fluxon-stdlib.md` 和 `fluxon-language.md`。已有 Fluxon 或 Aiyatsbus API 能完成需求时，必须使用现有 API，不要通过 `static`、反射或自建 Java 工具重复实现。本文的 `static` 和 `new` 规则只适用于确实需要 JVM 互操作的场景。

## Getter

### 无参数 Getter

无参数 Java getter 在 Fluxon 中**必须去掉 `get` 前缀**。

```fluxon
name = &player::name()       // Player#getName()
world = &player::world()     // Entity#getWorld()
type = &item::type()         // ItemStack#getType()
location = &entity::location() // Entity#getLocation()
```

以下写法不可用：

```fluxon
name = &player::getName()
world = &player::getWorld()
type = &item::getType()
location = &entity::getLocation()
```

这不是代码风格偏好。Fluxon 暴露的无参数 getter 名称本身就不包含 `get`。

### 有参数 Getter

有参数 Java getter 在 Fluxon 中**必须保留 `get` 前缀**。

```fluxon
block = &world::getBlockAt(10, 64, 10)
effect = &player::getPotionEffect(&effectType)
value = &map::getOrDefault("key", "default")
```

以下写法不可用：

```fluxon
block = &world::blockAt(10, 64, 10)
effect = &player::potionEffect(&effectType)
value = &map::orDefault("key", "default")
```

判断规则只取决于 getter 是否接收参数：无参数时去掉 `get`，有参数时保留 `get`。

## Setter

Java setter 在 Fluxon 中正常调用，**必须保留 `set` 前缀**。

```fluxon
&player::setDisplayName("名字")
&block::setType(material("STONE"))
&entity::setVelocity(&velocity)
```

不要把 setter 改写成属性名：

```fluxon
&player::displayName("名字") // 不可用
&block::type(material("STONE")) // 不可用
```

## 十个跨版本兼容函数

以下十个函数用于获取常见 Bukkit 类型，并提供跨 Minecraft 版本兼容。这十个函数**优先于 `static`**。

| 函数 | 返回类型 | 用法 |
| --- | --- | --- |
| `material(name)` | `Material` | `material("STONE")` |
| `materialOrNull(name)` | `Material?` | `materialOrNull(&materialName)` |
| `particle(name)` | `Particle` | `particle("CRIT")` |
| `sound(name)` | `Sound` | `sound("ENTITY_PLAYER_ATTACK_STRONG")` |
| `soundOrNull(name)` | `Sound?` | `soundOrNull(&soundName)` |
| `patternType(name)` | `PatternType` | `patternType("STRIPE_DOWNLEFT")` |
| `enchantment(name)` | `Enchantment` | `enchantment("SHARPNESS")` |
| `entityType(name)` | `EntityType` | `entityType("ZOMBIE")` |
| `potionEffectType(name)` | `PotionEffectType` | `potionEffectType("SPEED")` |
| `attribute(name)` | `Attribute` | `attribute("GENERIC_ATTACK_DAMAGE")` |

```fluxon
stone = material("STONE")
possibleMaterial = materialOrNull(&materialName)
crit = particle("CRIT")
hitSound = sound("ENTITY_PLAYER_ATTACK_STRONG")
possibleSound = soundOrNull(&soundName)
stripe = patternType("STRIPE_DOWNLEFT")
sharpness = enchantment("SHARPNESS")
zombie = entityType("ZOMBIE")
speed = potionEffectType("SPEED")
attackDamage = attribute("GENERIC_ATTACK_DAMAGE")
```

不要优先通过 Bukkit 静态字段获取这十类值：

```fluxon
// 不要这样生成；缺少跨版本兼容处理
stone = static org.bukkit.Material.STONE
crit = static org.bukkit.Particle.CRIT
```

需要允许解析失败时，只有 `Material` 和 `Sound` 提供可空函数：

```fluxon
materialType = materialOrNull(&configuredMaterial)
soundType = soundOrNull(&configuredSound)

if &materialType == null || &soundType == null {
    return
}
```

`particle`、`patternType`、`enchantment`、`entityType`、`potionEffectType` 和 `attribute` 没有对应的 `OrNull` 形式，不要编造不存在的函数。

## `static`

除上述十个跨版本兼容函数对应的类型外，其他 Bukkit 枚举、其他 Java 枚举、Java 静态字段和 Java 静态方法都必须使用 `static`。

### Bukkit 枚举

```fluxon
hand = static org.bukkit.inventory.EquipmentSlot.HAND
gameMode = static org.bukkit.GameMode.SURVIVAL
action = static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
cause = static org.bukkit.event.entity.EntityDamageEvent$DamageCause.FALL
```

不要根据枚举类名自行编造首字母小写的转换函数：

```fluxon
hand = equipmentSlot("HAND") // 不可用，除非项目明确提供该函数
gameMode = gameMode("SURVIVAL") // 不可用，除非项目明确提供该函数
```

### 其他 Java 枚举和静态字段

```fluxon
color = static java.awt.Color.RED
roundingMode = static java.math.RoundingMode.HALF_UP
maxValue = static java.lang.Integer.MAX_VALUE
```

### Java 静态方法

```fluxon
number = static java.lang.Integer.parseInt("42")
box = static org.bukkit.util.BoundingBox.of(&first, &second)
```

### 嵌套类和嵌套枚举

通过 `static`、`new` 或 `forName` 访问 Java 嵌套类时，必须使用 JVM 二进制类名，并用 `$` 连接外部类和嵌套类。

```fluxon
operation = static org.bukkit.attribute.AttributeModifier$Operation.ADD_SCALAR
damageCause = static org.bukkit.event.entity.EntityDamageEvent$DamageCause.FALL
operationClass = forName("org.bukkit.attribute.AttributeModifier$Operation")
```

以下写法不可用：

```fluxon
operation = static org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR
damageCause = static org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL
```

## 构造器

Java 构造器必须使用 `new`，不能使用 `static`。

```fluxon
velocity = new org.bukkit.util.Vector(0.0, 1.0, 0.0)
item = new org.bukkit.inventory.ItemStack(material("STONE"), 1)
list = new java.util.ArrayList()
```

以下写法不可用：

```fluxon
velocity = static org.bukkit.util.Vector(0.0, 1.0, 0.0)
```

## 快速判断

编写 Bukkit 或 Java 调用时按以下顺序判断：

1. 实例 setter：保留 `set`，通过 `&target::setXxx(...)` 调用。
2. 无参数实例 getter：去掉 `get`，通过 `&target::xxx()` 调用。
3. 有参数实例 getter：保留 `get`，通过 `&target::getXxx(...)` 调用。
4. 获取十类跨版本 Bukkit 类型：使用对应的兼容函数。
5. 获取其他 Bukkit 或 Java 枚举值、静态字段，或调用静态方法：使用 `static`。
6. 调用构造器：使用 `new`。

```fluxon
materialType = material("STONE")
attackDamage = attribute("GENERIC_ATTACK_DAMAGE")
hand = static org.bukkit.inventory.EquipmentSlot.HAND
item = new org.bukkit.inventory.ItemStack(&materialType, 1)

name = &player::name()
block = &player::world()::getBlockAt(10, 64, 10)
&block::setType(&materialType)
```
