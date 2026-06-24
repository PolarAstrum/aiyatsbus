package cc.polarastrum.aiyatsbus.core.data.trigger.builtin

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.data.trigger.Trigger
import cc.polarastrum.aiyatsbus.core.data.trigger.TriggerType
import cc.polarastrum.aiyatsbus.core.fastFixedEnchants
import cc.polarastrum.aiyatsbus.core.script.ScriptType
import cc.polarastrum.aiyatsbus.core.util.camelToSnake
import cc.polarastrum.aiyatsbus.core.util.isNull
import cc.polarastrum.aiyatsbus.core.util.random
import org.bukkit.Material
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common5.Baffle
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.submit
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 详见 NereusOpus 的 EventExecutor 和 PublicTasks 类
 *
 * @author mical
 * @since 2026/3/8 23:59
 */
open class Builtin internal constructor(
    val enchant: AiyatsbusEnchantment,
    section: ConfigurationSection = Configuration.empty(),
    type: TriggerType = TriggerType.BUILTIN
) : Trigger(section, enchant, ScriptType.FLUXON, "", 0, type), EventFunctions {

    constructor(enchant: AiyatsbusEnchantment) : this(enchant, Configuration.empty(), TriggerType.BUILTIN)

    fun call(level: Int, type: EventType, entity: LivingEntity, event: Event) {
        executors[type]?.invoke(this, level, event)
        executors[EventType.TRIGGER]?.invoke(this, level, type, event, entity)
    }

    fun call(level: Int, slot: EquipmentSlot, player: Player, type: EventType) {
        executors[type]?.invoke(this, level, slot, player, stamp)
        executors[EventType.TRIGGER]?.invoke(this, level, type, null, player)
    }

    override fun init() {
    }

    override fun preheat() {
    }

    override fun executeHandle(entity: LivingEntity, vars: MutableMap<String, Any?>) {
    }

    companion object {

        private val baffle = Baffle.of(150, TimeUnit.MILLISECONDS)

        val executors = ConcurrentHashMap<EventType, Method>()

        private var stamp = 0

        @Awake(LifeCycle.LOAD)
        fun init() {
            executors.putAll(EventFunctions::class.java.declaredMethods.associateBy { method ->
                EventType.valueOf(method.name.camelToSnake()) }
            )
            submit(period = 20) {
                stamp += 20
                onlinePlayers.forEach {
                    it.submit {
                        execute(it, EventType.TICK_TASK, null, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
                    }
                }
            }
        }

        fun execute(entity: Entity, type: EventType, event: Event?, vararg slots: EquipmentSlot) {
            if (entity !is LivingEntity) return
            for (slot in slots) {
                val item = entity.equipment?.getItem(slot)
                if (item.isNull) continue
                execute(entity, item!!, type, event, slot)
            }
        }

        /**
         * 适用于三叉戟这种触发时物品不在手上的附魔
         */
        fun execute(entity: Entity, type: EventType, event: Event?, item: ItemStack, vararg slots: EquipmentSlot) {
            if (entity !is LivingEntity) return
            for (slot in slots) {
                execute(entity, item, type, event, slot)
            }
        }

        fun execute(entity: Entity, item: ItemStack, type: EventType, event: Event?, slot: EquipmentSlot) {
            entity as LivingEntity
            if (item.fastFixedEnchants.isEmpty()) return
//            if (checkEvents.contains(event)) return
            if (entity is Player) {
                if ((type == EventType.BLOCK_BREAK || type == EventType.DAMAGED_BY_ENTITY) && "HOE" !in item.type.name) {
                    if (!baffle.hasNext(entity.uniqueId.toString())) return
                    baffle.next(entity.uniqueId.toString())
                }
            }
            if ((item.type == Material.BOW || item.type == Material.CROSSBOW) && event is EntityDamageByEntityEvent) {
                if (event.damager !is AbstractArrow) return
            }
            for ((enchant, level) in item.fastFixedEnchants) {
                enchant as AiyatsbusEnchantment
                level as Int
                val mechanism = enchant.mechanism ?: continue
                val builtins = mechanism.triggersOfType<Builtin>(TriggerType.BUILTIN) +
                    mechanism.triggersOfType<Builtin>(TriggerType.ARTIFACT)
                if (builtins.isEmpty()) continue
                val chance = when {
                    enchant.variables.leveled.contains("chance") -> enchant.variables.leveled("chance", level, false) as Double
                    enchant.variables.leveled.contains("概率") -> enchant.variables.leveled("概率", level, false) as Double
                    else -> 100.0
                }
                if (enchant.basicData.enable &&
                    random(chance) &&
                    enchant.limitations.checkAvailable(CheckType.USE, item, entity, slot).isSuccess
                ) {
                    for (builtin in builtins) {
                        if (event != null) {
                            builtin.call(level, type, entity, event)
                        } else {
                            builtin.call(level, slot, entity as Player, type)
                        }
                    }
                }
            }
        }
    }
}
