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
@file:Suppress("USELESS_IS_CHECK")

package cc.polarastrum.aiyatsbus.module.ingame.command.subcommand

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.fixedEnchants
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.toRevertMode
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submitAsync
import taboolib.common5.util.createBar
import taboolib.module.chat.colored
import taboolib.module.nms.NMSItemTag
import taboolib.platform.util.giveItem
import kotlin.system.measureTimeMillis

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.ingame.command.subcommand.Dev
 *
 * @author mical
 * @date 2024/9/7 13:10
 */
val devSubCommand = subCommand {
    execute<Player> { sender, _, _ ->
//        sender.giveItem(sender.equipment.itemInMainHand.toDisplayMode(sender))
        val item = sender.equipment.itemInMainHand
        val clone = item.clone()

        submitAsync {
            testItemStackCopy(clone, sender)
            testItemStackHandle(clone, sender)
        }

        sender.giveItem(clone.toDisplayMode(sender))
        sender.giveItem(clone.toRevertMode(sender))

        if (true) return@execute

        sender.sendMessage("----- fixedEnchants -----")
        val aiyatsbusEt = item.fixedEnchants.keys.firstOrNull()
        if (aiyatsbusEt != null) {
            sender.sendMessage(aiyatsbusEt.javaClass.name)
            sender.sendMessage((aiyatsbusEt is AiyatsbusEnchantment).toString())
            sender.sendMessage((aiyatsbusEt is AiyatsbusEnchantmentBase).toString())
            sender.sendMessage((aiyatsbusEt is Enchantment).toString())
        }
        sender.sendMessage("----- enchantments -----")
        val bukkitEt = item.enchantments.keys.firstOrNull()
        if (bukkitEt != null) {
            sender.sendMessage(bukkitEt.javaClass.name)
            sender.sendMessage((bukkitEt is AiyatsbusEnchantment).toString())
            sender.sendMessage((bukkitEt is AiyatsbusEnchantmentBase).toString())
            sender.sendMessage((bukkitEt is Enchantment).toString())
        }
        sender.sendMessage("----- AiyatsbusEnchantmentManager -----")
        val et3 = Aiyatsbus.api().getEnchantmentManager().getEnchant("accumulating")
        if (et3 != null) {
            sender.sendMessage(et3.javaClass.name)
            sender.sendMessage((et3 is AiyatsbusEnchantment).toString())
            sender.sendMessage((et3 is AiyatsbusEnchantmentBase).toString())
            sender.sendMessage((et3 is Enchantment).toString())
        }
        sender.sendMessage("----- Bukkit Enchantment getByKey -----")
        val et4 = Enchantment.getByKey(NamespacedKey.minecraft("accumulating"))
        if (et4 != null) {
            sender.sendMessage(et4.javaClass.name)
            sender.sendMessage((et4 is AiyatsbusEnchantment).toString())
            sender.sendMessage((et4 is AiyatsbusEnchantmentBase).toString())
            sender.sendMessage((et4 is Enchantment).toString())
        }
        sender.sendMessage("----- Bukkit Enchantment getByName -----")
        val et5 = Enchantment.getByName("accumulating")
        if (et5 != null) {
            sender.sendMessage(et5.javaClass.name)
            sender.sendMessage((et5 is AiyatsbusEnchantment).toString())
            sender.sendMessage((et5 is AiyatsbusEnchantmentBase).toString())
            sender.sendMessage((et5 is Enchantment).toString())
        }
    }
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