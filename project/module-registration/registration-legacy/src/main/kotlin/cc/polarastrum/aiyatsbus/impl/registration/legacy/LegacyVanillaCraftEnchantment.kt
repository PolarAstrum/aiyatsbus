package cc.polarastrum.aiyatsbus.impl.registration.legacy

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.util.legacyToAdventure
import io.papermc.paper.enchantments.EnchantmentRarity
import net.kyori.adventure.text.Component
import org.bukkit.craftbukkit.v1_20_R2.enchantments.CraftEnchantment
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityCategory
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.registration.legacy.LegacyVanillaCraftEnchantment
 *
 * @author mical
 * @since 2025/8/23 01:21
 */
class LegacyVanillaCraftEnchantment(
    private val enchant: AiyatsbusEnchantmentBase,
    private val vanilla: Enchantment
) : CraftEnchantment(getRaw(vanilla)), AiyatsbusEnchantment by enchant {

    init {
        enchant.enchantment = this
    }

    override fun getMaxLevel(): Int {
        return enchant.basicData.maxLevel
    }

    override fun conflictsWith(other: Enchantment): Boolean {
        return enchant.conflictsWith(other)
    }

    override fun canEnchantItem(item: ItemStack): Boolean {
        return enchant.canEnchantItem(item)
    }

    override fun displayName(p0: Int): Component {
        return enchant.displayName(p0).legacyToAdventure()
    }

    override fun isTradeable(): Boolean {
        return vanilla.isTradeable
    }

    override fun isDiscoverable(): Boolean {
        return vanilla.isDiscoverable
    }

    override fun getMinModifiedCost(p0: Int): Int {
        return vanilla.getMinModifiedCost(p0)
    }

    override fun getMaxModifiedCost(p0: Int): Int {
        return vanilla.getMaxModifiedCost(p0)
    }

    override fun getRarity(): EnchantmentRarity {
        return vanilla.rarity
    }

    override fun getDamageIncrease(p0: Int, p1: EntityCategory): Float {
        return vanilla.getDamageIncrease(p0, p1)
    }

    override fun getActiveSlots(): Set<EquipmentSlot?> {
        return vanilla.activeSlots
    }

    override fun translationKey(): String {
        return vanilla.translationKey()
    }
}