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
@file:Suppress("UNUSED_PARAMETER", "DuplicatedCode")

package cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant

import org.bukkit.NamespacedKey

/**
 * AiyatsbusLibreforge
 * com.mcstarrysky.aiyatsbus.libreforge.enchant.LibreforgeEnchants
 *
 * @author mical
 * @date 2024/8/21 19:47
 */
internal val libreforgeEnchantsMap = HashMap<NamespacedKey, LibreforgeAiyatsbusEnchantment>()

object LibreforgeEnchants : MutableMap<NamespacedKey, LibreforgeAiyatsbusEnchantment> by libreforgeEnchantsMap