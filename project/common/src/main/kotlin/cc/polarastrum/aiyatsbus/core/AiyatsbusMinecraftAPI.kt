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

/**
 * Aiyatsbus Minecraft API 接口
 *
 * 提供与 Minecraft 内部系统的交互接口。
 * 包含物品操作、方块破坏、组件转换等底层功能。
 *
 * @author mical
 * @since 2024/2/18 00:14
 */
interface AiyatsbusMinecraftAPI {

    /**
     * 获取 Minecraft 杂项工具
     *
     * 提供 Minecraft 底层的通用杂项操作工具，包括组件转换等功能。
     *
     * @return Minecraft 杂项工具实例
     */
    fun getHelper(): MinecraftHelper

    /**
     * 获取物品操作器
     *
     * 提供物品相关的底层操作，包括修复成本、物品损坏、物品创建等功能。
     *
     * @return 物品操作器实例
     */
    fun getItemOperator(): MinecraftItemOperator

    /**
     * 获取数据包处理器
     *
     * 负责处理网络数据包相关的操作，包括容器点击事件等。
     *
     * @return 数据包处理器实例
     */
    fun getPacketHandler(): MinecraftPacketHandler

    /**
     * 获取世界操作器
     *
     * 提供世界相关的底层操作，包括方块破坏等功能。
     *
     * @return 世界操作器实例
     */
    fun getWorldOperator(): MinecraftWorldOperator
}