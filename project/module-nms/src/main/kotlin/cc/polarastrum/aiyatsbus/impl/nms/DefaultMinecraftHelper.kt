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
package cc.polarastrum.aiyatsbus.impl.nms

import cc.polarastrum.aiyatsbus.core.MinecraftHelper
import io.papermc.paper.adventure.AdventureComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.network.chat.IChatBaseComponent
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage
import org.bukkit.inventory.ItemStack
import taboolib.module.nms.MinecraftVersion.isUniversalCraftBukkit
import taboolib.module.nms.MinecraftVersion.versionId

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.impl.nms.nms.DefaultMinecraftHelper
 *
 * @author mical
 * @since 2025/8/16 08:50
 */
class DefaultMinecraftHelper : MinecraftHelper {

    override fun componentFromJson(json: String): Any {
        return CraftChatMessage.fromJSON(json)
    }

    override fun componentToJson(iChatBaseComponent: Any): String {
        // 逆天 paper 1.21.4+
        if (isUniversalCraftBukkit && versionId >= 12104) {
            if (iChatBaseComponent is AdventureComponent) {
                return GsonComponentSerializer.gson().serialize(iChatBaseComponent.`adventure$component`())
            }
        }
        return CraftChatMessage.toJSON(iChatBaseComponent as IChatBaseComponent)
    }

    override fun asCraftMirror(nmsItemStack: Any): ItemStack {
        return CraftItemStack.asCraftMirror(nmsItemStack as NMSItemStack)
    }
}