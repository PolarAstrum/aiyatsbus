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
package cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl

import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeAiyatsbusEnchantBase
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeEnchantLevel
import cc.polarastrum.aiyatsbus.module.compat.libreforge.plugin
import com.willfp.libreforge.effects.emptyEffectList
import taboolib.module.configuration.Configuration
import java.io.File
import kotlin.collections.getOrPut

/**
 * AiyatsbusLibreforge
 * cc.polarastrum.aiyatsbus.libreforge.enchant.impl.HardcodedLibreforgeAiyatsbusEnchant
 *
 * @author mical
 * @since 2025/10/5 20:42
 */
interface HardcodedLibreforgeAiyatsbusEnchantment : LibreforgeAiyatsbusEnchantment {

    fun register() {
    }

    fun remove() {
    }
}

abstract class HardcodedLibreforgeAiyatsbusEnchantBase(
    id: String,
    file: File,
    config: Configuration,
) : LibreforgeAiyatsbusEnchantBase(id, file, config), HardcodedLibreforgeAiyatsbusEnchantment {

    override fun getLevel(level: Int): LibreforgeEnchantLevel {
        return levels.getOrPut(level) {
            LibreforgeEnchantLevel(this, level, emptyEffectList(), conditions, plugin)
        }
    }
}