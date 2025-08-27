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
package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.AiyatsbusMinecraftAPI
import cc.polarastrum.aiyatsbus.core.MinecraftHelper
import cc.polarastrum.aiyatsbus.core.MinecraftItemOperator
import cc.polarastrum.aiyatsbus.core.MinecraftPacketHandler
import cc.polarastrum.aiyatsbus.core.MinecraftWorldOperator
import cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusAPI.Companion.proxy
import cc.polarastrum.aiyatsbus.impl.nmsj21.NMSJ21
import taboolib.module.nms.MinecraftVersion.versionId
import java.util.concurrent.CompletableFuture

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.nms.DefaultAiyatsbusMinecraftAPI
 *
 * @author mical
 * @since 2024/2/18 00:21
 */
class DefaultAiyatsbusMinecraftAPI : AiyatsbusMinecraftAPI {

    val group = "${DefaultAiyatsbusMinecraftAPI::class.java.`package`.name}"

    /** 杂项 */
    val nmsHelper by lazy {
        proxy<MinecraftHelper>("$group.nmsj21.DefaultMinecraftHelper")
    }

    /** 物品操作接口 */
    val nmsItemOperator by lazy {
        proxy<MinecraftItemOperator>("$group.nms.DefaultMinecraftItemOperator")
    }

    /** 数据包控制接口 */
    val nmsPacketHandler by lazy {
        if (versionId >= 12105) {
            proxy<MinecraftPacketHandler>("$group.nmsj21.DefaultMinecraftPacketHandler")
        } else {
            proxy<MinecraftPacketHandler>("$group.nms.DefaultMinecraftPacketHandler")
        }
    }

    /** 世界操作接口 */
    val nmsWorldOperator by lazy {
        proxy<MinecraftWorldOperator>("$group.nms.DefaultMinecraftWorldOperator")
    }

    override fun getHelper(): MinecraftHelper {
        return nmsHelper
    }

    override fun getItemOperator(): MinecraftItemOperator {
        return nmsItemOperator
    }

    override fun getPacketHandler(): MinecraftPacketHandler {
        return nmsPacketHandler
    }

    override fun getWorldOperator(): MinecraftWorldOperator {
        return nmsWorldOperator
    }

    init {
        CompletableFuture.runAsync {
            if (versionId >= 12005) NMSJ21.instance
            nmsHelper
            nmsItemOperator
            nmsPacketHandler
            nmsWorldOperator
        }
    }
}