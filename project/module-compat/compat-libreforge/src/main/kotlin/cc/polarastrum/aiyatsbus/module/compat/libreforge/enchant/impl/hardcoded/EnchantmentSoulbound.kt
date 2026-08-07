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
import cc.polarastrum.aiyatsbus.module.compat.libreforge.target.LibreforgeEnchantFinder.getItemsWithEnchantActive
import cc.polarastrum.aiyatsbus.module.compat.libreforge.plugin
import com.willfp.eco.core.Prerequisite
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.fast.fast
import com.willfp.eco.core.items.Items
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.persistence.PersistentDataType
import taboolib.common.LifeCycle
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.configuration.Configuration
import java.io.File
import kotlin.collections.emptyList

/**
 * AiyatsbusLibreforge
 * cc.polarastrum.aiyatsbus.libreforge.enchant.impl.hardcoded.EnchantmentSoulbound
 *
 * @author mical
 * @since 2025/10/5 21:40
 */
class EnchantmentSoulbound(
    file: File,
    config: Configuration
) : HardcodedLibreforgeAiyatsbusEnchantBase(
    "soulbound",
    file,
    config,
) {

    private val handler = SoulboundHandler(this)

    override fun register() {
        registerLifeCycleTask(LifeCycle.ACTIVE) {
            plugin.eventManager.registerListener(handler)
        }
    }

    override fun remove() {
        plugin.eventManager.unregisterListener(handler)
    }

    private class SoulboundHandler(
        private val enchant: EnchantmentSoulbound
    ) : Listener {
        private val savedSoulboundItems = PersistentDataKey(
            plugin.namespacedKeyFactory.create("soulbound_items"),
            PersistentDataKeyType.STRING_LIST,
            emptyList()
        )

        private val soulboundKey = plugin.namespacedKeyFactory.create("soulbound")

        @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
        )
        fun handle(event: PlayerDeathEvent) {
            if (event.keepInventory) {
                return
            }

            val player = event.entity
            val items = player.getItemsWithEnchantActive(enchant).keys

            if (items.isEmpty()) {
                return
            }

            event.drops.removeAll(items)

            // Use native paper method
            if (Prerequisite.HAS_PAPER.isMet) {
                val modifiedItems = if (enchant.config.getBoolean("single-use")) {
                    items.map {
                        val meta = it.itemMeta
                        meta.removeEnchant(enchant.enchantment)
                        it.itemMeta = meta
                        it
                    }
                } else {
                    items
                }

                event.itemsToKeep += modifiedItems
                return
            }

            for (item in items) {
                item.fast().persistentDataContainer.set(soulboundKey, PersistentDataType.INTEGER, 1)

                if (enchant.config.getBoolean("single-use")) {
                    val meta = item.itemMeta
                    meta.removeEnchant(enchant.enchantment)
                    item.itemMeta = meta
                }
            }

            player.profile.write(savedSoulboundItems, items.map { Items.toSNBT(it) })
        }

        @EventHandler(
            ignoreCancelled = true
        )
        fun onJoin(event: PlayerJoinEvent) {
            giveItems(event.player)
        }

        @EventHandler(
            ignoreCancelled = true
        )
        fun onJoin(event: PlayerRespawnEvent) {
            giveItems(event.player)
        }

        private fun giveItems(player: Player) {
            val itemStrings = player.profile.read(savedSoulboundItems)

            if (itemStrings.isEmpty()) {
                return
            }

            val items = itemStrings.map { Items.fromSNBT(it) }

            plugin.scheduler.run {
                DropQueue(player)
                    .addItems(items)
                    .forceTelekinesis()
                    .push()
            }

            player.profile.write(savedSoulboundItems, emptyList())
        }

        @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
        )
        fun preventDroppingSoulboundItems(event: PlayerDeathEvent) {
            event.drops.removeIf {
                it.fast().persistentDataContainer.has(soulboundKey, PersistentDataType.INTEGER)
                        && it.itemMeta.hasEnchant(enchant.enchantment)
            }
        }
    }
}