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
package cc.polarastrum.aiyatsbus.module.script

import cc.polarastrum.aiyatsbus.core.script.AiyatsbusScriptHandler
import cc.polarastrum.aiyatsbus.core.script.ScriptHandler
import cc.polarastrum.aiyatsbus.core.script.ScriptType
import cc.polarastrum.aiyatsbus.module.script.kether.KetherScriptHandler
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.script.DefaultAiyatsbusScriptHandler
 *
 * @author mical
 * @since 2025/6/22 13:18
 */
class DefaultAiyatsbusScriptHandler : AiyatsbusScriptHandler {

    val ketherScriptHandler = KetherScriptHandler()
    val fluxonScriptHandler = FluxonScriptHandler()

    override fun getScriptHandler(type: ScriptType): ScriptHandler {
        return when (type) {
            ScriptType.KETHER -> ketherScriptHandler
            ScriptType.FLUXON -> fluxonScriptHandler
            else -> error("Unsupported script type: ${type.name}")
        }
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusScriptHandler>(DefaultAiyatsbusScriptHandler())
        }
    }
}