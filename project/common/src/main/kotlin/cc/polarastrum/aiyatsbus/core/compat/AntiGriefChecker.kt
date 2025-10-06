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
package cc.polarastrum.aiyatsbus.core.compat

import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.getProxyPlayer

/**
 * 反破坏检查器
 *
 * 负责检查玩家是否具有破坏、放置、交互等权限。
 * 支持多种反破坏插件的兼容性检查，如 WorldGuard、Factions 等。
 * 自动监听插件启用和禁用事件，动态管理检查器。
 *
 * @author mical
 * @since 2024/3/21 21:56
 */
object AntiGriefChecker {

    /** 已注册的检查器集合 */
    private val registeredChecker = hashSetOf<AntiGrief>()

    /** 缓存可用的检查器集合 */
    private val checkers = hashSetOf<AntiGrief>()

    /**
     * 检查玩家是否可以放置方块
     *
     * @param player 玩家
     * @param location 放置位置
     * @return 如果玩家可以放置则返回 true
     */
    fun canPlace(player: Player, location: Location): Boolean =
        checkPermission(player) { it.canPlace(player, location) }

    /**
     * 检查玩家是否可以破坏方块
     *
     * @param player 玩家
     * @param location 破坏位置
     * @return 如果玩家可以破坏则返回 true
     */
    fun canBreak(player: Player, location: Location): Boolean =
        checkPermission(player) {
//            with(getProxyPlayer("Y_Mical")) {
//                this?.sendMessage("插件名称: " + it.getAntiGriefPluginName())
//                this?.sendMessage("是否运行: " + it.checkRunning())
//                this?.sendMessage("检查结果: " + it.canBreak(player, location).toString())
//            }
            it.canBreak(player, location)
        }

    /**
     * 检查玩家是否可以与方块交互
     *
     * @param player 玩家
     * @param location 交互位置
     * @return 如果玩家可以交互则返回 true
     */
    fun canInteract(player: Player, location: Location): Boolean =
        checkPermission(player) { it.canInteract(player, location) }

    /**
     * 检查玩家是否可以与实体交互
     *
     * @param player 玩家
     * @param entity 交互实体
     * @return 如果玩家可以交互则返回 true
     */
    fun canInteractEntity(player: Player, entity: Entity): Boolean =
        checkPermission(player) { it.canInteractEntity(player, entity) }

    /**
     * 检查玩家是否可以伤害实体
     *
     * @param player 玩家
     * @param entity 目标实体
     * @return 如果玩家可以伤害则返回 true
     */
    fun canDamage(player: Player, entity: Entity): Boolean =
        checkPermission(player) { it.canDamage(player, entity) }

    /**
     * 注册新的兼容性检查器
     *
     * 将新的反破坏插件兼容性检查器注册到系统中。
     * 如果插件正在运行，则立即添加到可用检查器列表中。
     *
     * @param comp 要注册的兼容性检查器
     */
    fun registerNewCompatibility(comp: AntiGrief) {
        registeredChecker += comp

        if (comp.checkRunning()) {
            checkers += comp
        } // 这时候肯定可以读到了，先处理一次
    }

    /**
     * 插件启用事件监听器
     *
     * 当反破坏插件启用时，将对应的检查器添加到可用列表中。
     *
     * @param e 插件启用事件
     */
    @SubscribeEvent
    fun e(e: PluginEnableEvent) {
        val checker = registeredChecker
            .find { it.getAntiGriefPluginName() == e.plugin.name } ?: return
        checkers += checker
    }

    /**
     * 插件禁用事件监听器
     *
     * 当反破坏插件禁用时，从可用检查器列表中移除对应的检查器。
     *
     * @param e 插件禁用事件
     */
    @SubscribeEvent
    fun e(e: PluginDisableEvent) {
        checkers.removeAll {
            it.getAntiGriefPluginName() == e.plugin.name
        }
    }

    /**
     * 检查权限的通用方法
     *
     * 如果玩家是管理员且配置允许忽略管理员，则直接返回 true。
     * 否则检查所有可用的检查器，只有全部通过才返回 true。
     *
     * @param player 玩家
     * @param action 要执行的检查动作
     * @return 检查结果
     */
    private inline fun checkPermission(player: Player, action: (AntiGrief) -> Boolean): Boolean {
        if (player.isOp && AiyatsbusSettings.antiGriefIgnoreOp) return true
        return checkers.all { action(it) }
    }
}