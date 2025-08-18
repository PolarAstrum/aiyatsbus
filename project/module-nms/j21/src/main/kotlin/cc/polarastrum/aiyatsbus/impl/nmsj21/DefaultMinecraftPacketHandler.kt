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
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import net.minecraft.network.HashedPatchMap
import net.minecraft.network.HashedStack
import net.minecraft.world.item.ItemStack
import org.bukkit.entity.Player
import taboolib.module.nms.MinecraftVersion.isUniversal
import taboolib.module.nms.NMSItemTag
import taboolib.module.nms.PacketReceiveEvent

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftPacketHandler
 *
 * @author mical
 * @since 2025/8/16 09:34
 */
class DefaultMinecraftPacketHandler : MinecraftPacketHandler {

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

        override fun matches(serverItem: ItemStack, hashGenerator: HashedPatchMap.a): Boolean {
            // 数量不一样必须同步 (材料不要求)
            if (hashedStack is HashedStack.a && hashedStack.count != serverItem.count) return false
            var itemToMatch = serverItem

            // 判断是不是本插件的物品
            val bkItem = NMSItemTag.asBukkitCopy(itemToMatch)
            if (!bkItem.isNull) {
                itemToMatch = NMSItemTag.asNMSCopy(bkItem.toDisplayMode(player)) as ItemStack
            }

            // 匹配 (动态修饰的部分不一样也要同步, 不然怎么刷新呢)
            return hashedStack.matches(itemToMatch, hashGenerator)
        }
    }
}