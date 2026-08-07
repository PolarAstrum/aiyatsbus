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
package cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant

import com.willfp.eco.core.EcoPlugin
import taboolib.common.platform.function.warning
import kotlin.collections.iterator

class MissingDependencyException(
    val plugins: Set<String>
) : Exception() {
    override val message = "[AiyatsbusLibreforge] Missing the following plugins: ${plugins.joinToString(", ")}"
}

// Plugin names mapped to enchants that aren't installed.
private val prompts = mutableMapOf<String, Int>()

fun addPluginPrompt(plugin: EcoPlugin, plugins: Set<String>) {
    for (pluginName in plugins) {
        prompts[pluginName] = prompts.getOrDefault(pluginName, 0) + 1
    }
}

fun sendPrompts(plugin: EcoPlugin) {
    for ((pl, amount) in prompts) {
        warning("[AiyatsbusLibreforge] $amount enchantments were not loaded because they need $pl to be installed!")
        warning("[AiyatsbusLibreforge] Either download $pl or delete the folder at /plugins/AiyatsbusLibreforge/enchants/${pl.lowercase()} to remove this message")
    }

    prompts.clear()
}