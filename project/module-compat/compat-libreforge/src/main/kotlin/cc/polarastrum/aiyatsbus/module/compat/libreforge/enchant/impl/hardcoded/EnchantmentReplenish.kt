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
package cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.hardcoded

import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.HardcodedLibreforgeAiyatsbusEnchantBase
import cc.polarastrum.aiyatsbus.module.compat.libreforge.target.LibreforgeEnchantFinder.hasEnchantActive
import cc.polarastrum.aiyatsbus.module.compat.libreforge.plugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * AiyatsbusLibreforge
 * cc.polarastrum.aiyatsbus.libreforge.enchant.impl.hardcoded.EnchantmentReplenish
 *
 * @author mical
 * @since 2025/10/5 21:36
 */
class EnchantmentReplenish(
    file: File,
    config: Configuration
) : HardcodedLibreforgeAiyatsbusEnchantBase(
    "replenish",
    file,
    config
) {
    private var handler = ReplenishHandler(this)

    override fun register() {
        registerLifeCycleTask(LifeCycle.ACTIVE) {
            plugin.eventManager.registerListener(handler)
        }
    }

    override fun remove() {
        plugin.eventManager.unregisterListener(handler)
    }

    private class ReplenishHandler(
        private val enchant: EnchantmentReplenish
    ) : Listener {
        @EventHandler(
            ignoreCancelled = true
        )
        fun handle(event: BlockBreakEvent) {
            val player = event.player

            if (!player.hasEnchantActive(enchant)) {
                return
            }

            val block = event.block
            val type = block.type

            if (type in arrayOf(
                    Material.GLOW_BERRIES,
                    Material.SWEET_BERRY_BUSH,
                    Material.CACTUS,
                    Material.BAMBOO,
                    Material.CHORUS_FLOWER,
                    Material.SUGAR_CANE
                )
            ) {
                return
            }

            val data = block.blockData

            if (data !is Ageable) {
                return
            }

            if (enchant.config.getBoolean("consume-seeds")) {
                val item = ItemStack(
                    when (type) {
                        Material.WHEAT -> Material.WHEAT_SEEDS
                        Material.POTATOES -> Material.POTATO
                        Material.CARROTS -> Material.CARROT
                        Material.BEETROOTS -> Material.BEETROOT_SEEDS
                        Material.COCOA -> Material.COCOA_BEANS
                        else -> type
                    }
                )

                val hasSeeds = player.inventory.removeItem(item).isEmpty()

                if (!hasSeeds) {
                    return
                }
            }

            if (data.age != data.maximumAge) {
                if (enchant.config.getBoolean("only-fully-grown")) {
                    return
                }

                event.isDropItems = false
                event.expToDrop = 0
            }

            data.age = 0

            plugin.scheduler.run {
                block.type = type
                block.blockData = data

                // Improves compatibility with other plugins.
                Bukkit.getPluginManager().callEvent(
                    BlockPlaceEvent(
                        block,
                        block.state,
                        block.getRelative(BlockFace.DOWN),
                        player.inventory.itemInMainHand,
                        player,
                        true,
                        EquipmentSlot.HAND
                    )
                )
            }
        }
    }
}