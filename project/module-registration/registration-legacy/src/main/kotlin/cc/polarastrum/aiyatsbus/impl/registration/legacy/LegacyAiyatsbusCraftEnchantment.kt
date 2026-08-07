package cc.polarastrum.aiyatsbus.impl.registration.legacy

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.BuiltinAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.BuiltinAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.InternalAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.InternalAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.util.legacyToAdventure
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeAiyatsbusEnchantBase
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeAiyatsbusEnchantment
import io.papermc.paper.enchantments.EnchantmentRarity
import net.kyori.adventure.text.Component
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.entity.EntityCategory
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.registration.legacy.LegacyCraftEnchantment
 *
 * @author mical
 * @since 2024/2/17 15:01
 */
open class LegacyAiyatsbusCraftEnchantment(
    private val enchant: AiyatsbusEnchantmentBase
) : Enchantment(enchant.enchantmentKey), AiyatsbusEnchantment by enchant {

    init {
        enchant.enchantment = this
        enchant.mechanism?.enchant = this
        enchant.limitations.belonging = this
        enchant.displayer.enchant = this
    }

    private val hashCode = enchant.id.hashCode()

    override fun getName(): String = enchant.basicData.id.uppercase()

    override fun getMaxLevel(): Int = enchant.basicData.maxLevel

    override fun getStartLevel(): Int = 1

    override fun getItemTarget(): EnchantmentTarget = EnchantmentTarget.ALL

    override fun isTreasure(): Boolean = enchant.alternativeData.isTreasure

    override fun isCursed(): Boolean = enchant.alternativeData.isCursed

    override fun conflictsWith(other: Enchantment): Boolean = enchant.conflictsWith(other)

    override fun canEnchantItem(item: ItemStack): Boolean = enchant.canEnchantItem(item)

    override fun displayName(level: Int): Component {
        return enchant.displayName(level).legacyToAdventure()
    }

    override fun isTradeable(): Boolean = enchant.alternativeData.isTradeable

    override fun isDiscoverable(): Boolean = enchant.alternativeData.isDiscoverable

    override fun getMinModifiedCost(level: Int): Int {
        return 0
    }

    override fun getMaxModifiedCost(level: Int): Int {
        return 0
    }

    override fun getRarity(): EnchantmentRarity = EnchantmentRarity.VERY_RARE

    override fun getDamageIncrease(level: Int, entityCategory: EntityCategory): Float = 0.0f

    override fun getActiveSlots(): MutableSet<EquipmentSlot> = mutableSetOf()

    override fun equals(other: Any?): Boolean {
        return other is AiyatsbusEnchantment && this.hashCode() == other.hashCode() && this.id == other.id
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun translationKey(): String = enchant.basicData.id
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class LegacyLibreforgeAiyatsbusCraftEnchantment(
    private val enchant: LibreforgeAiyatsbusEnchantBase
) : LegacyAiyatsbusCraftEnchantment(enchant), LibreforgeAiyatsbusEnchantment by enchant

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class LegacyBuiltinAiyatsbusCraftEnchantment(
    private val enchant: BuiltinAiyatsbusEnchantmentBase
) : LegacyAiyatsbusCraftEnchantment(enchant), BuiltinAiyatsbusEnchantment by enchant

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class LegacyInternalAiyatsbusCraftEnchantment(
    private val enchant: InternalAiyatsbusEnchantmentBase
) : LegacyAiyatsbusCraftEnchantment(enchant), InternalAiyatsbusEnchantment by enchant