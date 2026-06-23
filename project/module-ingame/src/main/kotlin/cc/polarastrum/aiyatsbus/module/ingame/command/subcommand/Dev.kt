@file:Suppress("USELESS_IS_CHECK")

package cc.polarastrum.aiyatsbus.module.ingame.command.subcommand

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.BuiltinAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.InternalAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.command.subCommand
import taboolib.common5.util.createBar
import taboolib.module.chat.colored
import taboolib.module.nms.NMSItemTag
import kotlin.system.measureTimeMillis

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.ingame.command.subcommand.Dev
 *
 * @author mical
 * @date 2024/9/7 13:10
 */
val devSubCommand = subCommand {
    execute<CommandSender> { sender, _, _ ->
//        sender.itemInHand.modifyMeta<ItemMeta> { this["aiyatsbus_item_capability", PersistentDataType.INTEGER] = 999 }
////       printMap( Aiyatsbus.api().getEnchantmentManager().getEnchants())
//        try {
//            XEnchantment.of("wings").getOrNull()?.get()?.javaClass?.let { println(it) }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

        (sender as Player).itemInHand.enchantments.keys.forEach { ench ->
            println("Enchant(id=${ench.key.key},isInternal=${(ench as AiyatsbusEnchantment) is InternalAiyatsbusEnchantment},isVanilla=${(ench as AiyatsbusEnchantment) is VanillaAiyatsbusEnchantment},isBuiltin=${(ench as AiyatsbusEnchantment) is BuiltinAiyatsbusEnchantment},isBase=${(ench as AiyatsbusEnchantment) is AiyatsbusEnchantmentBase},isAiyatsbus=${(ench as AiyatsbusEnchantment) is AiyatsbusEnchantment})")
        }

    }
////        sender.giveItem(sender.equipment.itemInMainHand.toDisplayMode(sender))
//        val item = sender.equipment.itemInMainHand
//        val clone = item.clone()
//
//        submitAsync {
//            testItemStackCopy(clone, sender)
//            testItemStackHandle(clone, sender)
//        }
//
//        sender.giveItem(clone.toDisplayMode(sender))
//        sender.giveItem(clone.toRevertMode(sender))
//
//        if (true) return@execute
//
//        sender.sendMessage("----- fixedEnchants -----")
//        val aiyatsbusEt = item.fixedEnchants.keys.firstOrNull()
//        if (aiyatsbusEt != null) {
//            sender.sendMessage(aiyatsbusEt.javaClass.name)
//            sender.sendMessage((aiyatsbusEt is AiyatsbusEnchantment).toString())
//            sender.sendMessage((aiyatsbusEt is AiyatsbusEnchantmentBase).toString())
//            sender.sendMessage((aiyatsbusEt is Enchantment).toString())
//        }
//        sender.sendMessage("----- enchantments -----")
//        val bukkitEt = item.enchantments.keys.firstOrNull()
//        if (bukkitEt != null) {
//            sender.sendMessage(bukkitEt.javaClass.name)
//            sender.sendMessage((bukkitEt is AiyatsbusEnchantment).toString())
//            sender.sendMessage((bukkitEt is AiyatsbusEnchantmentBase).toString())
//            sender.sendMessage((bukkitEt is Enchantment).toString())
//        }
//        sender.sendMessage("----- AiyatsbusEnchantmentManager -----")
//        val et3 = Aiyatsbus.api().getEnchantmentManager().getEnchant("accumulating")
//        if (et3 != null) {
//            sender.sendMessage(et3.javaClass.name)
//            sender.sendMessage((et3 is AiyatsbusEnchantment).toString())
//            sender.sendMessage((et3 is AiyatsbusEnchantmentBase).toString())
//            sender.sendMessage((et3 is Enchantment).toString())
//        }
//        sender.sendMessage("----- Bukkit Enchantment getByKey -----")
//        val et4 = Enchantment.getByKey(NamespacedKey.minecraft("accumulating"))
//        if (et4 != null) {
//            sender.sendMessage(et4.javaClass.name)
//            sender.sendMessage((et4 is AiyatsbusEnchantment).toString())
//            sender.sendMessage((et4 is AiyatsbusEnchantmentBase).toString())
//            sender.sendMessage((et4 is Enchantment).toString())
//        }
//        sender.sendMessage("----- Bukkit Enchantment getByName -----")
//        val et5 = Enchantment.getByName("accumulating")
//        if (et5 != null) {
//            sender.sendMessage(et5.javaClass.name)
//            sender.sendMessage((et5 is AiyatsbusEnchantment).toString())
//            sender.sendMessage((et5 is AiyatsbusEnchantmentBase).toString())
//            sender.sendMessage((et5 is Enchantment).toString())
//        }
//    }
}

private fun testItemStackCopy(item: ItemStack, player: Player) {
    val nmsItem = NMSItemTag.asNMSCopy(item)
    measureTimeMillis {
        repeat(100000) { c ->
            NMSItemTag.asNMSCopy(NMSItemTag.asBukkitCopy(nmsItem).toDisplayMode(player))
            player.sendActionBar("当前进度: " + createBar("&f|".colored(), "&a|".colored(), 100, c.toDouble() / 100000.0))
        }
    }.let { player.sendMessage(it.toString() + " ms") }
}

private fun testItemStackHandle(item: ItemStack, player: Player) {
    val nmsItem = NMSItemTag.asNMSCopy(item)
    measureTimeMillis {
        repeat(100000) { c ->
            Aiyatsbus.api().getMinecraftAPI().getHelper().getCraftItemStackHandle(Aiyatsbus.api().getMinecraftAPI().getHelper().asCraftMirror(nmsItem).toDisplayMode(player))
            player.sendActionBar("当前进度: " + createBar("&f|".colored(), "&a|".colored(), 100, c.toDouble() / 100000.0))
        }
    }.let { player.sendMessage(it.toString() + " ms") }
}

private fun printMap(map: Map<*, *>) {
    val maxKeyLength = map.keys.maxByOrNull { it.toString().length }?.toString()?.length ?: 0

    map.forEach { (key, value) ->
        println("${key.toString().padEnd(maxKeyLength)} : $value")
    }
// 输出（键左对齐）：
// username   : Alice
// age        : 30
// country    : United States
}