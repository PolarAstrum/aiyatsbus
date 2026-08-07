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

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.InternalAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.data.trigger.Mechanism
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.hardcoded.EnchantmentPermanenceCurse
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.hardcoded.EnchantmentRepairing
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.hardcoded.EnchantmentReplenish
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.hardcoded.EnchantmentSoulbound
import cc.polarastrum.aiyatsbus.module.compat.libreforge.plugin
import com.willfp.eco.core.config.Configs
import taboolib.module.configuration.Configuration
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.conditions.ConditionList
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.EffectList
import com.willfp.libreforge.effects.Effects
import java.io.File

/**
 * AiyatsbusLibreforge
 * com.mcstarrysky.aiyatsbus.libreforge.enchant.LibreforgeAiyatsbusEnchant
 *
 * @author mical
 * @date 2024/8/21 19:15
 */
interface LibreforgeAiyatsbusEnchantment : InternalAiyatsbusEnchantment {

    val levels: MutableMap<Int, LibreforgeEnchantLevel>

    val conditions: ConditionList

    fun getLevel(level: Int): LibreforgeEnchantLevel

    companion object {

        fun newEnchant(id: String, file: File, config: Configuration): AiyatsbusEnchantmentBase {
            return when (id) {
                "permanence_curse" -> EnchantmentPermanenceCurse(file, config)
                "repairing" -> EnchantmentRepairing(file, config)
                "replenish" -> EnchantmentReplenish(file, config)
                "soulbound" -> EnchantmentSoulbound(file, config)
                else -> LibreforgeAiyatsbusEnchantBase(id, file, config)
            }
        }
    }
}

open class LibreforgeAiyatsbusEnchantBase(
    id: String,
    file: File,
    config: Configuration
) : AiyatsbusEnchantmentBase(id, file, config), LibreforgeAiyatsbusEnchantment {

    private val context = ViolationContext(plugin, "enchantment $id")

    override val levels = mutableMapOf<Int, LibreforgeEnchantLevel>()

    override val mechanism: Mechanism? = null

    override lateinit var conditions: ConditionList

    private val effects: EffectList

    override fun getLevel(level: Int): LibreforgeEnchantLevel {
        return levels.getOrPut(level) {
            LibreforgeEnchantLevel(this, level, effects, conditions, plugin)
        }
    }

    init {
        val ecoConfig = Configs.fromFile(file)

        // Compile here so MissingDependencyException is thrown before effects are compiled
        effects = Effects.compile(
            ecoConfig.getSubsections("effects"),
            context.with("effects")
        )

        conditions = Conditions.compile(
            ecoConfig.getSubsections("conditions"),
            context.with("conditions")
        )
    }
}