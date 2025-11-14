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

import cc.polarastrum.aiyatsbus.core.compat.GuardItemChecker
import cc.polarastrum.aiyatsbus.core.compat.GuardItemChecker.Companion.calculateItemCapacity
import cc.polarastrum.aiyatsbus.core.util.coerceInt
import cc.polarastrum.aiyatsbus.core.util.coerceLong
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.platform.util.submit

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.operation.operation.PickupItem
 *
 * @author mical
 * @since 2024/8/19 10:22
 */
object PickNearItems {

    fun pickNearItems(args: List<Any?>?) {
        pickNearItems(
            args?.get(0) as Player,
            args[1] as Location,
            args[2].coerceInt(),
            args[3].coerceLong()
        )
    }

    fun pickNearItems(player: Player, location: Location, checkRadius: Int, checkDelay: Long) {
        player.submit(delay = checkDelay) {
            for (item in location.getNearbyEntitiesByType(Item::class.java, checkRadius.toDouble())) {
                if (GuardItemChecker.checkIsGuardItem(item, player)) continue
                if (!item.isDead && canFitItem(player, item.itemStack)) {
                    player.inventory.addItem(item.itemStack)
                    item.remove()
                }
            }
        }
    }

    private fun canFitItem(player: Player, item: ItemStack): Boolean {
        return calculateItemCapacity(player, item) >= item.amount
    }
}