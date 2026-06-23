/*
 * This file is part of EcoEnchants, licensed under the GPL-3.0 License.
 *
 *  Copyright (C) 2024 Auxilor
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
package cc.polarastrum.aiyatsbus.impl.registration.v12100_paper

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantmentBase
import net.minecraft.world.item.enchantment.Enchantment
import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.bukkit.inventory.ItemStack

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.registration.v12104_paper.VanillaCraftEnchantment
 *
 * @author lynn
 * @since 2025/7/3
 */
class VanillaCraftEnchantment(
    private val enchant: VanillaAiyatsbusEnchantmentBase,
    holder: Enchantment
) : CraftEnchantment(enchant.enchantmentKey, holder), AiyatsbusEnchantment by enchant {

    init {
        enchant.enchantment = this
    }

    override fun getMaxLevel(): Int {
        return enchant.basicData.maxLevel
    }

    override fun conflictsWith(other: org.bukkit.enchantments.Enchantment): Boolean {
        return enchant.conflictsWith(other)
    }

    override fun canEnchantItem(item: ItemStack): Boolean {
        return enchant.canEnchantItem(item)
    }
}