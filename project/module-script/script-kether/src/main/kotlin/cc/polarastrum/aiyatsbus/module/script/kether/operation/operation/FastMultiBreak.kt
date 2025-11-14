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
package cc.polarastrum.aiyatsbus.module.script.kether.operation.operation

import cc.polarastrum.aiyatsbus.core.compat.AntiGriefChecker
import cc.polarastrum.aiyatsbus.core.util.coerceInt
import cc.polarastrum.aiyatsbus.core.util.doBreakBlock
import cc.polarastrum.aiyatsbus.core.util.serialized
import org.bukkit.Location
import org.bukkit.entity.Player
import taboolib.common.platform.function.warning
import taboolib.platform.util.submit

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.action.operation.FastMultiBreak
 *
 * @author mical
 * @since 2024/3/10 15:34
 */
object FastMultiBreak {

    @Suppress("UNCHECKED_CAST")
    fun fastMultiBreak(args: List<Any?>?) {
        fastMultiBreak(args?.get(0) as Player, (args[1] as List<Location>).toMutableList(), args[2].coerceInt())
    }

    private fun fastMultiBreak(player: Player, breaks: MutableList<Location>, speed: Int) {
        player.submit(delay = 0, period = 1) {
            for (i in 0 until speed) {

                val loc = breaks.firstOrNull()
                if (loc == null) {
                    cancel()
                    continue
                }

                val block = loc.block
                breaks -= loc

                if (block.location.world != player.world) {
                    warning("检测到玩家 ${player.name} 使用附魔破坏了一个不在同一世界的方块，可能是作弊行为或是极端情况下的极小概率行为，已忽略该破坏")
                    warning("|- 方块信息: ${block.location.serialized}")
                    continue
                }

                if (!AntiGriefChecker.canBreak(player, block.location)) continue
                player.doBreakBlock(block)
            }
        }
    }
}