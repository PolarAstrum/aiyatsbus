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
import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.script.ScriptType
import org.bukkit.command.CommandSender
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.kether.LocalizedException

/**
 * 定时器触发器类
 *
 * 定义定时执行的脚本触发器，支持预处理、主处理和后处理脚本。
 * 可以按指定间隔执行脚本，用于实现持续性的附魔效果。
 *
 * @author mical
 * @since 2024/3/20 22:28
 */
data class Ticker @JvmOverloads constructor(
    /** 配置根节点 */
    private val root: ConfigurationSection,
    /** 所属附魔 */
    private val enchant: AiyatsbusEnchantment,
    /** 脚本类型，默认为 KETHER */
    val scriptType: ScriptType = ScriptType.valueOf(root.getString("type")?.uppercase() ?: "KETHER"),
    /** 预处理脚本，在定时器启动前执行 */
    val preHandle: String = root.getString("pre-handle") ?: "",
    /** 主处理脚本，定时执行的主要逻辑 */
    val handle: String = root.getString("handle") ?: "",
    /** 后处理脚本，在定时器停止后执行 */
    val postHandle: String = root.getString("post-handle") ?: "",
    /** 执行间隔（tick），默认为 20（1 秒） */
    val interval: Long = root.getLong("interval", 20L)
) {

    init {
        if (AiyatsbusSettings.enableKetherPreheat) {
            try {
                with(Aiyatsbus.api().getScriptHandler().getScriptHandler(scriptType)) {
                    preheat(preHandle)
                    preheat(handle)
                    preheat(postHandle)
                }
            } catch (ex: LocalizedException) {
                warning("Unable to preheat the ticker ${root.name} of enchantment ${enchant.id}: $ex")
            }
        }
    }

    /**
     * 执行脚本
     *
     * 使用指定的脚本处理器执行脚本。
     *
     * @param source 脚本内容
     * @param sender 命令发送者
     * @param vars 变量映射表
     * 
     * @example
     * ```kotlin
     * ticker.execute("say Hello", player, mapOf("level" to 5))
     * ```
     */
    fun execute(source: String, sender: CommandSender, vars: Map<String, Any>) {
        Aiyatsbus.api().getScriptHandler().getScriptHandler(scriptType)
            .invoke(source, sender, vars)
    }
}