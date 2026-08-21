/*
 * This file is part of Ratziel, licensed under the GPL-3.0 License.
 *
 *  Copyright (C) 2025 TheFloodDragon
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
package cc.polarastrum.aiyatsbus.impl.nms

import cc.polarastrum.aiyatsbus.core.MinecraftPacketHandler
import cc.polarastrum.aiyatsbus.core.toRevertMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import net.minecraft.network.protocol.game.PacketPlayOutAbilities
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata
import net.minecraft.network.protocol.game.PacketPlayOutWindowData
import net.minecraft.network.syncher.DataWatcher
import net.minecraft.network.syncher.DataWatcherObject
import net.minecraft.network.syncher.DataWatcherRegistry
import net.minecraft.world.entity.player.PlayerAbilities
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Player
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.library.reflex.Reflex.Companion.unsafeInstance
import taboolib.module.nms.MinecraftVersion.isUniversal
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.sendPacket

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftPacketHandler
 *
 * @author mical
 * @since 2025/8/16 09:34
 */
class DefaultMinecraftPacketHandler : MinecraftPacketHandler {

    override fun synchronizeRegistries(player: Player) {
//        if (versionId < 12005) return // 小于这个版本的没有 SynchronizeRegistriesTask，调用无意义
//        val uuid = player.uniqueId
//        (player as CraftPlayer).handle.connection.switchToConfig()
//
//        submit(delay = 10L) {
//            ((Bukkit.getServer() as CraftServer).server.connection.connections
//                .firstOrNull {
//                    it.packetListener is ServerConfigurationPacketListenerImpl &&
//                            (it.packetListener as ServerConfigurationPacketListenerImpl).owner.id == uuid
//                }
//                ?.packetListener as? ServerConfigurationPacketListenerImpl)
//                ?.startConfiguration()
//        }
    }

    override fun sendPlayerAbilities(player: Player, isCreativeMode: Boolean) {
        player.sendPacket(PacketPlayOutAbilities(PlayerAbilities().apply {
            this.invulnerable = player.isInvulnerable
            this.flying = player.isFlying
            this.mayfly = player.allowFlight
            this.instabuild = isCreativeMode
            this.flyingSpeed = player.flySpeed / 2
            this.walkingSpeed = player.walkSpeed / 2
        }))
    }

    override fun sendAnvilWindowProperty(player: Player, windowId: Int, cost: Int) {
        player.sendPacket(PacketPlayOutWindowData(windowId, 0, cost))
    }

    override fun sendAllContainerData(player: Player) {
        (player as CraftPlayer).handle.containerMenu.sendAllDataToRemote()
    }

    override fun setHandActive(player: Player, isHandActive: Boolean) {
        val byte = (if (isHandActive) 0x01 else 0).toByte()
        when {
            versionId >= 11903 -> {
                player.sendPacket(
                    PacketPlayOutEntityMetadata::class.java.invokeConstructor(
                        player.entityId,
                        listOf((createByteMeta(8, byte) as DataWatcher.Item<*>).invokeMethod<Any>("value"))
                    )
                )
            }
            isUniversal -> {
                player.sendPacket(PacketPlayOutEntityMetadata::class.java.unsafeInstance().apply {
                    setProperty("id", player.entityId)
                    setProperty("packedItems", listOf((createByteMeta(8, byte) as DataWatcher.Item<*>)))
                })
            }
            else -> {
                player.sendPacket(net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata().apply {
                    setProperty("a", player.entityId)
                    setProperty("b", listOf((createByteMeta(8, byte) as DataWatcher.Item<*>)))
                })
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun createByteMeta(index: Int, value: Byte): Any {
        return when {
            // 1.19+
            versionId >= 11900 -> DataWatcher.Item(DataWatcherObject(index, DataWatcherRegistry.BYTE), value)
            else -> net.minecraft.server.v1_13_R2.DataWatcher.Item(net.minecraft.server.v1_13_R2.DataWatcherObject(index, net.minecraft.server.v1_13_R2.DataWatcherRegistry.a), value)
        }
    }

    /**
     * isUniversal -> carriedItem
     * 1.16- -> item
     * 这里不做判断
     */
    private val carriedItemFieldInContainerClick = "carriedItem"

    /** since 1.17 */
    private val changedSlotsField = "changedSlots"

    override fun handleContainerClick(event: PacketReceiveEvent) {
        val items = HashMap<Int, Any>()
        // 光标上的物品
        items[-10086] = event.packet.read<Any>(carriedItemFieldInContainerClick) ?: error("$carriedItemFieldInContainerClick does not exist.")

        handleItem(event.packet.read<Any>(carriedItemFieldInContainerClick), event.player)?.let { carried -> event.packet.write(carriedItemFieldInContainerClick, carried) }

        // 1.17 加入 changedSlots
        // 1.21.4-
        if (isUniversal) {
            items.putAll((event.packet.read<Map<Int, Any>>(changedSlotsField) ?: emptyMap()).mapValues { (_, nmsItem) ->
                handleItem(nmsItem, event.player) ?: nmsItem
            })
        }
    }

    private fun handleItem(nmsItem: Any?, player: Player): Any? {
        val bkItem = CraftItemStack.asCraftMirror(nmsItem as? NMSItemStack ?: return null)
        if (bkItem.isNull) return null
        return (bkItem.toRevertMode(player) as CraftItemStack).getProperty("handle")
    }
}
