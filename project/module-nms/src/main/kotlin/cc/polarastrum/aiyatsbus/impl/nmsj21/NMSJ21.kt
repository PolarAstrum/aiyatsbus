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
package cc.polarastrum.aiyatsbus.impl.nmsj21

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.util.unsafeLazy
import taboolib.module.nms.nmsProxy

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.nms.nms12005.NMS12005
 *
 * @author mical
 * @since 2024/5/5 20:20
 */
abstract class NMSJ21 {

    abstract fun getRepairCost(item: ItemStack): Int

    abstract fun setRepairCost(item: ItemStack, cost: Int) : ItemStack

    abstract fun adaptMerchantRecipe(merchantRecipeList: Any, player: Player)

    abstract fun hurtAndBreak(nmsItem: Any, amount: Int, entity: LivingEntity)

    companion object {

        val instance by unsafeLazy { nmsProxy<NMSJ21>() }
    }
}