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
package cc.polarastrum.aiyatsbus.module.script.kether

import cc.polarastrum.aiyatsbus.core.script.ScriptHandler
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console
import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import taboolib.module.kether.parseKetherScript
import taboolib.module.kether.runKether
import java.util.concurrent.CompletableFuture

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.kether.KetherScriptHandler
 *
 * @author mical
 * @since 2025/6/22 13:23
 */
class KetherScriptHandler : ScriptHandler {

    override fun invoke(
        source: String,
        sender: CommandSender?,
        variables: Map<String, Any?>
    ): CompletableFuture<Any?>? {
        val player = sender as? Player
        return runKether(detailError = true) {
            KetherShell.eval(source,
                ScriptOptions.builder().namespace(namespace = listOf("aiyatsbus"))
                    .sender(sender = if (player != null) adaptPlayer(player) else if (sender != null) adaptCommandSender(sender) else console())
                    .vars(variables)
                    .build())
        }
    }

    override fun preheat(source: String) {
        val s = if (source.startsWith("def ")) source else "def main = { $source }"
        KetherShell.mainCache.scriptMap[s] = s.parseKetherScript(listOf("aiyatsbus"))
    }
}