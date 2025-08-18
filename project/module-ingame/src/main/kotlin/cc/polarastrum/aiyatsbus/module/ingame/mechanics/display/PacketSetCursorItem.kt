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

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.NMSItemTag
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.PacketSendEvent

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.listener.packet.PacketSetCursorItem
 *
 * @author xiaozhangup
 * @since 2025/7/2 23:47
 */
object PacketSetCursorItem {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun e(e: PacketSendEvent) {
        // Spigot 与 Paper 同名
        if (e.packet.name == "ClientboundSetCursorItemPacket") {
            val origin = e.packet.read<Any>("contents")!!
            val bkItem = NMSItemTag.asBukkitCopy(origin)
            if (bkItem.isNull) return
            val adapted = NMSItemTag.asNMSCopy(bkItem.toDisplayMode(e.player))
            e.packet.write("contents", adapted)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun e(e: PacketReceiveEvent) {
        val name = e.packet.name
        if (name == "PacketPlayInWindowClick" || name == "ServerboundContainerClickPacket") {
            Aiyatsbus.api().getMinecraftAPI().getPacketHandler().handleContainerClick(e)
        }
    }
}