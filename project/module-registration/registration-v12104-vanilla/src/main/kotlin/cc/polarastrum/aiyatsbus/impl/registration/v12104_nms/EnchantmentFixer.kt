package cc.polarastrum.aiyatsbus.impl.registration.v12104_nms

import io.papermc.paper.enchantments.EnchantmentRarity
import net.kyori.adventure.text.Component
import org.bukkit.entity.EntityCategory
import org.bukkit.inventory.EquipmentSlot

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.registration.v12104_nms.EnchantmentFixer
 *
 * @author mical
 * @since 2025/2/23 12:45
 */
interface EnchantmentFixer {

    fun translationKey(): String

    fun displayName(p0: Int): Component

    fun isTradeable(): Boolean

    fun isDiscoverable(): Boolean

    fun getMinModifiedCost(level: Int): Int

    fun getMaxModifiedCost(level: Int): Int

    fun getDamageIncrease(level: Int, entityCategory: EntityCategory): Float

    fun getRarity(): EnchantmentRarity

    fun getActiveSlots(): Set<EquipmentSlot>
}