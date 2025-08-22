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

import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.trading.MerchantRecipeList
import org.bukkit.craftbukkit.v1_21_R4.entity.CraftLivingEntity
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_21_R4.inventory.CraftItemType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.setProperty
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.nms.v12005_nms.NMS12005Impl
 *
 * @author mical
 * @since 2024/5/5 20:26
 */
class NMSJ21Impl : NMSJ21() {

    override fun getRepairCost(item: ItemStack): Int {
        return (CraftItemStack.asNMSCopy(item) as NMSItemStack)[DataComponents.REPAIR_COST] ?: 0
    }

    override fun setRepairCost(item: ItemStack, cost: Int) : ItemStack {
        return CraftItemStack.asBukkitCopy((CraftItemStack.asNMSCopy(item) as NMSItemStack).apply {
            this[DataComponents.REPAIR_COST] = cost
        })
    }

    override fun adaptMerchantRecipe(merchantRecipeList: Any, player: Player) {

        fun adapt(item: Any, player: Player): Any {
            val bkItem = CraftItemStack.asBukkitCopy(item as NMSItemStack)
            if (bkItem.isNull) return item
            return CraftItemStack.asNMSCopy(bkItem.toDisplayMode(player))
        }

        val previous = merchantRecipeList as MerchantRecipeList
        for (i in 0 until previous.size) {
            with(previous[i]!!) {
                baseCostA.setProperty("itemStack", adapt(baseCostA.itemStack, player))
                setProperty("costB", Optional.ofNullable(costB.getOrNull()?.also { it.setProperty("itemStack", adapt(it.itemStack, player)) }))
                setProperty("result", adapt(result, player) as NMSItemStack)
            }
        }
    }

    override fun hurtAndBreak(nmsItem: Any, amount: Int, entity: LivingEntity) {
        return (nmsItem as NMSItemStack).hurtAndBreak(amount, (entity as CraftLivingEntity).handle, null)
    }
}

typealias NMSItemStack = net.minecraft.world.item.ItemStack