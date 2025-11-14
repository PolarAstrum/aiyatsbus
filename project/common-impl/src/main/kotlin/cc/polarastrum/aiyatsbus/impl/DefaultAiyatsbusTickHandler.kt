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
import cc.polarastrum.aiyatsbus.core.util.isNull
import cc.polarastrum.aiyatsbus.core.util.reloadable
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.submit
import java.util.UUID
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
        routine.cellSet() // 无需判断这里 trigger 是否为 null, 因为只有 Trigger 初始化时才会往这里扔 enchant
            .filter { counter % it.value == 0L }
            .sortedBy { it.rowKey.trigger!!.tickerPriority }
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
                            ench.trigger!!.tickers[id] ?: error("Unknown ticker $id for enchantment ${ench.basicData.id}")

                        val variables = mutableMapOf(
                            "player" to player,
                            "enchant" to ench,
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