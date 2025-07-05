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
package cc.polarastrum.aiyatsbus.core.data.trigger.event

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.script.ScriptType
import org.bukkit.command.CommandSender
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.kether.LocalizedException

/**
 * 事件执行器类
 *
 * 定义事件触发时的脚本执行逻辑，支持不同脚本类型和优先级设置。
 * 负责在特定事件发生时执行相应的脚本代码。
 *
 * @author mical
 * @since 2024/3/9 18:35
 */
data class EventExecutor @JvmOverloads constructor(
    /** 配置根节点 */
    private val root: ConfigurationSection,
    /** 所属附魔 */
    private val enchant: AiyatsbusEnchantment,
    /** 脚本类型，默认为 KETHER */
    val scriptType: ScriptType = ScriptType.valueOf(root.getString("type")?.uppercase() ?: "KETHER"),
    /** 监听的事件类型 */
    val listen: String = root.getString("listen")!!,
    /** 事件处理脚本 */
    val handle: String = root.getString("handle") ?: "",
    /** 执行优先级，默认为 0 */
    val priority: Int = root.getInt("priority", 0)
) {

    init {
        if (AiyatsbusSettings.enableKetherPreheat) {
            try {
                Aiyatsbus.api().getScriptHandler().getScriptHandler(scriptType).preheat(handle)
            } catch (ex: LocalizedException) {
                warning("Unable to preheat the event executor ${root.name} of enchantment ${enchant.id}: $ex")
            }
        }
    }

    /**
     * 执行事件脚本
     *
     * 使用指定的脚本处理器执行事件处理脚本。
     *
     * @param sender 命令发送者
     * @param vars 变量映射表
     * 
     * @example
     * ```kotlin
     * executor.execute(player, mapOf("level" to 5, "damage" to 10.0))
     * ```
     */
    fun execute(sender: CommandSender, vars: Map<String, Any?>) {
        Aiyatsbus.api().getScriptHandler().getScriptHandler(scriptType)
            .invoke(handle, sender, vars)
    }
}