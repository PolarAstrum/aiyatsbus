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
package cc.polarastrum.aiyatsbus.impl.nms

import cc.polarastrum.aiyatsbus.core.MinecraftWorldOperator
import net.minecraft.core.BlockPosition
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Player

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftWorldOperator
 *
 * @author mical
 * @since 2025/8/16 08:52
 */
class DefaultMinecraftWorldOperator : MinecraftWorldOperator {

    override fun breakBlock(player: Player, block: Block): Boolean {
        return (player as CraftPlayer).handle.gameMode.destroyBlock(BlockPosition(block.x, block.y, block.z))
    }
}