# Fluxon 运行时容器

Aiyatsbus 会为 Fluxon 触发器注入两个运行时 Map：当前附魔私有的 `container`，以及插件全局共享的 `globalContainer`。

这两个容器都是运行时内存数据，不是 YAML 变量，也不是持久化存储。服务器重启、插件生命周期变化或附魔重载后的状态保留不应依赖容器；需要写入物品时使用 `MODIFIABLE`，需要数据库或跨重启存储时使用持久化方案。

## 变量

| 变量 | 类型 | 作用范围 | 适用场景 |
| --- | --- | --- | --- |
| `container` | `HashMap<Any?, Any?>` | 当前附魔 | 当前附魔内部共享的运行时状态、缓存、计数 |
| `globalContainer` | `HashMap<Any?, Any?>` | 插件全部附魔 | 跨附魔共享的运行时状态或缓存 |

两个变量会注入 Listener、Ticker 和 Skill 的脚本上下文。Ticker 的 `pre-handle`、`handle`、`post-handle` 也可以使用它们。

读取 Fluxon 变量必须使用 `&`：

```fluxon
value = &container::get("key")
&container::put("key", 1)
```

## 私有容器的结构

### 单一用途：直接使用容器

如果当前附魔只有一种用途，例如只记录玩家计数，就把整个 `container` 直接作为该用途的 Map 使用，不要额外嵌套一层 `player-counts` Map。

```fluxon
uuid = &player::uniqueId()::toString()
count = &container::getOrDefault(&uuid, 0) + 1
&container::put(&uuid, &count)
```

这里的 `container` 本身就是玩家计数表，玩家 UUID 直接作为 key。

### 多种用途：按用途嵌套 Map

只有同一个附魔需要保存多个互不相关的用途时，才在 `container` 内按用途保存多个 Map。

```fluxon
playerCounts = &container::getOrDefault("player-counts", new java.util.HashMap())
cooldowns = &container::getOrDefault("cooldowns", new java.util.HashMap())

uuid = &player::uniqueId()::toString()
count = &playerCounts::getOrDefault(&uuid, 0) + 1
&playerCounts::put(&uuid, &count)
&container::put("player-counts", &playerCounts)
```

不要为了统一结构，在只有一个用途时写成：

```fluxon
// 不必要的额外嵌套
playerCounts = &container::getOrDefault("player-counts", new java.util.HashMap())
```

选择规则：

- 只有一个用途：`container` 直接保存该用途的数据。
- 有多个用途：`container` 的 key 使用用途名，value 保存对应 Map 或对象。
- 单用途仍需按玩家区分：直接使用玩家 UUID 作为 `container` 的 key。
- 多用途且某一用途需按玩家区分：使用“用途名 Map -> 玩家 UUID -> 数据”的结构。

## 全局容器

`globalContainer` 被所有附魔共享。多个附魔可能同时读写它，因此 key 必须带有稳定的插件或附魔前缀，避免互相覆盖。

```fluxon
key = "my-addon:global-kills"
total = &globalContainer::getOrDefault(&key, 0) + 1
&globalContainer::put(&key, &total)
```

如果全局容器需要多个用途，也可以按用途嵌套 Map，但用途 key 仍应带前缀：

```fluxon
cache = &globalContainer::getOrDefault("my-addon:cache", new java.util.HashMap())
&cache::put("last-value", &value)
&globalContainer::put("my-addon:cache", &cache)
```

不要把附魔私有状态放入 `globalContainer`。只属于当前附魔的数据应放入 `container`。

## 与变量和持久化的区别

| 需求 | 推荐方式 |
| --- | --- |
| 当前附魔的临时运行时状态 | `container` |
| 多个附魔共享的临时运行时状态 | `globalContainer` |
| 物品上的累计值、充能值或状态 | `MODIFIABLE`，使用 PDC/NBT |
| 可配置的伤害、概率、时间、范围、数量等数值 | `LEVELED`，即使是固定值也定义变量 |
| 布尔、字符串、枚举名等固定配置 | `ORDINARY` |
| 跨服务器重启或数据库共享的数据 | 数据库或其他持久化系统 |

容器不是 `MODIFIABLE` 的替代品。需要让数据随物品移动、保存或重启后恢复时，必须使用对应的持久化机制。

## Ticker 状态注意事项

容器适合在 Ticker 的三个阶段之间传递运行时状态：

```fluxon
// pre-handle
&container::put("active", true)

// handle
if &container::getOrDefault("active", false) {
    // 周期效果
}

// post-handle
&container::remove("active")
```

但 Ticker 的重载、关闭和 recorder 状态存在已知限制，不能依赖 `post-handle` 一定执行。清理逻辑应允许重复执行，也不能因为容器中的旧状态而造成永久效果。

## 使用原则

- 优先使用 Fluxon Map 扩展函数，例如 `get`、`getOrDefault`、`put`、`remove`。
- 不要通过 Java 静态字段或反射自行创建同用途的全局 Map。
- 不要把 `container` 或 `globalContainer` 当作跨重启存储。
- 全局容器 key 使用插件或附魔前缀，例如 `my-addon:cache`。
- 私有容器单一用途时直接使用整个 Map，多用途时才增加嵌套 Map。
