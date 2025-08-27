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

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.aiyatsbusEt
import cc.polarastrum.aiyatsbus.core.book
import cc.polarastrum.aiyatsbus.core.sendLang
import cc.polarastrum.aiyatsbus.module.ingame.command.AiyatsbusCommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggestPlayers
import taboolib.platform.util.giveItem

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.command.subcommand.Book
 *
 * @author mical
 * @since 2024/3/3 17:40
 */
val bookSubCommand = subCommand cmd@{
    dynamic("enchant") {
        suggestion<CommandSender> { _, _ -> AiyatsbusCommand.enchantNamesAndIds }
        execute<CommandSender> { sender, args, _ -> handleBook(sender, null, aiyatsbusEt(args["enchant"])!!) }
        dynamic("level", true) {
            suggestionUncheck<Player> level@{ _, cmd ->
                val maxLevel = (aiyatsbusEt(cmd["enchant"])?.basicData?.maxLevel ?: return@level listOf())
                buildList { repeat(maxLevel) { add("${it + 1}") } }
            }
            execute<CommandSender> { sender, args, _ -> handleBook(sender, null, aiyatsbusEt(args["enchant"])!!, args["level"].toInt()) }
            dynamic("player", true) {
                suggestPlayers()
                execute<CommandSender> { sender, args, _ -> handleBook(sender, args["player"], aiyatsbusEt(args["enchant"])!!, args["level"].toInt()) }
            }
        }
    }
}

private fun handleBook(sender: CommandSender, who: String?, enchant: AiyatsbusEnchantment, level: Int = enchant.basicData.maxLevel) {
    (who?.let { Bukkit.getPlayer(it) } ?: (sender as? Player))?.let { receiver ->
        receiver.giveItem(enchant.book(level))
        sender.sendLang("command-subCommands-book-sender", receiver.name to "name")
        receiver.sendLang("command-subCommands-book-receiver", enchant.displayName(level) to "enchantment")
    } ?: sender.sendLang("command-subCommands-book-fail")
}