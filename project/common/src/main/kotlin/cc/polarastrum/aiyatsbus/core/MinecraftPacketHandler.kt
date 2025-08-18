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
package cc.polarastrum.aiyatsbus.core

import taboolib.module.nms.PacketReceiveEvent

/**
 * Minecraft 数据包处理器接口
 *
 * 负责处理网络数据包相关的操作，
 * 提供对 Minecraft 底层数据包事件的处理能力。
 *
 * @author mical
 * @since 2025/8/16 08:45
 */
interface MinecraftPacketHandler {

    /**
     * 处理容器点击数据包
     *
     * 当玩家点击容器（如箱子、背包等）时，
     * 处理相应的数据包事件以进行附魔相关逻辑。
     *
     * @param event 数据包接收事件
     */
    fun handleContainerClick(event: PacketReceiveEvent)
}