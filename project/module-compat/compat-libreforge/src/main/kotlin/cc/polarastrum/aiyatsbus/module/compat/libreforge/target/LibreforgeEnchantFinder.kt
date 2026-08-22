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
package cc.polarastrum.aiyatsbus.module.compat.libreforge.target

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.fastFixedEnchants
import cc.polarastrum.aiyatsbus.core.util.isNull
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeEnchantLevel
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeEnchants
import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.libreforge.*
import com.willfp.libreforge.slot.ItemHolderFinder
import com.willfp.libreforge.slot.SlotType
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.Requires
import taboolib.common.env.RuntimeDependency
import taboolib.common.util.unsafeLazy
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * AiyatsbusLibreforge
 * com.mcstarrysky.aiyatsbus.libreforge.LibreforgeEnchantFinder
 *
 * @author mical
 * @date 2024/8/21 20:08
 */
@Requires(classes = ["com.willfp.libreforge.slot.ItemHolderFinder"])
@RuntimeDependency(value = "!com.github.ben-manes.caffeine:caffeine:3.1.5", test = "!com.github.benmanes.caffeine.cache.Caffeine")
object LibreforgeEnchantFinder {

    private val levelCache by unsafeLazy {
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build<UUID, List<ProvidedLevel>>()
    }

    private val LivingEntity.cachedLevels: List<ProvidedLevel>
        get() = levelCache.get(this.uniqueId) {
            finder.toHolderProvider().provide(this.toDispatcher())
                .mapNotNull {
                    val level = it.holder as? LibreforgeEnchantLevel ?: return@mapNotNull null
                    val item = it.provider as? ItemStack ?: return@mapNotNull null

                    ProvidedLevel(level, item, it)
                }
        }

    /**
     * 奇妙的 Bug
     */
    val finder by unsafeLazy {
        object : ItemHolderFinder<LibreforgeEnchantLevel>() {

            private val transfer = mapOf(
                "armor" to listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET),
                "boots" to listOf(EquipmentSlot.FEET),
                "chestplate" to listOf(EquipmentSlot.CHEST),
                "hand" to listOf(EquipmentSlot.HAND),
                "hands" to listOf(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND),
                "helmet" to listOf(EquipmentSlot.HEAD),
                "leggings" to listOf(EquipmentSlot.LEGS),
                "mainhand" to listOf(EquipmentSlot.HAND),
                "offhand" to listOf(EquipmentSlot.OFF_HAND),
                "any" to EquipmentSlot.values().toList(),
            )

            override fun find(item: ItemStack): List<LibreforgeEnchantLevel> {
                val enchants = mutableListOf<LibreforgeEnchantLevel>()
                if (item.isNull) return enchants
                val enchantMap = item.fastFixedEnchants

                for ((enchant, level) in enchantMap) {
                    enchant as AiyatsbusEnchantment
                    level as Int
                    val libreforgeEnchant = LibreforgeEnchants[enchant.enchantmentKey] ?: continue

                    enchants += libreforgeEnchant.getLevel(level)
                }

                return enchants
            }

            override fun isValidInSlot(holder: LibreforgeEnchantLevel, slot: SlotType): Boolean {
                return holder.enchant.targets.map { it.activeSlots }
                    .any { target ->
                        transfer[slot.id]?.any { it in target } == true
//                it == SlotTypeTransfer.transfer[slot.id]
                    }
            }
        }
    }

    internal fun LivingEntity.clearEnchantmentCache() = levelCache.invalidate(this.uniqueId)

    fun LivingEntity.hasEnchantActive(enchant: LibreforgeAiyatsbusEnchantment): Boolean {
        return this.cachedLevels
            .filter { it.level.enchant == enchant }
            .any { it.level.conditions.areMet(this.toDispatcher(), it.holder) }
    }

    fun LivingEntity.getItemsWithEnchantActive(enchant: LibreforgeAiyatsbusEnchantment): Map<ItemStack, Int> {
        return this.cachedLevels
            .filter { it.level.enchant == enchant }
            .filter { it.level.conditions.areMet(this.toDispatcher(), it.holder) }
            .associate { it.item to it.level.level }
    }

    private data class ProvidedLevel(
        val level: LibreforgeEnchantLevel,
        val item: ItemStack,
        val holder: ProvidedHolder
    )
}