# 纯代码附魔

当 Java 需要同时定义附魔信息和效果时使用此路线。入口是 Kotlin 接口向 Java 暴露的静态方法 `BuiltinAiyatsbusEnchantment.builder()`。

## 最小 Java 示例

```java
package com.example.enchant;

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment;
import cc.polarastrum.aiyatsbus.core.BuiltinAiyatsbusEnchantment;
import cc.polarastrum.aiyatsbus.core.data.BasicData;
import cc.polarastrum.aiyatsbus.core.data.Displayer;
import cc.polarastrum.aiyatsbus.core.data.trigger.builtin.EventFunctions;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomEnchantPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        BuiltinAiyatsbusEnchantment.builder()
            .basicData(BasicData.builder()
                .id("custom_enchant")
                .name("自定义附魔")
                .maxLevel(3)
                .build())
            .displayer(Displayer.builder()
                .generalDescription("挖掘方块时触发")
                .specificDescription("&7挖掘方块时触发")
                .build())
            .rarity("普通")
            .targets("镐")
            .eventExecutor(new EventFunctions() {
                @Override
                public void blockBreak(
                        AiyatsbusEnchantment enchant,
                        int level,
                        BlockBreakEvent event
                ) {
                    event.getPlayer().sendMessage("附魔触发，等级 " + level);
                }
            })
            .register();
    }
}
```

`JavaPlugin#onLoad()` 阶段的调用会将此外部附魔加入 Aiyatsbus 管理器的队列。Aiyatsbus 会在自己的 `JavaPlugin#onEnable()` 阶段、现代注册表冻结前完成实际注册。附属插件必须将 Aiyatsbus 声明为运行时依赖。不要把注册移动到事件回调或普通的 `onEnable()`。

如果附属插件已经使用 TabooLib，也可以使用 `@Awake(LifeCycle.LOAD)` 注册。此时需要由附属插件自行提供 TabooLib 依赖；普通 Bukkit 插件不应该为了创建 Aiyatsbus 附魔而引入 TabooLib。

## Builder 契约

- `BasicData.builder()` 支持 `enable`、`disableWorlds`、`id`、`name` 和 `maxLevel`。有效附魔至少应设置 ID、名称和最大等级。
- `Displayer.builder()` 支持 `display`、`previous`、`subsequent`、`generalDescription` 和 `specificDescription`。
- `rarity(String)` 按名称或 ID 解析已注册的 Aiyatsbus 品质。
- `targets(String...)` 解析已注册的附魔对象，例如 `剑`、`斧`、`弓`、`镐` 或 `all`。源码会丢弃未知对象。
- `addLimitation(LimitType, String)` 保存一条 `TYPE:value` 限制。
- `addVariable(VariableType, String, String)` 使用默认单位 `""` 添加变量。
- `addVariable(VariableType, String, String, String)` 添加带单位的变量。该方法通过 `@JvmOverloads` 为 Java 生成了 3 参数和 4 参数重载。
- `eventExecutor(EventFunctions)` 提供效果回调。
- `build()` 返回尚未注册的附魔。
- `register()` 创建附魔并通过 Aiyatsbus 公开管理器注册。

`AlternativeData.builder()` 是可选项。只在需要改变默认值时使用，例如 `.isVanilla(false)`、`.weight(50)` 或 `.inaccessible(true)`。`isVanilla(true)` 不是展示设置，它会改变注册路径，不应供普通自定义附魔使用。

## Java 变量

伤害、倍率、概率、持续时间、冷却、范围、速度、数量和消耗等可调玩法数值必须尽量通过 `addVariable(...)` 暴露，不要直接硬编码在 `EventFunctions` 回调中。数值默认使用 `VariableType.LEVELED`；没有等级成长需求时也使用固定表达式：

```java
.addVariable(
    VariableType.LEVELED,
    "伤害提升",
    "30",
    "%"
)
```

固定表达式不需要包含 `{level}`。需要随等级变化时再改为例如 `"20+10*{level}"`。回调通过 `enchant.getVariables().leveled("伤害提升", level, false)` 读取数值，描述通过 `{伤害提升}` 展示同一变量。

只有需要写入物品 NBT/PDC 并在运行时变化的数据才使用 `VariableType.MODIFIABLE`；布尔开关、字符串、枚举名、列表等非数值配置使用 `VariableType.ORDINARY`。不要把可调固定数值仅因“不随等级变化”而放入 ORDINARY。

不需要单位时，Java 可以使用 3 参数重载：

```java
.addVariable(
    VariableType.LEVELED,
    "damage",
    "15.0*{level}"
)
```

需要单位时，使用 4 参数重载：

```java
.addVariable(
    VariableType.LEVELED,
    "damage",
    "15.0*{level}",
    "点"
)
```

对于 `LEVELED` 变量，3 参数重载和 4 参数重载传入空字符串时都表示没有指定单位：

```java
.addVariable(VariableType.LEVELED, "test", "{level}*10")
.addVariable(VariableType.LEVELED, "test", "{level}*10", "")
```

只有传入非空字符串时才表示指定单位：

```java
.addVariable(VariableType.LEVELED, "test", "{level}*10", "个")
```

`addVariable(...)` 接受 `VariableType`。只有 `LEVELED` 的非空单位会自动进入描述展示值。ORDINARY 和 MODIFIABLE 无法配置展示单位；它们表示有单位数值时，应在 `specificDescription` 的占位符后手写单位，并让单位与变量保持同色，例如 `&7持续&a{test}秒&7才停止`。`generalDescription` 默认使用不含变量的自然语言概括，除非开发者明确要求，不要把具体变量放入 general。

## Java API 入口

`Aiyatsbus` 是 Kotlin `object`。Java 必须通过 `INSTANCE` 访问 API：

```java
Aiyatsbus.INSTANCE.api().getEnchantmentManager();
```

## 注册注意事项

- `BuiltinAiyatsbusEnchantment` 是接口，不是附魔实现的父类。
- 不要实例化 `AiyatsbusEnchantmentBase`，它是需要配置的抽象类。
- 普通附属代码不要直接调用 `getEnchantmentRegisterer()`。
- 在 `JavaPlugin#onLoad()` 生命周期中注册一次。若插件使用 TabooLib，也可以在对应的 `LifeCycle.LOAD` 中注册。默认管理器会在重载过程中保留纯代码附魔。
