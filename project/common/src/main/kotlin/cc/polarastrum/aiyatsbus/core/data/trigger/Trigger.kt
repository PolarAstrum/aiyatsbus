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
package cc.polarastrum.aiyatsbus.core.data.trigger

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.data.trigger.event.EventExecutor
import cc.polarastrum.aiyatsbus.core.util.coerceInt
import taboolib.common.LifeCycle
import taboolib.common.TabooLib
import taboolib.common.platform.function.severe
import taboolib.common.util.t
import taboolib.library.configuration.ConfigurationSection
import java.util.concurrent.ConcurrentHashMap

/**
 * 触发器类
 *
 * 管理附魔的事件监听器和定时器，负责注册和注销各种触发器。
 * 支持事件监听器和定时器两种触发方式。
 *
 * @author mical
 * @since 2024/3/9 18:36
 */
data class Trigger(
    /** 配置节点 */
    private val section: ConfigurationSection?,
    /** 所属附魔 */
    private val enchant: AiyatsbusEnchantment,
    /** 定时器优先级，默认为 0 */
    val tickerPriority: Int = (section?.getString("tickerPriority")
        ?: section?.getString("ticker-priority")).coerceInt(0),
    /** 监听器优先级，默认为 0 */
    val listenerPriority: Int = (section?.getString("listenerPriority")
        ?: section?.getString("listener-priority")).coerceInt(0),
) {

    /** 事件监听器映射表 */
    val listeners: ConcurrentHashMap<String, EventExecutor> = ConcurrentHashMap()
    /** 定时器映射表 */
    val tickers: ConcurrentHashMap<String, Ticker> = ConcurrentHashMap()

    init {
        try {
            // 初始化事件监听器
            section?.getConfigurationSection("listeners")?.let { listenersSection ->
                listeners += listenersSection.getKeys(false)
                    .associateWith { EventExecutor(listenersSection.getConfigurationSection(it)!!, enchant) }
            }
            // 初始化定时器
            section?.getConfigurationSection("tickers")?.let { tickersSection ->
                tickers += tickersSection.getKeys(false)
                    .associateWith { Ticker(tickersSection.getConfigurationSection(it)!!, enchant) }
                    .mapKeys { "${enchant.basicData.id}.$it" }.also {
                        it.entries.forEach { (id, ticker) ->
                            Aiyatsbus.api().getTickHandler().getRoutine().put(enchant, id, ticker.interval)
                        }
                    }
            }
        } catch (ex: Throwable) {
            if (TabooLib.getCurrentLifeCycle() != LifeCycle.ACTIVE) {
                severe("""
                    无法初始化附魔触发器，为避免数据丢失，服务器将会被强制关闭！
                    Failed to initialize trigger. To avoid data loss, the server will be forced to shut down!
                """.t())
                ex.printStackTrace()
                Runtime.getRuntime().halt(-1)
            }
        }
    }

    /**
     * 禁用触发器
     *
     * 清理所有监听器和定时器，释放相关资源。
     * 
     * @example
     * ```kotlin
     * trigger.onDisable()
     * ```
     */
    fun onDisable() {
        listeners.clear()
        tickers.keys.forEach { Aiyatsbus.api().getTickHandler().getRoutine().remove(enchant, it) }
        tickers.clear()
    }
}