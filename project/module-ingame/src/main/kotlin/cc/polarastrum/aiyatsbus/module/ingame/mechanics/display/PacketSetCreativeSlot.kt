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

import cc.polarastrum.aiyatsbus.core.toRevertMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.NMSItemTag
import taboolib.module.nms.PacketReceiveEvent

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.listener.packet.PacketSetCreativeSlot
 *
 * @author mical
 * @since 2024/2/18 00:35
 */
object PacketSetCreativeSlot {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun e(e: PacketReceiveEvent) {
        val name = e.packet.name
        if (name == "PacketPlayInSetCreativeSlot" || name == "ServerboundSetCreativeModeSlotPacket") {
            val origin = e.packet.read<Any>("itemStack")!!
            val bkItem = NMSItemTag.asBukkitCopy(origin)
            if (bkItem.isNull) return
            val adapted = NMSItemTag.asNMSCopy(bkItem.toRevertMode(e.player))
            e.packet.write("itemStack", adapted)
        }
    }
}