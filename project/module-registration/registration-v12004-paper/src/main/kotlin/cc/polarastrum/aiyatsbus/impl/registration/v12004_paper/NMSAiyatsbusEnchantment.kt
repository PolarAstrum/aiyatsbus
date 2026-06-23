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
package cc.polarastrum.aiyatsbus.impl.registration.v12004_paper

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import net.minecraft.network.chat.IChatBaseComponent
import net.minecraft.world.inventory.ContainerAnvil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentSlotType
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import java.util.*

/**
 * TabooLib nmsProxy 有些问题, 又用不了 paperweight (也没必要用)
 * 只能用这么丑陋的办法来实现, 还好就这一个版本...
 *
 * @author mical
 * @date 2024/8/21 01:32
 */
class NMSAiyatsbusEnchantment(val id: String) : net.minecraft.world.item.enchantment.Enchantment(
    Rarity.d, // VERT_RARE
    EnchantmentSlotType.n, // VANISHABLE
    emptyArray()
) {

    private val enchantmentKey: NamespacedKey = NamespacedKey.minecraft(id)

    private val enchant: AiyatsbusEnchantment?
        get() = Aiyatsbus.api().getEnchantmentManager().getEnchant(enchantmentKey)

    // canEnchantItem
    override fun a(stack: ItemStack): Boolean {
        // 使 Aiyatsbus 接管铁砧
        val caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass
        if (caller.name == ContainerAnvil::class.java.name) {
            return false
        }

        val item = CraftItemStack.asCraftMirror(stack)
        return enchant?.canEnchantItem(item) ?: false
    }

    // getMinLevel
    override fun e(): Int = 1

    // getMaxLevel
    override fun a(): Int = enchant?.basicData?.maxLevel ?: 1

    // isCurse
    override fun c(): Boolean = false

    // isDiscoverable
    override fun i(): Boolean = false

    // isTradeable
    override fun h(): Boolean = false

    // isTreasureOnly
    override fun b(): Boolean = true

    // getMinCost
//    override fun a(var0: Int): Int {
//        return Int.MAX_VALUE
//    }
//
    // getMaxCost
//    override fun b(var0: Int): Int {
//        return Int.MAX_VALUE
//    }

    // getFullname
    override fun d(level: Int): IChatBaseComponent {
        return if (enchant != null) {
            IChatBaseComponent::class.java.invokeMethod<IChatBaseComponent>("a", enchant!!.displayName(level), remap = false)!!
        } else super.d(level) // super.getFullname(level)
    }

    override fun toString(): String {
        return "NMSAiyatsbusEnchantment(id='$id')"
    }

    override fun equals(other: Any?): Boolean {
        return other is NMSAiyatsbusEnchantment && other.id == this.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}