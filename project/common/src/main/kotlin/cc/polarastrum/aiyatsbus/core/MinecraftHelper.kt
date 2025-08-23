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

import org.bukkit.inventory.ItemStack

/**
 * Minecraft 杂项工具接口
 *
 * 提供 Minecraft 底层的通用杂项操作工具，
 * 主要包括组件转换等跨版本兼容功能。
 *
 * @author mical
 * @since 2025/8/16 08:42
 */
interface MinecraftHelper {

    /**
     * 将 Json 转成 IChatBaseComponent
     *
     * @param json JSON 字符串
     * @return IChatBaseComponent 对象
     */
    fun componentFromJson(json: String): Any

    /**
     * 将 IChatBaseComponent 转成 Json
     *
     * @param iChatBaseComponent IChatBaseComponent 对象
     * @return JSON 字符串
     */
    fun componentToJson(iChatBaseComponent: Any): String

    /**
     * 将 NMSItemStack 转成 CraftItemStack
     *
     * @param nmsItemStack NMS 的 ItemStack 对象
     * @return CraftItemStack 对象
     */
    fun asCraftMirror(nmsItemStack: Any): ItemStack
}