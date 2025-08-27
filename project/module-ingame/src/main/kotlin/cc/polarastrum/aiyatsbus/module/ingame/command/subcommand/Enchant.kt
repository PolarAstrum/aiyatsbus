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
package cc.polarastrum.aiyatsbus.module.ingame.command.subcommand

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.util.isNull
import cc.polarastrum.aiyatsbus.module.ingame.command.AiyatsbusCommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggestPlayers

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.command.subcommand.Enchant
 *
 * @author mical
 * @since 2024/3/3 17:48
 */
val enchantSubCommand = subCommand {
    dynamic("enchant") {
        suggestion<CommandSender> { _, _ -> AiyatsbusCommand.enchantNamesAndIds }
        execute<CommandSender> { sender, args, _ -> handleEnchant(sender, null, aiyatsbusEt(args["enchant"])!!) }
        dynamic("level", true) {
            suggestionUncheck<Player> level@{ _, cmd ->
                val maxLevel = (aiyatsbusEt(cmd["enchant"])?.basicData?.maxLevel ?: return@level listOf())
                buildList { repeat(maxLevel + 1) { add("$it") } }
            }
            execute<CommandSender> { sender, args, _ -> handleEnchant(sender, null, aiyatsbusEt(args["enchant"])!!, args["level"].toInt()) }
            dynamic("player", true) {
                suggestPlayers()
                execute<CommandSender> { sender, args, _ -> handleEnchant(sender, args["player"], aiyatsbusEt(args["enchant"])!!, args["level"].toInt()) }
            }
        }
    }
}

private fun handleEnchant(sender: CommandSender, who: String?, enchant: AiyatsbusEnchantment, level: Int = enchant.basicData.maxLevel) {
    (who?.let { Bukkit.getPlayer(it) } ?: (sender as? Player))?.let { receiver ->
        val item = receiver.inventory.itemInMainHand
        if (item.isNull) {
            sender.sendLang("command-subCommands-enchant-empty")
            return
        }
        val state = if (level == 0) sender.asLang("remove") else sender.asLang("add")
        if (level == 0) item.removeEt(enchant)
        else item.addEt(enchant, level)
        sender.sendLang(
            "command-subCommands-enchant-sender",
            receiver.name to "name",
            state to "state",
            enchant.displayName(level) to "enchantment"
        )
        receiver.sendLang("command-subCommands-enchant-receiver", state to "state", enchant.displayName(level) to "enchantment")
    } ?: sender.sendLang("command-subCommands-enchant-fail")
}