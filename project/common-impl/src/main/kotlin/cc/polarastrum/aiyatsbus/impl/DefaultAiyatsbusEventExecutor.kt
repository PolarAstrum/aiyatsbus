package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.data.trigger.TriggerType
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventExecutor
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventMapping
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventResolver
import cc.polarastrum.aiyatsbus.core.event.AiyatsbusBowChargeEvent
import cc.polarastrum.aiyatsbus.core.event.AiyatsbusPrepareAnvilEvent
import cc.polarastrum.aiyatsbus.core.util.*
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Trident
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
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
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.unregisterListener
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import taboolib.platform.util.isAir
import taboolib.platform.util.killer
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

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

    private fun LivingEntity?.checkedIfIsNPC(): Pair1<LivingEntity?, Boolean> {
        if (checkIfIsNPC()) return this to1 false // 如果是 NPC 则不再进行处理事件
        return this to1 true
    }

    init {
        resolvers += Event::class.java to EventResolver<Event>(
            // 最后面返回 true 是因为这是最后一步直接解析, 如果这都解析不到那就没必要再重复一次解析了
            entityResolver = { event, playerReference ->
                event.invokeMethodDeep<LivingEntity?>(playerReference ?: return@EventResolver null to1 true)
                    .checkedIfIsNPC()
            },
            itemResolver = { event, itemReference, entity, slot ->
                val (item, itemResolved) = EventResolver.defaultItemResolver(entity, slot)
                if (!itemResolved || item.isNull) {
                    return@EventResolver event.invokeMethodDeep<ItemStack?>(
                        itemReference ?: return@EventResolver null to1 true
                    ) to1 true
                } else {
                    return@EventResolver item to1 true
                }
            }
        )
        resolvers += PlayerEvent::class.java to EventResolver<PlayerEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += PlayerMoveEvent::class.java to EventResolver<PlayerMoveEvent>(
            entityResolver = { event, _ -> event.player.checkedIfIsNPC() },
//            eventResolver = { event ->
//                /* 过滤视角转动 */
//                if (event.from.world == event.to.world && event.from.distance(event.to) < 1e-1) return@EventResolver
//            }
        )
        resolvers += BlockDropItemEvent::class.java to EventResolver<BlockDropItemEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += BlockDamageEvent::class.java to EventResolver<BlockDamageEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += BlockPlaceEvent::class.java to EventResolver<BlockPlaceEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += BlockBreakEvent::class.java to EventResolver<BlockBreakEvent>({ event, _ -> event.player.checkedIfIsNPC() })
        resolvers += ProjectileHitEvent::class.java to EventResolver<ProjectileHitEvent>(
            entityResolver = { event, _ -> (event.entity.shooter as? LivingEntity).checkedIfIsNPC() },
            itemResolver = { event, _, entity, slot ->
                if (event.entity is Trident) {
                    return@EventResolver (event.entity as Trident).item to1 true
                }
                return@EventResolver EventResolver.defaultItemResolver(entity, slot)
            }
        )
        resolvers += EntityDamageByEntityEvent::class.java to EventResolver<EntityDamageByEntityEvent>(
            entityResolver = { event, playerReference ->
                // 攻击者和受害者有任何一方是 NPC 就都不应触发此事件
                if (event.damager.checkIfIsNPC() || event.entity.checkIfIsNPC()) {
                    null to1 false
                }
                when (playerReference) {
                    "damager", null -> when (event.damager) {
                        is Player -> event.damager as? LivingEntity
                        is Projectile -> ((event.damager as Projectile).shooter as? LivingEntity)
                        else -> null
                    }.checkedIfIsNPC()

                    "entity" -> (event.entity as? LivingEntity).checkedIfIsNPC()
                    else -> null to1 false
                }
            },
            itemResolver = { event, _, entity, slot ->
                if (event.damager is Trident) {
                    return@EventResolver (event.damager as Trident).item to1 true
                }
                return@EventResolver EventResolver.defaultItemResolver(entity, slot)
            }
        )
        resolvers += EntityDeathEvent::class.java to EventResolver<EntityDeathEvent>({ event, _ -> event.killer.checkedIfIsNPC() })
        resolvers += EntityEvent::class.java to EventResolver<EntityEvent>({ event, _ -> (event.entity as? LivingEntity).checkedIfIsNPC() })
        resolvers += InventoryClickEvent::class.java to EventResolver<InventoryClickEvent>({ event, _ -> (event.whoClicked).checkedIfIsNPC() })
        resolvers += InventoryEvent::class.java to EventResolver<InventoryEvent>({ event, _ -> (event.view.player).checkedIfIsNPC() })
        resolvers += AiyatsbusPrepareAnvilEvent::class.java to EventResolver<AiyatsbusPrepareAnvilEvent>(
            entityResolver = { event, _ -> event.player.checkedIfIsNPC() },
            itemResolver = { event, itemReference, _, _ ->
                when (itemReference) {
                    "left" -> event.left to1 true
                    "right" -> event.right to1 true
                    "result" -> event.result to1 true
                    else -> null to1 false
                }
            }
        )
        resolvers += EnchantItemEvent::class.java to EventResolver<EnchantItemEvent>({ event, _ -> event.enchanter.checkedIfIsNPC() })
        resolvers += PrepareItemEnchantEvent::class.java to EventResolver<PrepareItemEnchantEvent>({ event, _ -> event.enchanter.checkedIfIsNPC() })
        resolvers += AiyatsbusBowChargeEvent.Prepare::class.java to EventResolver<AiyatsbusBowChargeEvent.Prepare>({ event, _ -> (event.player).checkedIfIsNPC() })
        resolvers += AiyatsbusBowChargeEvent.Released::class.java to EventResolver<AiyatsbusBowChargeEvent.Released>({ event, _ -> (event.player).checkedIfIsNPC() })
        resolvers += AiyatsbusBowChargeEvent.Break::class.java to EventResolver<AiyatsbusBowChargeEvent.Break>({ event, _ -> (event.player).checkedIfIsNPC() })
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
    override fun <T : Event> getResolver(instance: T): EventResolver<T>? {
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
                val (item, itemResolved) = resolver.itemResolver.apply(event, eventMapping.itemReference, entity, slot)
//                println("尝试锁定物品: $item")
                if (!itemResolved || item.isNull) return@forEach
//                println("锁定物品: $item")
                item!!.triggerEts(listen, event, entity, slot, false)
            }
        } else {
            var (item, itemResolved) = resolver.itemResolver.apply(event, eventMapping.itemReference, entity, null)
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
        val enchants = fastFixedEnchants
            .filter { (it[0] as AiyatsbusEnchantment).mechanism?.hasTrigger(TriggerType.LISTENER) == true }
            .sortedBy { (it[0] as AiyatsbusEnchantment).mechanism!!.priority(TriggerType.LISTENER) }
//        println("listen: " + listen)
//        println("event: " + event)
//        println("获取可触发物品附魔: " + enchants)
        for ((enchant, level) in enchants) {
            enchant as AiyatsbusEnchantment
            level as Int
//            println("遍历到附魔: " + enchant.id)
            val checkResult = enchant.limitations.checkAvailable(CheckType.USE, this, entity, slot, ignoreSlot)

            if (checkResult.isFailure) {
//                println("----- DefaultAiyatsbusEventExecutor -----")
//                println("附魔: " + enchant.basicData.name)
//                println("原因: " + checkResult.reason)
//                println("----- DefaultAiyatsbusEventExecutor -----")
                continue
            }

            enchant.mechanism!!.triggers(TriggerType.LISTENER)
                .filter { (it as EventExecutor).listen == listen }
                .sortedBy { it.priority }
                .forEach { executor ->
//                    println("执行事件: ${executor.id}")
                    val vars = hashMapOf(
                        "triggerSlot" to slot?.name,
                        "trigger-slot" to slot?.name,
                        "event" to event,
                        "player" to (entity as? Player ?: entity),
                        "item" to this,
                        "enchant" to enchant,
                        "level" to level,
                        "maxLevel" to enchant.basicData.maxLevel,
                        "container" to AiyatsbusContainer.getContainer(enchant),
                        "globalContainer" to AiyatsbusContainer.globalContainer
                    )

                    vars += enchant.variables.variables(level, this, false)

                    executor.executeHandle(entity, vars)
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
            reloadable {
                registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.EVENT_EXECUTORS) {
                    Aiyatsbus.api().getEventExecutor().destroyListeners()
                    Aiyatsbus.api().getEventExecutor().registerListeners()
                }
            }
        }

        @Awake(LifeCycle.ENABLE)
        fun onReload() {
            conf.onReload {
                measureTimeMillis {
                    Aiyatsbus.api().getEventExecutor().destroyListeners()
                    Aiyatsbus.api().getEventExecutor().registerListeners()
                }.let { console().sendLang("configuration-reload", conf.file!!.name, it) }
            }
        }

        @Awake(LifeCycle.DISABLE)
        fun onDisable() {
            Aiyatsbus.api().getEventExecutor().destroyListeners()
        }
    }
}