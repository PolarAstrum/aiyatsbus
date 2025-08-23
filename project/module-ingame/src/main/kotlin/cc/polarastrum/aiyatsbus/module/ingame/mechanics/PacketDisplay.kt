package cc.polarastrum.aiyatsbus.module.ingame.mechanics

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.toRevertMode
import cc.polarastrum.aiyatsbus.core.util.isNull
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.module.nms.NMSItemTag
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.PacketSendEvent

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.ingame.mechanics.PacketDisplay
 *
 * @author mical
 * @since 2025/8/22 18:33
 */
object PacketDisplay {

    @SubscribeEvent
    fun e(e: PacketSendEvent) {
        when (e.packet.name) {
            "PacketPlayOutOpenWindowMerchant", "ClientboundMerchantOffersPacket" -> handlePacketPlayOutOpenWindowMerchant(e)
            "ClientboundSetPlayerInventoryPacket" -> handlePacketClientboundSetPlayerInventory(e)
            "PacketPlayOutSetSlot", "ClientboundContainerSetSlotPacket" -> handlePacketPlayOutSetSlot(e)
            "PacketPlayOutWindowItems", "ClientboundContainerSetContentPacket" -> handlePacketPlayOutWindowItems(e)
            "ClientboundSetCursorItemPacket" -> handlePacketClientboundSetCursorItem(e)
        }
    }

    @SubscribeEvent
    fun e(e: PacketReceiveEvent) {
        when (e.packet.name) {
            "PacketPlayInSetCreativeSlot", "ServerboundSetCreativeModeSlotPacket" -> handlePacketPlayInSetCreativeSlot(e)
            "PacketPlayInWindowClick", "ServerboundContainerClickPacket" -> Aiyatsbus.api().getMinecraftAPI().getPacketHandler().handleContainerClick(e)
        }
    }

    // Clientbound
    fun handlePacketPlayOutOpenWindowMerchant(e: PacketSendEvent) {
        // 1.16 - 1.20.4 全部版本都可以直接读 b, 1.20.5 改成 c
        Aiyatsbus.api().getMinecraftAPI()
            .getItemOperator()
            .adaptMerchantRecipe(
                e.packet.read<Any>("offers")!!,
                e.player
            )
    }

    fun handlePacketClientboundSetCursorItem(e: PacketSendEvent) {
        e.packet.write("contents", renderItem(e.packet.read("contents")!!, e.player))
    }

    fun handlePacketClientboundSetPlayerInventory(e: PacketSendEvent) {
        e.packet.write("contents", renderItem(e.packet.read("contents")!!, e.player))
    }

    fun handlePacketPlayOutSetSlot(e: PacketSendEvent) {
        e.packet.write("itemStack", renderItem(e.packet.read("itemStack")!!, e.player))
    }

    fun handlePacketPlayOutWindowItems(e: PacketSendEvent) {
        e.packet.write("items", e.packet.read<List<Any>>("items")!!.map { renderItem(it, e.player) })
        e.packet.write("carriedItem", renderItem(e.packet.read("carriedItem")!!, e.player))
    }

    // Serverbound
    fun handlePacketPlayInSetCreativeSlot(e: PacketReceiveEvent) {
        e.packet.write("itemStack", recoverItem(e.packet.read("itemStack")!!, e.player))
    }

    private fun renderItem(item: Any, player: Player): Any {
        val bkItem = Aiyatsbus.api().getMinecraftAPI().getHelper().asCraftMirror(item)
        if (bkItem.isNull) return item
        return bkItem.toDisplayMode(player).getProperty("handle")!!
    }

    private fun recoverItem(item: Any, player: Player): Any {
        val bkItem = Aiyatsbus.api().getMinecraftAPI().getHelper().asCraftMirror(item)
        if (bkItem.isNull) return item
        return bkItem.toRevertMode(player).getProperty("handle")!!
    }
}