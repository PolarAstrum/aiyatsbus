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
package cc.polarastrum.aiyatsbus.core

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.util.ResettableLazy
import taboolib.common.util.resettableLazy
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import java.math.RoundingMode

/**
 * Aiyatsbus 系统设置
 *
 * 管理 Aiyatsbus 插件的所有配置选项。
 * 支持自动重载配置，提供丰富的自定义选项。
 *
 * @author mical
 * @since 2024/2/17 14:31
 */
@ConfigNode(bind = "core/config.yml")
object AiyatsbusSettings {

    /** 配置文件实例，支持自动重载 */
    @Config("core/config.yml", autoReload = true)
    lateinit var conf: Configuration
        private set

    @Awake(LifeCycle.ENABLE)
    private fun init() {
        conf.onReload {
            ResettableLazy.reset()
        }
    }

    /**
     * 是否在管理员登录游戏时发送鸣谢信息
     * 默认值：true
     */
    @ConfigNode("Settings.send-thank-messages")
    var sendThankMessages = true

    /**
     * 自动释放插件内置的附魔包
     * 当附魔文件夹为空时，自动释放默认附魔文件
     * 默认值：true
     */
    @ConfigNode("Settings.auto-release-enchants")
    var autoReleaseEnchants = true

    /**
     * 默认品质
     * 当附魔没有指定品质时使用的默认品质
     * 默认值："common"
     */
    @ConfigNode("Settings.default-rarity")
    var defaultRarity = "common"

    /**
     * 附魔主菜单返回按钮的指令
     * 点击返回按钮时执行的命令
     * 默认值："cd"
     */
    @ConfigNode("Settings.main-menu-back")
    var mainMenuBack = "cd"

    /**
     * 命令别称，方便迁移
     * 支持多个命令别名，用于兼容其他插件
     * 默认值：["spe", "splendidenchants", "nerous", "nos", "nereusopus"]
     */
    @ConfigNode("Settings.command-aliases")
    var commandAliases = listOf("spe", "splendidenchants", "nerous", "nos", "nereusopus")

    /**
     * 是否开启 ItemsAdder 支持
     * 启用对 ItemsAdder 插件的兼容支持
     * 默认值：false
     */
    @ConfigNode("Settings.support-items-adder")
    var supportItemsAdder = false

    /**
     * 是否开启脚本预热
     * 启用 Kether 脚本的预热功能，提高性能
     * 默认值：true
     */
    @ConfigNode("Settings.enable-kether-preheat")
    var enableKetherPreheat = true

    /**
     * 对于附魔的挖掘放置攻击生物等操作，OP 是否无视领地等保护
     * 管理员是否绕过领地保护等限制
     * 默认值：true
     */
    @ConfigNode("Settings.anti-grief-ignore-op")
    var antiGriefIgnoreOp = true

    /**
     * 若没有特殊设置，将冷却消息显示在动作栏而不是聊天框
     * 冷却提示的显示位置设置
     * 默认值：false
     */
    @ConfigNode("Settings.cool-down-in-actionbar")
    var coolDownInActionBar = false

    /**
     * 参数数值保留小数点后几位数字
     * 变量数值的精度设置
     * 默认值：2
     */
    @ConfigNode("Settings.variable-rounding-scale")
    var variableRoundingScale = 2

    /**
     * 参数数值的舍位模式
     * 变量数值的舍入方式设置
     * 默认值：RoundingMode.HALF_UP
     */
    @delegate:ConfigNode("Settings.variable-rounding-mode")
    val variableRoundingMode by conversion<String, RoundingMode> { RoundingMode.valueOf(this) }

    /**
     * 是否启用平衡性自动更新的功能
     * 启用配置文件自动更新功能
     * 默认值：true
     */
    @ConfigNode("Settings.updater.enable")
    var enableUpdater = true

    /**
     * 附魔平衡性+语言修正调整自动获取官方最新
     * 需要自动更新的内容列表
     * 默认值：空列表
     */
    @ConfigNode("Settings.updater.contents")
    var updateContents = emptyList<String>()

    /**
     * 是否开启调试信息
     * 默认值: false
     */
    val debug by resettableLazy { conf.getBoolean("Settings.debug") }

    /**
     * 会收到调试信息的用户名列表
     * 默认值: ["Y_Mical", "TabooLib", "xiaozhangup", "HamsterYDS"]
     */
    val debugUsers by resettableLazy { conf.getStringList("Settings.debug-users") }
}