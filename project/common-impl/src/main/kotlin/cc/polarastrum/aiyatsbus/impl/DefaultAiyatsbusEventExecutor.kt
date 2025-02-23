/*
 *  Copyright (C) 2022-2024 PolarAstrumLab
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cc.polarastrum.aiyatsbus.impl

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventMapping
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventResolver
import cc.polarastrum.aiyatsbus.core.event.AiyatsbusBowChargeEvent
import cc.polarastrum.aiyatsbus.core.event.AiyatsbusPrepareAnvilEvent
import cc.polarastrum.aiyatsbus.core.util.*
import cc.polarastrum.aiyatsbus.core.util.inject.Reloadable
import cc.polarastrum.aiyatsbus.core.util.inject.AwakePriority
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import taboolib.platform.util.isAir
import taboolib.platform.util.killer
import java.util.concurrent.ConcurrentHashMap

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.DefaultAiyatsbusEventExecutor
 *
 * @author mical
 * @since 2024/7/18 00:45
 */
class DefaultAiyatsbusEventExecutor : AiyatsbusEventExecutor {

    private val resolvers = ConcurrentHashMap<Class<out Event>, EventResolver<*>>()

    private val externalMappings: ConcurrentHashMap<String, EventMapping> = ConcurrentHashMap()

    private val listeners: Table<String, EventPriority, ProxyListener> = HashBasedTable.create()

    private val cachedClasses = ConcurrentHashMap<String, Class<*>>()

    private fun LivingEntity?.checkedIfIsNPC(): Pair<LivingEntity?, Boolean> {
        if (checkIfIsNPC()) return this to false // 如果是 NPC 则不再进行处理事件
        return this to true
    }

    init {
        resolvers += Event::class.java to EventResolver<Event>(
            // 最后面返回 true 是因为这是最后一步直接解析, 如果这都解析不到那就没必要再重复一次解析了
            entityResolver = { event, playerReference ->
                event.invokeMethodDeep<LivingEntity?>(playerReference ?: return@EventResolver null to true).checkedIfIsNPC()
            },
            itemResolver = { event, itemReference, _ -> event.invokeMethodDeep<ItemStack?>(itemReference ?: return@EventResolver null to true) to true }
        )
        resolvers += PlayerEvent::class.java to EventResolver<PlayerEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += PlayerMoveEvent::class.java to EventResolver<PlayerMoveEvent>(
            entityResolver = { event, _ -> event.player.checkedIfIsNPC() },
            eventResolver = { event ->
                /* 过滤视角转动 */
                if (event.from.world == event.to.world && event.from.distance(event.to) < 1e-1) return@EventResolver
            }
        )
        resolvers += BlockDropItemEvent::class.java to EventResolver<BlockDropItemEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += BlockDamageEvent::class.java to EventResolver<BlockDamageEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += BlockPlaceEvent::class.java to EventResolver<BlockPlaceEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += BlockBreakEvent::class.java to EventResolver<BlockBreakEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += ProjectileHitEvent::class.java to EventResolver<ProjectileHitEvent>({ event, _ -> (event.entity.shooter as? LivingEntity).checkedIfIsNPC() })
        resolvers += EntityDamageByEntityEvent::class.java to EventResolver<EntityDamageByEntityEvent>({ event, playerReference ->
            // 攻击者和受害者有任何一方是 NPC 就都不应触发此事件
            if (event.damager.checkIfIsNPC() || event.entity.checkIfIsNPC()) {
                null to false
            }
            when (playerReference) {
                "damager", null -> when (event.damager) {
                    is Player -> event.damager as? LivingEntity
                    is Projectile -> ((event.damager as Projectile).shooter as? LivingEntity)
                    else -> null
                }.checkedIfIsNPC()
                "entity" -> (event.entity as? LivingEntity).checkedIfIsNPC()
                else -> null to false
            }
        })
        resolvers += EntityDeathEvent::class.java to EventResolver<EntityDeathEvent>({ event, _ -> event.killer.checkedIfIsNPC() })
        resolvers += EntityEvent::class.java to EventResolver<EntityEvent>({ event, _ -> (event.entity as? LivingEntity).checkedIfIsNPC() })
        resolvers += InventoryClickEvent::class.java to EventResolver<InventoryClickEvent>({ event, _ -> (event.whoClicked).checkedIfIsNPC() })
        resolvers += InventoryEvent::class.java to EventResolver<InventoryEvent>({ event, _ -> (event.view.player).checkedIfIsNPC() })
        resolvers += AiyatsbusPrepareAnvilEvent::class.java to EventResolver<AiyatsbusPrepareAnvilEvent>(
            entityResolver = { event, _ -> event.player.checkedIfIsNPC() },
            itemResolver = { event, itemReference, _ ->
                when (itemReference) {
                    "left" -> event.left to true
                    "right" -> event.right to true
                    "result" -> event.result to true
                    else -> null to false
                }
            }
        )
        resolvers += AiyatsbusBowChargeEvent.Prepare::class.java to EventResolver<AiyatsbusBowChargeEvent.Prepare>({ event, _ -> (event.player).checkedIfIsNPC() })
        resolvers += AiyatsbusBowChargeEvent.Released::class.java to EventResolver<AiyatsbusBowChargeEvent.Released>({ event, _ -> (event.player).checkedIfIsNPC() })
    }

    override fun registerListener(listen: String, eventMapping: EventMapping) {
        val (className, _, _, _, priority, ignoreCancelled) = eventMapping
        // 缓存
        try {
            val clazz = cachedClasses.computeIfAbsent(className) { Class.forName(className) }
            listeners.put(listen, priority, registerBukkitListener(clazz, priority, ignoreCancelled) {
                processEvent(listen, it as? Event ?: return@registerBukkitListener, eventMapping, priority)
            })
        } catch (_: ClassNotFoundException) {
        }
    }

    override fun registerListeners() {
        mappings.forEach(::registerListener)
        externalMappings.forEach(::registerListener)
    }

    override fun destroyListener(listen: String) {
        listeners.row(listen).values.forEach { unregisterListener(it) }
        listeners.row(listen).clear()
    }

    override fun destroyListeners() {
        listeners.values().forEach { unregisterListener(it) }
        listeners.clear()
    }

    override fun getEventMappings(): Map<String, EventMapping> {
        return mappings
    }

    override fun getExternalEventMappings(): MutableMap<String, EventMapping> {
        return externalMappings
    }

    override fun getResolvers(): MutableMap<Class<out Event>, EventResolver<*>> {
        return resolvers
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Event> getResolver(instance: T): EventResolver<T>? {
        var currentClass: Class<*>? = instance::class.java
        while (currentClass != null) {
            val resolver = resolvers[currentClass] as? EventResolver<T>
            if (resolver != null) {
                return resolver
            }
            currentClass = currentClass.superclass
        }
        return null
    }

    private fun processEvent(listen: String, event: Event, eventMapping: EventMapping, eventPriority: EventPriority) {
        // NOTICE 不要删除下面的调试信息, 关键时刻能救命
//        println("我是 $listen, 我的优先级是 ${eventPriority.name}")
        val resolver = getResolver(event) ?: return
//        println("我的事件处理器是 $resolver")
        /* 特殊事件处理 */
        resolver.eventResolver.apply(event)

        var (entity, entityResolved) = resolver.entityResolver.apply(event, eventMapping.playerReference)

        if (entity == null && !entityResolved) {
            entity = event.invokeMethodDeep<LivingEntity>(eventMapping.playerReference ?: return) ?: return
        }
//        println("尝试锁定生物: $entity")
        if (entity == null) return
//        println("锁定生物: $entity")

        if (entity.checkIfIsNPC()) return
//        println("NPC 检测通过")

        if (eventMapping.slots.isNotEmpty()) {
            eventMapping.slots.forEach { slot ->
//                println("检测槽位: $slot")
                val item: ItemStack?
                try {
                    item = entity.equipment?.getItem(slot)
                } catch (_: Throwable) {
                    // 离谱的低版本报错:
                    // java.lang.NullPointerException: player.inventory.getItem(slot) must not be null
                    return@forEach
                }
//                println("尝试锁定物品: $item")

                if (item.isNull) return@forEach

//                println("锁定物品: $item")

                item!!.triggerEts(listen, event, entity, slot, false)
            }
        } else {
            var (item, itemResolved) = resolver.itemResolver.apply(event, eventMapping.itemReference, entity)
//            println("我尝试使用 resolver 获取物品, 结果是: $item")
            if (item.isAir && !itemResolved) {
                item = event.invokeMethodDeep(eventMapping.itemReference ?: return) as? ItemStack ?: return
//                println("我用反射获取到了物品: $item")
            }
            if (item.isNull) return
            item!!.triggerEts(listen, event, entity, null, true)
        }
    }

    private fun ItemStack.triggerEts(listen: String, event: Event, entity: LivingEntity, slot: EquipmentSlot?, ignoreSlot: Boolean = false) {

        val enchants = fixedEnchants.entries
            .filter { it.key.trigger != null }
            .sortedBy { it.key.trigger!!.listenerPriority }

        for (enchantPair in enchants) {
            val enchant = enchantPair.key

            if (enchant.limitations.checkAvailable(CheckType.USE, this, entity, slot, ignoreSlot).isFailure) continue

            enchant.trigger!!.listeners
                .filterValues { it.listen == listen }
                .entries
                .sortedBy { it.value.priority }
                .forEach { (_, executor) ->
//                    println("执行事件: $executor")
                    val vars = mutableMapOf(
                        "triggerSlot" to slot?.name,
                        "trigger-slot" to slot?.name,
                        "event" to event,
                        "player" to (entity as? Player ?: entity),
                        "item" to this,
                        "enchant" to enchant,
                        "level" to enchantPair.value,
                        "mirror" to Mirror.MirrorStatus()
                    )

                    vars += enchant.variables.variables(enchantPair.value, this, false)

                    if (AiyatsbusSettings.enablePerformanceTool) {
                        mirrorNow("Enchantment:Listener:Kether" + if (AiyatsbusSettings.showPerformanceDetails) ":${enchant.basicData.id}" else "") {
                            vars += "mirror" to it
                            Aiyatsbus.api().getKetherHandler().invoke(executor.handle, entity, variables = vars)
                        }
                    } else {
                        Aiyatsbus.api().getKetherHandler().invoke(executor.handle, entity, variables = vars)
                    }
                }
        }
    }

    companion object {

        @Config(value = "core/event-mapping.yml", autoReload = true)
        private lateinit var conf: Configuration

        @delegate:ConfigNode("mappings", bind = "core/event-mapping.yml")
        private val mappings: MutableMap<String, EventMapping> by conversion<ConfigurationSection, MutableMap<String, EventMapping>> {
            getKeys(false).associateWith { EventMapping(conf.getConfigurationSection("mappings.$it")!!) }.toMutableMap()
        }

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusEventExecutor>(DefaultAiyatsbusEventExecutor())
        }

        @Reloadable
        @AwakePriority(LifeCycle.ENABLE, StandardPriorities.EVENT_EXECUTORS)
        fun onEnable() {
            Aiyatsbus.api().getEventExecutor().destroyListeners()
            Aiyatsbus.api().getEventExecutor().registerListeners()
        }

        @Awake(LifeCycle.ENABLE)
        fun onReload() {
            conf.onReload {
                Aiyatsbus.api().getEventExecutor().destroyListeners()
                Aiyatsbus.api().getEventExecutor().registerListeners()
            }
        }

        @Awake(LifeCycle.DISABLE)
        fun onDisable() {
            Aiyatsbus.api().getEventExecutor().destroyListeners()
        }
    }
}