package cc.polarastrum.aiyatsbus.module.ingame.command.subcommand

import cc.polarastrum.aiyatsbus.core.book
import cc.polarastrum.aiyatsbus.core.capability
import cc.polarastrum.aiyatsbus.core.sendLang
import cc.polarastrum.aiyatsbus.core.util.get
import cc.polarastrum.aiyatsbus.core.util.set
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggestPlayers
import taboolib.platform.util.modifyMeta

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.ingame.command.subcommand.Capability
 *
 * @author mical
 * @since 2026/4/22 15:21
 */
val capabilitySubCommand = subCommand cmd@{ 
    dynamic("operation") {
        suggestion<CommandSender> { _, _ -> listOf("add", "take", "set") }
        dynamic("amount") {
            suggestionUncheck<CommandSender> { _, _ ->
                listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
            }
            execute<Player> { sender, args, _ -> handleCapability(sender, sender.name, args["operation"], args["amount"].toInt()) }
            dynamic("player", optional = true) {
                suggestPlayers()
                execute<CommandSender> { sender, args, _ -> handleCapability(sender, args["player"], args["operation"], args["amount"].toInt()) }
            }
        }
    }
}

private fun handleCapability(sender: CommandSender, who: String?, operation: String, amount: Int) {
    (who?.let { Bukkit.getPlayer(it) } ?: (sender as? Player))?.let { receiver ->
        receiver.itemInHand.let { item ->
            item.modifyMeta<ItemMeta> {
                val current = item.capability
                val update = when (operation) {
                    "add" -> current + amount
                    "take" -> current - amount
                    "set" -> amount
                    else -> amount
                }
                set("aiyatsbus_item_capability", PersistentDataType.INTEGER, update)

                sender.sendLang("command-subCommands-capability-sender", receiver.name to "name", update to "amount")
                receiver.sendLang("command-subCommands-capability-receiver", update to "amount")
            }
        }
    } ?: sender.sendLang("command-subCommands-capability-fail")
}