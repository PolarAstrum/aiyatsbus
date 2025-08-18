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
package cc.polarastrum.aiyatsbus.module.ingame.mechanics.display

import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.NMSItemTag
import taboolib.module.nms.PacketSendEvent

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.listener.packet.PacketWIndowItems
 *
 * @author mical
 * @since 2024/2/18 00:43
 */
object PacketWindowItems {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun e(e: PacketSendEvent) {
        val name = e.packet.name
        if (name == "PacketPlayOutWindowItems" || name == "ClientboundContainerSetContentPacket") {
            val slots = e.packet.read<List<Any>>("items")!!.toMutableList()
            for (i in slots.indices) {
                val bkItem = NMSItemTag.asBukkitCopy(slots[i])
                if (bkItem.isNull) continue
                val nmsItem = NMSItemTag.asNMSCopy(bkItem.toDisplayMode(e.player))
                slots[i] = nmsItem
            }
            e.packet.write("items", slots)

            val cursor = e.packet.read<Any>("carriedItem")!! // carriedItem
            val bkItem = NMSItemTag.asBukkitCopy(cursor)
            if (bkItem.isNull) return
            val nmsItem = NMSItemTag.asNMSCopy(bkItem.toDisplayMode(e.player))
            e.packet.write("carriedItem", nmsItem)
        }
    }
}