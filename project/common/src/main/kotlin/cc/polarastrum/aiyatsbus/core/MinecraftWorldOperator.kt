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

import org.bukkit.block.Block
import org.bukkit.entity.Player

/**
 * Minecraft 世界操作器接口
 *
 * 提供世界相关的底层操作功能，
 * 包括方块破坏等跨版本兼容的世界交互操作。
 *
 * @author mical
 * @since 2025/8/16 08:44
 */
interface MinecraftWorldOperator {

    /**
     * 破坏方块
     *
     * 取代高版本 player.breakBlock 的函数，会触发 BlockBreakEvent。
     *
     * @param player 玩家
     * @param block 方块
     * @return 是否成功破坏
     */
    fun breakBlock(player: Player, block: Block): Boolean
}