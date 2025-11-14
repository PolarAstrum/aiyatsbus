# Aiyatsbus Folia 兼容性迁移清单

> **文档版本**: 1.0
> **生成日期**: 2025-11-14
> **项目**: Aiyatsbus 附魔框架
> **目标**: 实现 Folia 完全兼容

---

## 📋 目录

- [1. 关键优先级修改 (P0)](#1-关键优先级修改-p0)
  - [1.1 全局调度器重构](#11-全局调度器重构)
  - [1.2 区域感知实现](#12-区域感知实现)
  - [1.3 NMS 操作迁移](#13-nms-操作迁移)
- [2. 重要修改 (P1)](#2-重要修改-p1)
  - [2.1 线程安全修复](#21-线程安全修复)
  - [2.2 事件处理优化](#22-事件处理优化)
- [3. 推荐修改 (P2)](#3-推荐修改-p2)
- [4. 兼容层实现方案](#4-兼容层实现方案)
- [5. 测试清单](#5-测试清单)

---

## 1. 关键优先级修改 (P0)

### 1.1 全局调度器重构

#### 🔴 **文件 1: DefaultAiyatsbusTickHandler.kt**

**位置**: `project/common-impl/src/main/kotlin/cc/polarastrum/aiyatsbus/impl/DefaultAiyatsbusTickHandler.kt`

| 行号 | 当前代码 | 问题 | 修改建议 |
|------|---------|------|---------|
| **47** | `private val routine: Table<AiyatsbusEnchantment, String, Long> = HashBasedTable.create()` | ❌ 非线程安全 | 使用 `Tables.synchronizedTable(HashBasedTable.create())` |
| **65-68** | `task = submit(period = 1L) { onTick() }` | ❌ 全局调度器 | 改为为每个玩家单独调度 |
| **79-141** | `onlinePlayers.forEach { player -> ... }` | ❌ 跨区域遍历 | 在玩家加入时为其注册区域调度器 |
| **97** | `item = player.inventory.getItem(slot)` | ❌ 跨区域访问 | 确保在玩家所在区域线程执行 |

**详细修改方案**:

```kotlin
// ❌ 旧代码 (第 62-68 行)
override fun start() {
    if (task != null) reset()
    task = submit(period = 1L) {
        onTick()
    }
}

private fun onTick() {
    routine.cellSet()
        .filter { counter % it.value == 0L }
        .sortedBy { it.rowKey.trigger!!.tickerPriority }
        .forEach {
            val ench = it.rowKey
            val id = it.columnKey
            val slots = ench.targets.flatMap { it.activeSlots }.toSet()

            onlinePlayers.forEach { player ->  // ❌ 全局遍历
                // ... 处理逻辑
            }
        }
    counter++
}

// ✅ 新代码 - Folia 兼容
private val playerTasks = ConcurrentHashMap<UUID, ScheduledTask>()

override fun start() {
    if (playerTasks.isNotEmpty()) reset()

    // 为当前在线玩家启动调度器
    onlinePlayers.forEach { player ->
        scheduleForPlayer(player)
    }
}

private fun scheduleForPlayer(player: Player) {
    // 为每个玩家在其实体调度器上运行
    val task = player.scheduler.runAtFixedRate(
        Aiyatsbus.plugin,
        { _ -> onPlayerTick(player) },
        null,  // 不退役的任务
        1L,    // 延迟 1 tick
        1L     // 周期 1 tick
    )
    playerTasks[player.uniqueId] = task
}

private fun onPlayerTick(player: Player) {
    routine.cellSet()
        .filter { counter % it.value == 0L }
        .sortedBy { it.rowKey.trigger!!.tickerPriority }
        .forEach {
            val ench = it.rowKey
            val id = it.columnKey
            val slots = ench.targets.flatMap { it.activeSlots }.toSet()

            var flag = false
            val record = recorder.computeIfAbsent(player.uniqueId) { mutableSetOf() }
            val ticker = ench.trigger!!.tickers[id]
                ?: error("Unknown ticker $id for enchantment ${ench.basicData.id}")

            val variables = mutableMapOf(
                "player" to player,
                "enchant" to ench,
            )
            variables += ench.variables.ordinary

            slots.forEach slot@{ slot ->
                val item: ItemStack
                try {
                    // 现在在正确的线程中访问
                    item = player.inventory.getItem(slot)
                } catch (_: Throwable) {
                    return@slot
                }
                if (item.isNull) return@slot

                val level = item.etLevel(ench)
                if (level > 0) {
                    val checkResult = ench.limitations.checkAvailable(CheckType.USE, item, player, slot)
                    if (checkResult.isFailure) {
                        sendDebug("----- DefaultAiyatsbusTickHandler -----")
                        sendDebug("附魔: " + ench.basicData.name)
                        sendDebug("原因: " + checkResult.reason)
                        sendDebug("----- DefaultAiyatsbusTickHandler -----")
                        return@slot
                    }
                    flag = true

                    val vars = variables.toMutableMap()
                    vars += mapOf(
                        "triggerSlot" to slot.name,
                        "trigger-slot" to slot.name,
                        "item" to item,
                        "level" to level,
                    )
                    vars += ench.variables.variables(level, item, false)

                    if (!record.contains(id)) {
                        record += id
                        ticker.execute(ticker.preHandle, player, vars)
                    }
                    ticker.execute(ticker.handle, player, vars)
                }
            }

            if (!flag && record.contains(id)) {
                record -= id
                ticker.execute(ticker.postHandle, player, variables)
            }
        }
    counter++
}

// 玩家加入时注册
@SubscribeEvent
fun onPlayerJoin(event: PlayerJoinEvent) {
    scheduleForPlayer(event.player)
}

// 玩家退出时清理
@SubscribeEvent
fun onPlayerQuit(event: PlayerQuitEvent) {
    playerTasks.remove(event.player.uniqueId)?.cancel()
    recorder.remove(event.player.uniqueId)
}

override fun reset() {
    counter = 0
    // 取消所有玩家任务
    playerTasks.values.forEach { it.cancel() }
    playerTasks.clear()
    routine.clear()
}
```

**新增依赖**:
```kotlin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
```

---

#### 🔴 **文件 2: DefaultAiyatsbusPlayerDataHandler.kt**

**位置**: `project/common-impl/src/main/kotlin/cc/polarastrum/aiyatsbus/impl/DefaultAiyatsbusPlayerDataHandler.kt`

| 行号 | 当前代码 | 问题 | 修改建议 |
|------|---------|------|---------|
| **49** | `private val data = mutableMapOf<UUID, PlayerData>()` | ❌ 非线程安全 | 改为 `ConcurrentHashMap<UUID, PlayerData>()` |
| **75-78** | `@Schedule(period = 600L)` + 全局遍历 | ❌ 全局调度器 | 为每个玩家单独调度保存任务 |

**详细修改方案**:

```kotlin
// ❌ 旧代码 (第 49 行)
private val data = mutableMapOf<UUID, PlayerData>()

// ❌ 旧代码 (第 75-78 行)
@Schedule(period = 600L)
fun tick() {
    onlinePlayers.forEach { Aiyatsbus.api().getPlayerDataHandler().save(it) }
}

// ✅ 新代码 - Folia 兼容
class DefaultAiyatsbusPlayerDataHandler : AiyatsbusPlayerDataHandler {

    // 线程安全的数据存储
    private val data = ConcurrentHashMap<UUID, PlayerData>()

    // 追踪每个玩家的保存任务
    private val saveTasks = ConcurrentHashMap<UUID, ScheduledTask>()

    override fun load(player: Player) {
        data[player.uniqueId] = PlayerData(player["aiyatsbus_data", PersistentDataType.STRING])

        // 启动该玩家的自动保存任务
        scheduleSaveTask(player)
    }

    override fun save(player: Player) {
        data[player.uniqueId]?.let {
            player["aiyatsbus_data", PersistentDataType.STRING] = it.serialize()
        }
    }

    override fun get(player: Player): PlayerData {
        return data[player.uniqueId]!!
    }

    private fun scheduleSaveTask(player: Player) {
        // 为每个玩家在其实体调度器上运行保存任务
        val task = player.scheduler.runAtFixedRate(
            Aiyatsbus.plugin,
            { _ -> save(player) },
            null,
            600L,  // 首次延迟 30 秒
            600L   // 每 30 秒执行一次
        )
        saveTasks[player.uniqueId] = task
    }

    companion object {
        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusPlayerDataHandler>(DefaultAiyatsbusPlayerDataHandler())
            reloadable {
                registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.PLAYER_DATA) {
                    onlinePlayers.forEach(PlatformFactory.getAPI<AiyatsbusPlayerDataHandler>()::load)
                }
            }
        }

        // 删除全局 @Schedule 方法

        @SubscribeEvent(priority = EventPriority.MONITOR)
        fun e(e: PlayerJoinEvent) {
            Aiyatsbus.api().getPlayerDataHandler().load(e.player)
        }

        @SubscribeEvent(priority = EventPriority.MONITOR)
        fun e(e: PlayerQuitEvent) {
            val handler = Aiyatsbus.api().getPlayerDataHandler() as DefaultAiyatsbusPlayerDataHandler
            handler.save(e.player)
            // 取消该玩家的保存任务
            handler.saveTasks.remove(e.player.uniqueId)?.cancel()
        }
    }
}
```

**新增导入**:
```kotlin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.ConcurrentHashMap
```

---

#### 🔴 **文件 3: ActionAwait.kt**

**位置**: `project/module-script/script-kether/src/main/kotlin/cc/polarastrum/aiyatsbus/module/script/kether/action/ActionAwait.kt`

| 行号 | 当前代码 | 问题 | 修改建议 |
|------|---------|------|---------|
| **39** | `submit(delay = ..., async = !isPrimaryThread)` | ⚠️ 全局调度 | 使用实体调度器 |

**详细修改方案**:

```kotlin
// ❌ 旧代码 (第 34-50 行)
@KetherParser(["a-wait", "a-delay", "a-sleep"])
fun actionWait() = scriptParser {
    val ticks = it.next(ArgTypes.ACTION)
    actionFuture { f ->
        newFrame(ticks).run<Double>().thenApply { d ->
            val task = submit(delay = (d * 20).roundToLong(), async = !isPrimaryThread) {
                if (script().sender?.isOnline() == false) {
                    ScriptService.terminateQuest(script())
                    return@submit
                }
                f.complete(null)
            }
            addClosable(AutoCloseable { task.cancel() })
        }
    }
}

// ✅ 新代码 - Folia 兼容
@KetherParser(["a-wait", "a-delay", "a-sleep"])
fun actionWait() = scriptParser {
    val ticks = it.next(ArgTypes.ACTION)
    actionFuture { f ->
        newFrame(ticks).run<Double>().thenApply { d ->
            val sender = script().sender
            val delayTicks = (d * 20).roundToLong()

            val task = if (sender is Player) {
                // 使用玩家的实体调度器
                sender.scheduler.runDelayed(
                    Aiyatsbus.plugin,
                    { _ ->
                        if (!sender.isOnline) {
                            ScriptService.terminateQuest(script())
                        } else {
                            f.complete(null)
                        }
                    },
                    null,
                    delayTicks
                )
            } else {
                // 非玩家情况使用异步调度器
                Bukkit.getAsyncScheduler().runDelayed(
                    Aiyatsbus.plugin,
                    { _ -> f.complete(null) },
                    delayTicks * 50,  // 转换为毫秒
                    TimeUnit.MILLISECONDS
                )
            }

            addClosable(AutoCloseable { task.cancel() })
        }
    }
}
```

**新增导入**:
```kotlin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit
```

---

#### 🔴 **文件 4: FastMultiBreak.kt**

**位置**: `project/module-script/script-kether/src/main/kotlin/cc/polarastrum/aiyatsbus/module/script/kether/operation/operation/FastMultiBreak.kt`

| 行号 | 当前代码 | 问题 | 修改建议 |
|------|---------|------|---------|
| **43** | `submit(delay = 0, period = 1)` | ❌ 全局调度器 | 使用区域调度器 |

**详细修改方案**:

```kotlin
// ❌ 旧代码
submit(delay = 0, period = 1) {
    // 处理方块破坏
}

// ✅ 新代码 - Folia 兼容
fun scheduleMultiBreak(player: Player, blocks: List<Block>) {
    player.scheduler.runAtFixedRate(
        Aiyatsbus.plugin,
        { _ ->
            // 处理方块破坏逻辑
            // 确保所有方块都在玩家附近（同一区域）
        },
        null,
        0L,
        1L
    )
}
```

---

#### 🔴 **文件 5: Aiming.kt**

**位置**: `project/module-script/script-kether/src/main/kotlin/cc/polarastrum/aiyatsbus/module/script/kether/operation/operation/Aiming.kt`

| 行号 | 当前代码 | 问题 | 修改建议 |
|------|---------|------|---------|
| **70** | `submit(delay = 1L, period = ticks)` | ❌ 全局调度器 | 使用实体调度器 |

**详细修改方案**:

```kotlin
// ❌ 旧代码 (第 70 行)
submit(delay = 1L, period = ticks) {
    // 瞄准逻辑
}

// ✅ 新代码 - Folia 兼容
fun scheduleAiming(player: Player, ticks: Long) {
    player.scheduler.runAtFixedRate(
        Aiyatsbus.plugin,
        { _ ->
            // 瞄准逻辑
        },
        null,
        1L,
        ticks
    )
}
```

---

#### 🔴 **文件 6: PickNearItems.kt**

**位置**: `project/module-script/script-kether/src/main/kotlin/cc/polarastrum/aiyatsbus/module/script/kether/operation/operation/PickNearItems.kt`

| 行号 | 当前代码 | 问题 | 修改建议 |
|------|---------|------|---------|
| **48** | `submit(delay = checkDelay)` | ❌ 全局调度器 | 使用实体调度器 |

**详细修改方案**:

```kotlin
// ❌ 旧代码 (第 48 行)
submit(delay = checkDelay) {
    // 拾取物品逻辑
}

// ✅ 新代码 - Folia 兼容
fun scheduleItemPickup(player: Player, checkDelay: Long) {
    player.scheduler.runDelayed(
        Aiyatsbus.plugin,
        { _ ->
            // 拾取物品逻辑
        },
        null,
        checkDelay
    )
}
```

---

### 1.2 区域感知实现

#### 🔴 **文件 7: Entities.kt**

**位置**: `project/common/src/main/kotlin/cc/polarastrum/aiyatsbus/core/util/Entities.kt`

**需要修改的函数**:

| 函数名 | 行号范围 | 问题 | 修改建议 |
|-------|---------|------|---------|
| `Player.doBreakBlock(block: Block)` | ~240-270 | ❌ 直接方块操作 | 添加区域检查 |
| `Player.placeBlock(...)` | ~200-210 | ❌ 直接方块操作 | 添加区域检查 |

**详细修改方案**:

```kotlin
// ❌ 旧代码
fun Player.doBreakBlock(block: Block) {
    try {
        block.mark("block-ignored")
        Aiyatsbus.api().getMinecraftAPI().getWorldOperator().breakBlock(this, block)
    } catch (ex: Throwable) {
        ex.printStackTrace()
    } finally {
        if (block.type != Material.AIR) {
            if (AiyatsbusSettings.supportItemsAdder && itemsAdderEnabled) {
                CustomBlock.getLoot(block, inventory.itemInMainHand, true).forEach {
                    world.dropItem(block.location, it)
                }
                CustomBlock.remove(block.location)
            } else {
                block.breakNaturally(inventory.itemInMainHand)
            }
        }
        block.unmark("block-ignored")
    }
}

// ✅ 新代码 - Folia 兼容
fun Player.doBreakBlock(block: Block) {
    // 检查玩家和方块是否在同一区域
    if (!isSameRegion(this.location, block.location)) {
        // 跨区域操作，需要调度到方块所在区域
        Bukkit.getRegionScheduler().run(
            Aiyatsbus.plugin,
            block.location
        ) { _ ->
            actualBreakBlock(block)
        }
    } else {
        // 同一区域，直接执行
        actualBreakBlock(block)
    }
}

private fun Player.actualBreakBlock(block: Block) {
    try {
        block.mark("block-ignored")
        Aiyatsbus.api().getMinecraftAPI().getWorldOperator().breakBlock(this, block)
    } catch (ex: Throwable) {
        ex.printStackTrace()
    } finally {
        if (block.type != Material.AIR) {
            if (AiyatsbusSettings.supportItemsAdder && itemsAdderEnabled) {
                CustomBlock.getLoot(block, inventory.itemInMainHand, true).forEach {
                    world.dropItem(block.location, it)
                }
                CustomBlock.remove(block.location)
            } else {
                block.breakNaturally(inventory.itemInMainHand)
            }
        }
        block.unmark("block-ignored")
    }
}

// 新增工具函数
fun isSameRegion(loc1: Location, loc2: Location): Boolean {
    if (loc1.world != loc2.world) return false

    // Folia 中，同一区块 = 同一区域（简化假设）
    return loc1.chunk == loc2.chunk
}
```

**新增导入**:
```kotlin
import org.bukkit.Bukkit
```

---

#### 🔴 **文件 8: 新建区域工具类**

**位置**: `project/common/src/main/kotlin/cc/polarastrum/aiyatsbus/core/util/FoliaUtils.kt` (新建文件)

**完整内容**:

```kotlin
package cc.polarastrum.aiyatsbus.core.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Folia 兼容性工具类
 *
 * 提供区域感知的调度和操作
 */
object FoliaUtils {

    /**
     * 检测当前服务器是否为 Folia
     */
    val isFolia: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * 检查两个位置是否在同一区域
     */
    fun isSameRegion(loc1: Location, loc2: Location): Boolean {
        if (loc1.world?.uid != loc2.world?.uid) return false

        // Folia 中每个区块是一个独立的区域
        val chunk1 = loc1.chunk
        val chunk2 = loc2.chunk

        return chunk1.x == chunk2.x && chunk1.z == chunk2.z
    }

    /**
     * 在实体所在区域执行任务
     */
    fun runOnEntity(plugin: Plugin, entity: Entity, task: Runnable) {
        if (isFolia) {
            entity.scheduler.run(plugin, { _ -> task.run() }, null)
        } else {
            // Paper/Spigot 回退
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    /**
     * 在实体所在区域延迟执行任务
     */
    fun runOnEntityDelayed(plugin: Plugin, entity: Entity, task: Runnable, delay: Long) {
        if (isFolia) {
            entity.scheduler.runDelayed(plugin, { _ -> task.run() }, null, delay)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay)
        }
    }

    /**
     * 在实体所在区域周期性执行任务
     */
    fun runOnEntityTimer(plugin: Plugin, entity: Entity, task: Runnable, delay: Long, period: Long) {
        if (isFolia) {
            entity.scheduler.runAtFixedRate(plugin, { _ -> task.run() }, null, delay, period)
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period)
        }
    }

    /**
     * 在区域调度器上执行任务
     */
    fun runOnRegion(plugin: Plugin, location: Location, task: Runnable) {
        if (isFolia) {
            Bukkit.getRegionScheduler().run(plugin, location) { _ -> task.run() }
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    /**
     * 在全局区域执行任务（不依赖特定位置）
     */
    fun runGlobal(plugin: Plugin, task: Runnable) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().run(plugin) { _ -> task.run() }
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    /**
     * 异步执行任务
     */
    fun runAsync(plugin: Plugin, task: Runnable) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin) { _ -> task.run() }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
        }
    }
}
```

---

### 1.3 NMS 操作迁移

#### 🔴 **文件 9: DefaultMinecraftWorldOperator.kt**

**位置**: `project/module-nms/src/main/kotlin/cc/polarastrum/aiyatsbus/impl/nms/DefaultMinecraftWorldOperator.kt`

| 函数名 | 问题 | 修改建议 |
|-------|------|---------|
| `breakBlock(player, block)` | ❌ 直接 NMS 调用 | 添加线程检查 |

**详细修改方案**:

```kotlin
// ❌ 旧代码
class DefaultMinecraftWorldOperator : MinecraftWorldOperator {
    override fun breakBlock(player: Player, block: Block): Boolean {
        return (player as CraftPlayer).handle.gameMode.destroyBlock(
            BlockPosition(block.x, block.y, block.z)
        )
    }
}

// ✅ 新代码 - Folia 兼容
class DefaultMinecraftWorldOperator : MinecraftWorldOperator {
    override fun breakBlock(player: Player, block: Block): Boolean {
        // 确保在正确的线程中执行
        if (FoliaUtils.isFolia && !Bukkit.isOwnedByCurrentRegion(block.location)) {
            throw IllegalStateException(
                "Attempted to access block at ${block.location} from wrong region thread"
            )
        }

        return (player as CraftPlayer).handle.gameMode.destroyBlock(
            BlockPosition(block.x, block.y, block.z)
        )
    }
}
```

**新增导入**:
```kotlin
import cc.polarastrum.aiyatsbus.core.util.FoliaUtils
import org.bukkit.Bukkit
```

---

## 2. 重要修改 (P1)

### 2.1 线程安全修复

#### 🟡 **文件 10: DefaultAiyatsbusEventExecutor.kt**

**位置**: `project/common-impl/src/main/kotlin/cc/polarastrum/aiyatsbus/impl/DefaultAiyatsbusEventExecutor.kt`

| 行号 | 当前代码 | 问题 | 修改建议 |
|------|---------|------|---------|
| **77** | `private val listeners: Table<...> = HashBasedTable.create()` | ❌ 非线程安全 | 使用同步包装 |

**详细修改方案**:

```kotlin
// ❌ 旧代码 (第 77 行)
private val listeners: Table<String, EventPriority, ProxyListener> = HashBasedTable.create()

// ✅ 新代码
private val listeners: Table<String, EventPriority, ProxyListener> =
    Tables.synchronizedTable(HashBasedTable.create())
```

**新增导入**:
```kotlin
import com.google.common.collect.Tables
```

---

#### 🟡 **文件 11: DefaultAiyatsbusEnchantmentManager.kt**

**位置**: `project/common-impl/src/main/kotlin/cc/polarastrum/aiyatsbus/impl/DefaultAiyatsbusEnchantmentManager.kt`

**需要检查的字段**:

| 行号 | 字段 | 当前类型 | 是否线程安全 |
|------|------|---------|------------|
| 需定位 | `byKeyMap` | `ConcurrentHashMap` | ✅ 已安全 |
| 需定位 | `byKeyStringMap` | `ConcurrentHashMap` | ✅ 已安全 |
| 需定位 | `byNameMap` | `ConcurrentHashMap` | ✅ 已安全 |
| 需定位 | `enchantmentsToRegister` | `CopyOnWriteArraySet` | ✅ 已安全 |

**操作**: 使用 Grep 找到文件位置并确认，如有非并发容器需替换。

---

### 2.2 事件处理优化

#### 🟡 **文件 12: DefaultAiyatsbusEventExecutor.kt**

**位置**: `project/common-impl/src/main/kotlin/cc/polarastrum/aiyatsbus/impl/DefaultAiyatsbusEventExecutor.kt`

| 行号 | 函数 | 问题 | 修改建议 |
|------|------|------|---------|
| **193-242** | `processEvent(...)` | ⚠️ 假设单线程 | 添加线程安全检查 |

**详细修改方案**:

```kotlin
// ✅ 在函数开头添加区域检查
private fun processEvent(listen: String, event: Event, eventMapping: EventMapping, eventPriority: EventPriority) {
    val resolver = getResolver(event) ?: return

    resolver.eventResolver.apply(event)

    var (entity, entityResolved) = resolver.entityResolver.apply(event, eventMapping.playerReference)

    if (entity == null && !entityResolved) {
        entity = event.invokeMethodDeep<LivingEntity>(eventMapping.playerReference ?: return) ?: return
    }

    if (entity == null) return
    if (entity.checkIfIsNPC()) return

    // ✅ 新增：Folia 线程安全检查
    if (FoliaUtils.isFolia) {
        if (!Bukkit.isOwnedByCurrentRegion(entity)) {
            // 实体不在当前区域，重新调度
            FoliaUtils.runOnEntity(Aiyatsbus.plugin, entity) {
                processEvent(listen, event, eventMapping, eventPriority)
            }
            return
        }
    }

    // 原有处理逻辑...
    if (eventMapping.slots.isNotEmpty()) {
        eventMapping.slots.forEach { slot ->
            val item: ItemStack?
            try {
                item = entity.equipment?.getItem(slot)
            } catch (_: Throwable) {
                return@forEach
            }
            if (item.isNull) return@forEach
            item!!.triggerEts(listen, event, entity, slot, false)
        }
    } else {
        // ... 其他逻辑
    }
}
```

**新增导入**:
```kotlin
import cc.polarastrum.aiyatsbus.core.util.FoliaUtils
import org.bukkit.Bukkit
```

---

## 3. 推荐修改 (P2)

### 3.1 Registry 生命周期管理

#### 🟢 **文件 13: Registry.kt**

**位置**: `project/common/src/main/kotlin/cc/polarastrum/aiyatsbus/core/RegistryItem.kt`

| 字段/行号 | 问题 | 修改建议 |
|----------|------|---------|
| `isLoaded` 变量 | ⚠️ 缺少 volatile | 添加 `@Volatile` 或使用 `AtomicBoolean` |

**详细修改方案**:

```kotlin
// ❌ 旧代码
private var isLoaded = false

// ✅ 新代码 - 选项 1: Volatile
@Volatile
private var isLoaded = false

// ✅ 新代码 - 选项 2: AtomicBoolean（推荐）
private val isLoaded = AtomicBoolean(false)

// 使用时修改：
if (!isLoaded.get()) {
    // ...
    isLoaded.set(true)
}
```

---

### 3.2 性能优化建议

#### 🟢 **所有涉及 `onlinePlayers` 的位置**

**搜索命令**:
```bash
grep -r "onlinePlayers" project/
```

**需要检查的模式**:
- `onlinePlayers.forEach { ... }` - 可能需要区域调度
- `onlinePlayers.filter { ... }` - 可能跨区域访问数据

**建议**: 为每个使用点添加注释说明是否 Folia 兼容。

---

## 4. 兼容层实现方案

### 4.1 创建调度器适配器

#### 📄 **新建文件: SchedulerAdapter.kt**

**位置**: `project/common/src/main/kotlin/cc/polarastrum/aiyatsbus/core/scheduler/SchedulerAdapter.kt` (新建)

**完整内容**:

```kotlin
package cc.polarastrum.aiyatsbus.core.scheduler

import cc.polarastrum.aiyatsbus.core.util.FoliaUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

/**
 * 调度器适配器接口
 *
 * 提供统一的调度 API，自动适配 Paper 和 Folia
 */
interface SchedulerAdapter {

    /**
     * 为实体调度任务
     */
    fun runForEntity(entity: Entity, task: Runnable)

    /**
     * 为实体延迟调度任务
     */
    fun runForEntityDelayed(entity: Entity, task: Runnable, delay: Long)

    /**
     * 为实体周期性调度任务
     */
    fun runForEntityTimer(entity: Entity, task: Runnable, delay: Long, period: Long): TaskHandle

    /**
     * 在指定位置调度任务
     */
    fun runAtLocation(location: Location, task: Runnable)

    /**
     * 全局任务调度
     */
    fun runGlobal(task: Runnable)

    /**
     * 异步任务调度
     */
    fun runAsync(task: Runnable)
}

/**
 * 任务句柄
 */
interface TaskHandle {
    fun cancel()
    fun isCancelled(): Boolean
}

/**
 * Paper/Spigot 调度器实现
 */
class PaperScheduler(private val plugin: Plugin) : SchedulerAdapter {

    override fun runForEntity(entity: Entity, task: Runnable) {
        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun runForEntityDelayed(entity: Entity, task: Runnable, delay: Long) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delay)
    }

    override fun runForEntityTimer(entity: Entity, task: Runnable, delay: Long, period: Long): TaskHandle {
        val taskId = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period).taskId
        return object : TaskHandle {
            override fun cancel() {
                Bukkit.getScheduler().cancelTask(taskId)
            }

            override fun isCancelled(): Boolean {
                return !Bukkit.getScheduler().isCurrentlyRunning(taskId) &&
                       !Bukkit.getScheduler().isQueued(taskId)
            }
        }
    }

    override fun runAtLocation(location: Location, task: Runnable) {
        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun runGlobal(task: Runnable) {
        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun runAsync(task: Runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
    }
}

/**
 * Folia 调度器实现
 */
class FoliaScheduler(private val plugin: Plugin) : SchedulerAdapter {

    override fun runForEntity(entity: Entity, task: Runnable) {
        entity.scheduler.run(plugin, { _ -> task.run() }, null)
    }

    override fun runForEntityDelayed(entity: Entity, task: Runnable, delay: Long) {
        entity.scheduler.runDelayed(plugin, { _ -> task.run() }, null, delay)
    }

    override fun runForEntityTimer(entity: Entity, task: Runnable, delay: Long, period: Long): TaskHandle {
        val scheduledTask = entity.scheduler.runAtFixedRate(
            plugin,
            { _ -> task.run() },
            null,
            delay,
            period
        )

        return object : TaskHandle {
            override fun cancel() {
                scheduledTask.cancel()
            }

            override fun isCancelled(): Boolean {
                return scheduledTask.isCancelled
            }
        }
    }

    override fun runAtLocation(location: Location, task: Runnable) {
        Bukkit.getRegionScheduler().run(plugin, location) { _ -> task.run() }
    }

    override fun runGlobal(task: Runnable) {
        Bukkit.getGlobalRegionScheduler().run(plugin) { _ -> task.run() }
    }

    override fun runAsync(task: Runnable) {
        Bukkit.getAsyncScheduler().runNow(plugin) { _ -> task.run() }
    }
}

/**
 * 调度器工厂
 */
object SchedulerFactory {

    private lateinit var adapter: SchedulerAdapter

    fun initialize(plugin: Plugin) {
        adapter = if (FoliaUtils.isFolia) {
            FoliaScheduler(plugin)
        } else {
            PaperScheduler(plugin)
        }
    }

    fun getScheduler(): SchedulerAdapter {
        return adapter
    }
}
```

**使用示例**:

```kotlin
// 在插件主类初始化
class AiyatsbusPlugin : JavaPlugin() {
    override fun onEnable() {
        SchedulerFactory.initialize(this)
        // ...
    }
}

// 使用适配器
val scheduler = SchedulerFactory.getScheduler()

// 为玩家调度任务
scheduler.runForEntityTimer(player, task = {
    // 执行逻辑
}, delay = 1L, period = 20L)
```

---

### 4.2 Plugin 实例访问

#### 📄 **修改文件: Aiyatsbus.kt**

**位置**: `project/common/src/main/kotlin/cc/polarastrum/aiyatsbus/core/Aiyatsbus.kt`

**添加 Plugin 实例访问**:

```kotlin
object Aiyatsbus {

    private lateinit var pluginInstance: Plugin

    fun setPlugin(plugin: Plugin) {
        pluginInstance = plugin
    }

    fun getPlugin(): Plugin {
        return pluginInstance
    }

    // 现有代码...
    fun api(): AiyatsbusAPI {
        return PlatformFactory.getAPI()
    }
}
```

**在插件主类中初始化**:

找到插件主类（通常在 `module-bukkit` 模块），添加：

```kotlin
class AiyatsbusPlugin : JavaPlugin() {
    override fun onEnable() {
        Aiyatsbus.setPlugin(this)
        SchedulerFactory.initialize(this)
        // ...
    }
}
```

---

## 5. 测试清单

### 5.1 单元测试

创建文件: `project/module-bukkit/src/test/kotlin/FoliaCompatibilityTest.kt`

```kotlin
package cc.polarastrum.aiyatsbus.test

import cc.polarastrum.aiyatsbus.core.util.FoliaUtils
import org.junit.Test
import kotlin.test.assertNotNull

class FoliaCompatibilityTest {

    @Test
    fun testFoliaDetection() {
        // 测试 Folia 检测
        val isFolia = FoliaUtils.isFolia
        println("Running on Folia: $isFolia")
    }

    @Test
    fun testRegionCheck() {
        // 测试区域检查逻辑
        // TODO: 实现具体测试
    }
}
```

---

### 5.2 集成测试清单

| 测试场景 | 测试方法 | 预期结果 |
|---------|---------|---------|
| **单区域玩家** | 1 个玩家在同一区域使用附魔 | ✅ 定时器正常触发 |
| **多区域玩家** | 10+ 玩家分散在不同区域 | ✅ 每个玩家独立工作 |
| **跨区域方块破坏** | 玩家在区块边界破坏方块 | ✅ 正确调度到目标区域 |
| **玩家数据保存** | 玩家退出时保存数据 | ✅ 数据不丢失 |
| **并发修改测试** | 多个玩家同时使用附魔 | ✅ 无 ConcurrentModificationException |
| **区域卸载** | 玩家离开导致区域卸载 | ✅ 任务正确取消 |
| **热重载** | 运行中重载配置 | ✅ 所有玩家任务重新调度 |

---

### 5.3 性能测试

创建文件: `project/performance-test.md`

```markdown
# 性能测试计划

## 测试环境
- 服务器: Paper vs Folia
- 玩家数量: 10, 50, 100, 200
- 附魔数量: 100+
- 定时器数量: 500+

## 测试指标
1. TPS (Ticks Per Second)
2. 内存使用
3. CPU 使用率
4. 区域线程数（Folia）
5. 事件处理延迟

## 基准测试结果记录

| 玩家数 | Paper TPS | Folia TPS | 内存 (Paper) | 内存 (Folia) |
|-------|-----------|-----------|-------------|-------------|
| 10    |           |           |             |             |
| 50    |           |           |             |             |
| 100   |           |           |             |             |
| 200   |           |           |             |             |
```

---

## 6. 修改进度追踪

### 6.1 进度表

| 编号 | 文件 | 优先级 | 状态 | 负责人 | 完成日期 |
|------|------|-------|------|--------|---------|
| 1 | DefaultAiyatsbusTickHandler.kt | P0 | ⬜ 待处理 | | |
| 2 | DefaultAiyatsbusPlayerDataHandler.kt | P0 | ⬜ 待处理 | | |
| 3 | ActionAwait.kt | P0 | ⬜ 待处理 | | |
| 4 | FastMultiBreak.kt | P0 | ⬜ 待处理 | | |
| 5 | Aiming.kt | P0 | ⬜ 待处理 | | |
| 6 | PickNearItems.kt | P0 | ⬜ 待处理 | | |
| 7 | Entities.kt | P0 | ⬜ 待处理 | | |
| 8 | FoliaUtils.kt (新建) | P0 | ⬜ 待处理 | | |
| 9 | DefaultMinecraftWorldOperator.kt | P0 | ⬜ 待处理 | | |
| 10 | DefaultAiyatsbusEventExecutor.kt | P1 | ⬜ 待处理 | | |
| 11 | DefaultAiyatsbusEnchantmentManager.kt | P1 | ⬜ 待处理 | | |
| 12 | SchedulerAdapter.kt (新建) | P0 | ⬜ 待处理 | | |
| 13 | Registry.kt | P2 | ⬜ 待处理 | | |

**状态图例**:
- ⬜ 待处理
- 🔄 进行中
- ✅ 已完成
- ⚠️ 需要审查
- ❌ 已阻塞

---

### 6.2 里程碑

| 里程碑 | 目标 | 预计完成 |
|-------|------|---------|
| **M1: 基础架构** | FoliaUtils + SchedulerAdapter 完成 | 第 1 周 |
| **M2: 核心调度器** | 所有调度器迁移完成 | 第 2 周 |
| **M3: 区域感知** | 实体/方块操作改造完成 | 第 3 周 |
| **M4: 测试和优化** | 所有测试通过 | 第 4 周 |
| **M5: 正式发布** | 发布 Folia 兼容版本 | 第 5 周 |

---

## 7. 风险和依赖

### 7.1 外部依赖

| 依赖 | 当前版本 | Folia 支持 | 备注 |
|------|---------|-----------|------|
| **TabooLib** | 6.2.3-test-18 | ❓ 未知 | 需要联系作者确认 |
| **Paper API** | 1.20.2 | ✅ 支持 | 需升级到支持 Folia 的版本 |
| **ItemsAdder** | - | ❓ 未知 | 第三方插件兼容性 |
| **Citizens** | - | ❓ 未知 | NPC 插件兼容性 |

### 7.2 风险评估

| 风险 | 严重性 | 概率 | 缓解措施 |
|------|-------|------|---------|
| TabooLib 不支持 Folia | 🔴 高 | 中 | 直接使用 Paper API |
| 第三方插件不兼容 | 🟡 中 | 高 | 添加兼容性检查 |
| 性能下降 | 🟡 中 | 低 | 性能测试和优化 |
| 数据丢失 | 🔴 高 | 低 | 加强测试和备份 |

---

## 8. 附录

### 8.1 参考命令

**搜索所有调度器使用**:
```bash
cd /Users/lynn/IdeaProjects/aiyatsbus
grep -rn "submit(" project/ | grep -v ".class"
grep -rn "@Schedule" project/ | grep -v ".class"
grep -rn "runTask" project/ | grep -v ".class"
```

**搜索所有 onlinePlayers 使用**:
```bash
grep -rn "onlinePlayers" project/ | grep -v ".class"
```

**搜索非线程安全数据结构**:
```bash
grep -rn "mutableMapOf\|HashMap\|HashSet" project/ | grep -v ".class"
```

---

### 8.2 快速参考

#### Folia API 对照表

| 旧 API (Paper/Spigot) | 新 API (Folia) |
|----------------------|---------------|
| `Bukkit.getScheduler().runTask()` | `entity.scheduler.run()` 或 `Bukkit.getGlobalRegionScheduler().run()` |
| `Bukkit.getScheduler().runTaskLater()` | `entity.scheduler.runDelayed()` |
| `Bukkit.getScheduler().runTaskTimer()` | `entity.scheduler.runAtFixedRate()` |
| `Bukkit.getScheduler().runTaskAsynchronously()` | `Bukkit.getAsyncScheduler().runNow()` |
| `isPrimaryThread()` | `Bukkit.isOwnedByCurrentRegion(entity/location)` |

---

### 8.3 联系方式

**需要支持时联系**:
- TabooLib 作者: [GitHub](https://github.com/TabooLib/taboolib)
- Paper 社区: [Discord](https://discord.gg/papermc)
- Folia 文档: [官方文档](https://docs.papermc.io/folia)

---

## 📝 文档更新日志

| 日期 | 版本 | 修改内容 | 作者 |
|------|------|---------|------|
| 2025-11-14 | 1.0 | 初始版本创建 | Claude |

---

**祝迁移顺利！如有问题请及时更新此文档。** 🚀
