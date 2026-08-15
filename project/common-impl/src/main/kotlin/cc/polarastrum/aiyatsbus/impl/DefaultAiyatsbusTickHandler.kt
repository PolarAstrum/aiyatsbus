package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.data.trigger.TriggerType
import cc.polarastrum.aiyatsbus.core.data.trigger.ticker.Ticker
import cc.polarastrum.aiyatsbus.core.util.isNull
import cc.polarastrum.aiyatsbus.core.util.reloadable
import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.submit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.DefaultAiyatsbusTickHandler
 *
 * @author mical
 * @since 2024/3/20 22:10
 */
class DefaultAiyatsbusTickHandler : AiyatsbusTickHandler {

    private var counter = 0
    private var task: PlatformExecutor.PlatformTask? = null
    private val routine: Table<AiyatsbusEnchantment, String, Long> = HashBasedTable.create()

    private val recorder = ConcurrentHashMap<UUID, MutableSet<String>>()

    override fun getRoutine(): Table<AiyatsbusEnchantment, String, Long> {
        return routine
    }

    override fun reset() {
        counter = 0
        task?.cancel()
        task = null
        routine.clear() // 清空等待重新加载
    }

    override fun start() {
        if (task != null) reset()

        task = submit(period = 1L) {
            onTick()
        }
    }

    private fun onTick() {
        // 这里无需判断附魔是否有机制, 也无需判断是否有 Ticker
        // 因为只有 Ticker 初始化时才会往这里扔附魔
        routine.cellSet()
            .filter { counter % it.value == 0L }
            .sortedBy { it.rowKey.mechanism!!.priority(TriggerType.TICKER) }
            .forEach {
                val ench = it.rowKey
                val id = it.columnKey
                val slots = ench.targets.flatMap { it.activeSlots }.toSet()

                onlinePlayers.forEach { player ->
                    player.submit {
                        var flag = false
                        val record = recorder.computeIfAbsent(player.uniqueId) { mutableSetOf() }

                        // 一般能存在 routine 里的, trigger 和 tickers 必不为 null
                        val ticker =
                            (ench.mechanism!!.triggers(TriggerType.TICKER).firstOrNull { t -> t.id == id }
                                ?: error("Unknown ticker $id for enchantment ${ench.basicData.id}")) as Ticker

                        val variables = hashMapOf(
                            "player" to player,
                            "enchant" to ench,
                            "maxLevel" to ench.basicData.maxLevel,
                            "container" to AiyatsbusContainer.getContainer(ench),
                            "globalContainer" to AiyatsbusContainer.globalContainer
                        )

                        variables += ench.variables.ordinary

                        slots.forEach slot@{ slot ->
                            val item: ItemStack
                            try {
                                item = player.inventory.getItem(slot)
                            } catch (_: Throwable) {
                                // 离谱的低版本报错:
                                // java.lang.NullPointerException: player.inventory.getItem(slot) must not be null
                                return@slot
                            }
                            if (item.isNull) return@slot

                            val level = item.fastEtLevel(ench)

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

                                val vars = HashMap(variables)
                                vars += mapOf(
                                    "triggerSlot" to slot.name,
                                    "trigger-slot" to slot.name,
                                    "item" to item,
                                    "level" to level,
                                )

                                vars += ench.variables.variables(level, item, false)

                                if (!record.contains(id)) {
                                    record += id
                                    ticker.executePreHandle(player, vars)
                                }

                                ticker.executeHandle(player, vars)
                            }
                        }
                        if (!flag && record.contains(id)) {
                            record -= id
                            ticker.executePostHandle(player, variables)
                        }
                    }
                }
            }
        counter++
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusTickHandler>(DefaultAiyatsbusTickHandler())
            reloadable {
                registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.TICKERS) {
                    Aiyatsbus.api().getTickHandler().reset()
                    Aiyatsbus.api().getTickHandler().start()
                }
            }
        }

        @Awake(LifeCycle.DISABLE)
        fun onDisable() {
            Aiyatsbus.api().getTickHandler().reset()
        }
    }
}