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
 */
package cc.polarastrum.aiyatsbus.module.bukkit

import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.bukkit.AdventureSetup
 *
 * @author mical
 * @since 2025/2/23 11:54
 */
@RuntimeDependencies(
    RuntimeDependency(
        value = "!net.kyori:adventure-api:4.18.0",
        test = "!net.kyori.adventure.Adventure"
    ),
    RuntimeDependency(
        value = "!net.kyori:adventure-text-serializer-gson:4.18.0",
        test = "!net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"
    ),
    RuntimeDependency(
        value = "!net.kyori:adventure-text-serializer-legacy:4.18.0",
        test = "!net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"
    )
)
object AdventureSetup {

    @Awake(LifeCycle.CONST)
    fun init() {}
}