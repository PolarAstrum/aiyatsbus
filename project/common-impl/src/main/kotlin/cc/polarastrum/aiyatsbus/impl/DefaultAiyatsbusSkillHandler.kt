package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.data.trigger.TriggerType
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventResolver
import cc.polarastrum.aiyatsbus.core.data.trigger.skill.ActionType
import cc.polarastrum.aiyatsbus.core.data.trigger.skill.Skill
import cc.polarastrum.aiyatsbus.core.util.*
import cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusSkillHandler.AiyatsbusSkillSettings.conf
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.unregisterListener
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import taboolib.platform.util.isLeftClick
import taboolib.platform.util.isMainhand
import taboolib.platform.util.isRightClick

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusSkillHandler
 *
 * @author mical
 * @since 2026/1/27 18:07
 */
class DefaultAiyatsbusSkillHandler : AiyatsbusSkillHandler {

    val listeners = hashSetOf<ProxyListener>()

    override fun getSettings(): AiyatsbusSkillHandler.Settings {
        return AiyatsbusSkillSettings
    }

    override fun registerEvents() {
        unregisterEvents()
        listeners.add(registerBukkitListener(PlayerInteractEvent::class.java, priority = EventPriority.HIGHEST, ignoreCancelled = false) {
            if (!it.isMainhand()) return@registerBukkitListener
            val type = when {
                it.isLeftClick() -> ActionType.LEFT_CLICK
                it.isRightClick() -> ActionType.RIGHT_CLICK
                else -> null
            }
            handleEvent(it, type ?: return@registerBukkitListener)
        })
        listeners.add(registerBukkitListener(PlayerSwapHandItemsEvent::class.java, priority = EventPriority.HIGHEST) {
            handleEvent(it, ActionType.SWAP)
        })
    }

    override fun unregisterEvents() {
        listeners.forEach { unregisterListener(it) }
        listeners.clear()
    }

    override fun handleEvent(e: PlayerEvent, type: ActionType) {
        if (e.player.checkIfIsNPC()) return
        // 检测过程参考 DefaultAiyatsbusEventExecutor
        val (item, resolved) = EventResolver.defaultItemResolver(e.player, EquipmentSlot.HAND)
        if (!resolved || item.isNull) return

        val enchants = item!!.fastFixedEnchants
            .filter { (it[0] as AiyatsbusEnchantment).mechanism?.hasTrigger(TriggerType.SKILL) == true }
            .sortedBy { (it[0] as AiyatsbusEnchantment).mechanism!!.priority(TriggerType.SKILL) }

        for ((enchant, level) in enchants) {
            enchant as AiyatsbusEnchantment
            level as Int
            if (enchant.limitations.checkAvailable(CheckType.USE, item, e.player, EquipmentSlot.HAND).isFailure) {
                continue
            }
            enchant.mechanism!!.triggers(TriggerType.SKILL)
                .filter { (it as Skill).getAction() == type }
                .sortedBy { it.priority }
                .forEach { skill ->
                    skill as Skill
                    if (!e.player.isSneaking && skill.isShiftNeeded()) return
                    if (e.player.isSneaking && skill.isShiftIgnored()) return
                    // TODO: Silence
                    val cooldown = AiyatsbusSkillSettings.calculateCooldown(enchant.variables.variable(skill.getCooldownVar(), level, item, false).coerceDouble(), e.player)
                    val (isReady, remainingSeconds) = e.player.checkCd(enchant.basicData.id, cooldown)
                    if (!isReady && skill.isEnableCooldown()) e.player.sendCooldownTip(remainingSeconds)
                    if (!isReady) return
                    e.player.addCd(enchant.basicData.id)
                    skill.playSound(e.player)
                    skill.spawnParticle(e.player)
                    skill.executeHandle(e.player, hashMapOf<String, Any?>(
                        "event" to e,
                        "player" to e.player,
                        "item" to item,
                        "enchant" to enchant,
                        "level" to level,
                        "cooldown" to cooldown,
                        "maxLevel" to enchant.basicData.maxLevel
                    ).apply { putAll(enchant.variables.variables(level, item, false)) })
                }
        }
    }

    @ConfigNode(bind = "enchants/skill.yml")
    object AiyatsbusSkillSettings : AiyatsbusSkillHandler.Settings {

        @Config("enchants/skill.yml", autoReload = true)
        override lateinit var conf: Configuration

        @ConfigNode("cooldown.enable")
        override var enableCooldown: Boolean = true

        @ConfigNode("cooldown.name")
        override var cooldownVar: String = "冷却"

        @delegate:ConfigNode("trigger.action")
        override val action: ActionType by conversion<String, ActionType> {
            ActionType.valueOf(this)
        }

        @ConfigNode("trigger.shift-needed")
        override var shiftNeeded: Boolean = false

        @ConfigNode("trigger.shift-ignored")
        override var shiftIgnored: Boolean = true

        @delegate:ConfigNode("privilege")
        override val privilege: Map<String, String> by conversion<List<String>, Map<String, String>> {
            mapOf(*map { it.split(":")[0] to it.split(":")[1] }.toTypedArray())
        }

        /**
         * TODO 显示还没做好
         */
        fun calculateCooldown(origin: Double, player: ProxyPlayer) = calculateCooldown(origin, player.origin as Player)

        /**
         * TODO 显示还没做好
         */
        fun calculateCooldown(origin: Double, player: Player) = privilege.minOf { (perm, expression) ->
            if (player.hasPermission(perm)) expression.calcToDouble("cooldown" to origin) else origin
        }.coerceAtLeast(0.0)
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusSkillHandler>(DefaultAiyatsbusSkillHandler())
            registerLifeCycleTask(LifeCycle.ENABLE) {
                conf.onReload {
                    console().sendLang("configuration-reload", conf.file!!.name, 0)
                }
            }
            registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.SKILL_HANDLER) {
                Aiyatsbus.api().getSkillHandler().registerEvents()
            }
        }

        @Awake(LifeCycle.DISABLE)
        fun onDisable() {
            Aiyatsbus.api().getSkillHandler().unregisterEvents()
        }
    }
}
