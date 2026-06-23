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
package cc.polarastrum.aiyatsbus.impl.nmsj21

import cc.polarastrum.aiyatsbus.core.MinecraftPacketHandler
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import com.mojang.authlib.GameProfile
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import net.minecraft.network.HashedPatchMap
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl
import net.minecraft.world.item.ItemStack
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.module.nms.MinecraftVersion.isUniversal
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.remap.DynamicOpcode
import taboolib.module.nms.remap.dynamic
import taboolib.module.nms.sendPacket
import taboolib.platform.Folia
import java.util.*

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftPacketHandler
 *
 * @author mical
 * @since 2025/8/16 09:34
 */
class DefaultMinecraftPacketHandler : MinecraftPacketHandler {

    override fun synchronizeRegistries(player: Player) {
        if (versionId < 12107) return // fixme 1.21.7 以前的版本有点问题
        if (Folia.isFolia) return // fixme folia 有点问题
        val uuid = player.uniqueId
        (player as CraftPlayer).handle.connection.switchToConfig()

        submit(delay = 10L) {
            (MinecraftServer.getServer().connection.connections
                .firstOrNull {
                    it.packetListener is ServerConfigurationPacketListenerImpl &&
                            getGameProfileId((it.packetListener as ServerConfigurationPacketListenerImpl).owner) == uuid
                }
                ?.packetListener as? ServerConfigurationPacketListenerImpl)
                ?.startConfiguration()
        }
    }

    private fun getGameProfileId(gameProfile: GameProfile): UUID {
        return if (versionId >= 12109) {
            // record
            dynamic(
                DynamicOpcode.INVOKEVIRTUAL,
                "com.mojang.authlib.GameProfile#id()java.util.UUID;",
                gameProfile
            )
        } else {
            dynamic(
                DynamicOpcode.INVOKEVIRTUAL,
                "com.mojang.authlib.GameProfile#getId()java.util.UUID;",
                gameProfile
            )
        } as UUID
    }

    override fun setHandActive(player: Player, isHandActive: Boolean) {
        val byte = (if (isHandActive) 0x01 else 0).toByte()
        player.sendPacket(
            ClientboundSetEntityDataPacket::class.java.invokeConstructor(
                player.entityId,
                listOf((createByteMeta(8, byte) as SynchedEntityData.DataItem<*>).invokeMethod<Any>("value"))
            )
        )
    }

    @Suppress("SameParameterValue")
    private fun createByteMeta(index: Int, value: Byte): Any {
        return SynchedEntityData.DataItem(EntityDataAccessor(index, EntityDataSerializers.BYTE), value)
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

        // 1.17 加入 changedSlots
        if (isUniversal) {
            items.putAll(event.packet.read<Map<Int, Any>>(changedSlotsField) ?: emptyMap())
        }

        // 1.21.5+
        handleModern(event, items)
    }

    private fun handleModern(event: PacketReceiveEvent, items: MutableMap<Int, Any>) {
        // 替换成代理类
        val changedItems = Int2ObjectArrayMap<HashedStack>(items.size)
        for ((slot, value) in items) {
            if (slot == -10086) {
                val carried = ProxyHashedStack(value as HashedStack, event.player)
                event.packet.write(carriedItemFieldInContainerClick, carried)
            } else {
                changedItems.put(slot, ProxyHashedStack(value as HashedStack, event.player))
            }
        }
        event.packet.write(changedSlotsField, Int2ObjectMaps.unmodifiable(changedItems))
    }

    /**
     * 修改匹配逻辑的 [HashedStack]
     */
    class ProxyHashedStack(val hashedStack: HashedStack, val player: Player) : HashedStack {

        override fun matches(serverItem: ItemStack, hashGenerator: HashedPatchMap.HashGenerator): Boolean {
            // 数量不一样必须同步 (材料不要求)
            if (hashedStack is HashedStack.ActualItem && hashedStack.count != serverItem.count) return false
            var itemToMatch = serverItem

            // 判断是不是本插件的物品
            val bkItem = CraftItemStack.asCraftMirror(itemToMatch)
            if (!bkItem.isNull) {
                itemToMatch = (bkItem.toDisplayMode(player) as CraftItemStack).handle
            }

            // 匹配 (动态修饰的部分不一样也要同步, 不然怎么刷新呢)
            return hashedStack.matches(itemToMatch, hashGenerator)
        }
    }
}