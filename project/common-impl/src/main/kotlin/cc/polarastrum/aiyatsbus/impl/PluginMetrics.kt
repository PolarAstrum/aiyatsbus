/*
 *  Copyright (C) 2022-2024 PolarAstrumLab
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
 */package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.common.platform.function.pluginVersion
import taboolib.module.metrics.Metrics
import taboolib.module.metrics.charts.SingleLineChart

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.cc.polarastrum.aiyatsbus.impl.PluginMetrics
 *
 * @author mical
 * @since 2024/3/19 23:20
 */
object PluginMetrics {

    lateinit var metrics: Metrics
        private set

    @Awake(LifeCycle.ACTIVE)
    private fun init() {
        metrics = Metrics(21363, pluginVersion, Platform.BUKKIT)

        // enchantments
        metrics.addCustomChart(SingleLineChart("enchantments") {
            Aiyatsbus.api().getEnchantmentManager().getEnchants().size
        })
    }
}