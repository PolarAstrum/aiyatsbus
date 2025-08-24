package cc.polarastrum.aiyatsbus.impl.nmsj21

import cc.polarastrum.aiyatsbus.core.MinecraftHelper
import cc.polarastrum.aiyatsbus.impl.nms.NMSItemStack
import io.papermc.paper.adventure.AdventureComponent
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.network.chat.IChatBaseComponent
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_20_R3.util.CraftChatMessage
import org.bukkit.inventory.ItemStack
import taboolib.module.nms.MinecraftVersion

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
        if (MinecraftVersion.isUniversalCraftBukkit && MinecraftVersion.versionId >= 12104) {
            if (iChatBaseComponent is AdventureComponent) {
                return GsonComponentSerializer.gson().serialize(iChatBaseComponent.`adventure$component`())
            }
        }
        return CraftChatMessage.toJSON(iChatBaseComponent as IChatBaseComponent)
    }

    override fun asCraftMirror(nmsItemStack: Any): ItemStack {
        return CraftItemStack.asCraftMirror(nmsItemStack as NMSItemStack)
    }

    override fun getCraftItemStackHandle(item: ItemStack): Any {
        return (item as CraftItemStack).handle
    }
}