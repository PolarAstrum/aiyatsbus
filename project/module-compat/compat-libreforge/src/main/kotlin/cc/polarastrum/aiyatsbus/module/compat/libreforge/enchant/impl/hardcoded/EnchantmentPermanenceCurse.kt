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

import cc.polarastrum.aiyatsbus.core.aiyatsbusEt
import cc.polarastrum.aiyatsbus.core.etLevel
import cc.polarastrum.aiyatsbus.core.event.AiyatsbusPrepareAnvilEvent
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.HardcodedLibreforgeAiyatsbusEnchantBase
import cc.polarastrum.aiyatsbus.module.compat.libreforge.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import taboolib.common.LifeCycle
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * AiyatsbusLibreforge
 * cc.polarastrum.aiyatsbus.libreforge.enchant.impl.hardcoded.EnchantmentPermanenceCurse
 *
 * @author mical
 * @since 2025/10/5 21:51
 */
class EnchantmentPermanenceCurse(
    file: File,
    config: Configuration,
) : HardcodedLibreforgeAiyatsbusEnchantBase(
    "permanence_curse",
    file,
    config
) {

    private val handler = PermanenceCurseHandler(this)

    override fun register() {
        registerLifeCycleTask(LifeCycle.ACTIVE) {
            plugin.eventManager.registerListener(handler)
        }
    }

    override fun remove() {
        plugin.eventManager.unregisterListener(handler)
    }

    private class PermanenceCurseHandler(
        private val enchant: EnchantmentPermanenceCurse
    ) : Listener {

        @EventHandler
        fun handle(event: AiyatsbusPrepareAnvilEvent) {
            val first = event.left
            if (first.isEmpty || first.type.isAir) {
                return
            }
            if (first.etLevel(aiyatsbusEt("permanence_curse")!!) >= 1) {
                event.isCancelled = true
            }
        }
    }
}