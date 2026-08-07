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
import cc.polarastrum.aiyatsbus.module.compat.libreforge.target.LibreforgeEnchantFinder.hasEnchantActive
import cc.polarastrum.aiyatsbus.module.compat.libreforge.plugin
import com.willfp.eco.core.config.Configs
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.util.DurabilityUtils
import com.willfp.libreforge.slot.impl.SlotTypeHands
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * AiyatsbusLibreforge
 * cc.polarastrum.aiyatsbus.libreforge.enchant.impl.hardcoded.EnchantmentRepairing
 *
 * @author mical
 * @since 2025/10/5 21:28
 */
class EnchantmentRepairing(
    file: File,
    config: Configuration
) : HardcodedLibreforgeAiyatsbusEnchantBase(
    "repairing",
    file,
    config
) {

    lateinit var ecoConfig: Config
        private set

    override fun register() {
        if (!::ecoConfig.isInitialized) {
            ecoConfig = Configs.fromFile(file)
        }
        val frequency = ecoConfig.getInt("frequency").toLong()

        registerLifeCycleTask(LifeCycle.ACTIVE) {
            plugin.scheduler.runTimer(frequency, frequency) {
                handleRepairing()
            }
        }
    }

    private fun handleRepairing() {
        val notWhileHolding = ecoConfig.getBool("not-while-holding")

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasEnchantActive(this)) {
                val repairPerLevel = ecoConfig.getIntFromExpression("repair-per-level", player)

                for ((item, level) in player.getItemsWithEnchantActive(this)) {
                    val isHolding = item in SlotTypeHands.getItems(player)

                    if (notWhileHolding && isHolding) {
                        continue
                    }

                    DurabilityUtils.repairItem(item, level * repairPerLevel)
                }
            }
        }
    }
}