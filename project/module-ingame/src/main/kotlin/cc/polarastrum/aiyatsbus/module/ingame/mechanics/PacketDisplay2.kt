package cc.polarastrum.aiyatsbus.module.ingame.mechanics

import cc.polarastrum.aiyatsbus.core.AiyatsbusDisplayManager
import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.toDisplayMode
import cc.polarastrum.aiyatsbus.core.toRevertMode
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMerchantOffers
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.ingame.mechanics.PacketDisplay2
 *
 * @author mical
 * @since 2026/8/18 00:16
 */
class PacketDisplay2 : PacketListenerAbstract() {

    private fun check(): Boolean {
        val config = AiyatsbusSettings
        if (!config.enablePacketSystem) return false
        if (config.packetSystem != AiyatsbusDisplayManager.PacketSystem.PACKET_EVENTS || ! AiyatsbusDisplayManager.PacketSystem.PACKET_EVENTS.isAvailable) return false
        return true
    }

    override fun onPacketSend(e: PacketSendEvent) {
        if (!check()) return
        val player = e.getPlayer<Player>()
        when (e.packetType) {
            // PacketPlayOutOpenWindowMerchant, ClientboundMerchantOffersPacket
            PacketType.Play.Server.MERCHANT_OFFERS -> handlePacketPlayOutOpenWindowMerchant(player, e)
            // ClientboundSetPlayerInventoryPacket
            PacketType.Play.Server.SET_PLAYER_INVENTORY -> handlePacketClientboundSetPlayerInventory(player, e)
            // PacketPlayOutSetSlot, ClientboundContainerSetSlotPacket
            PacketType.Play.Server.SET_SLOT -> handlePacketPlayOutSetSlot(player, e)
            // PacketPlayOutWindowItems, ClientboundContainerSetContentPacket
            PacketType.Play.Server.WINDOW_ITEMS -> handlePacketPlayOutWindowItems(player, e)
            // ClientboundSetCursorItemPacket
            PacketType.Play.Server.SET_CURSOR_ITEM -> handlePacketClientboundSetCursorItem(player, e)
        }
    }

    override fun onPacketReceive(e: PacketReceiveEvent) {
        val player = e.getPlayer<Player>()
        when (e.packetType) {
            // PacketPlayInSetCreativeSlot, ServerboundSetCreativeModeSlotPacket
            PacketType.Play.Client.CREATIVE_INVENTORY_ACTION -> handlePacketPlayInSetCreativeSlot(player, e)
        }
    }

    // Clientbound
    fun handlePacketPlayOutOpenWindowMerchant(player: Player, e: PacketSendEvent) {
        val packet = WrapperPlayServerMerchantOffers(e)
        packet.merchantOffers.forEach {
            it.firstInputItem = renderItem(it.firstInputItem, player)
            it.secondInputItem = renderItem(it.secondInputItem, player)
            it.outputItem = renderItem(it.outputItem, player)
        }
    }

    fun handlePacketClientboundSetCursorItem(player: Player, e: PacketSendEvent) {
        val packet = WrapperPlayServerSetCursorItem(e)
        packet.stack = renderItem(packet.stack, player)
    }

    fun handlePacketClientboundSetPlayerInventory(player: Player, e: PacketSendEvent) {
        val packet = WrapperPlayServerSetPlayerInventory(e)
        packet.stack = renderItem(packet.stack, player)
    }

    fun handlePacketPlayOutSetSlot(player: Player, e: PacketSendEvent) {
        val packet = WrapperPlayServerSetSlot(e)
        packet.item = renderItem(packet.item, player)
    }

    fun handlePacketPlayOutWindowItems(player: Player, e: PacketSendEvent) {
        val packet = WrapperPlayServerWindowItems(e)
        packet.items = packet.items.map { renderItem(it, player) }
        packet.carriedItem.ifPresent { packet.setCarriedItem(renderItem(it, player)) }
    }

    // Serverbound
    fun handlePacketPlayInSetCreativeSlot(player: Player, e: PacketReceiveEvent) {
        val packet = WrapperPlayClientCreativeInventoryAction(e)
        packet.itemStack = recoverItem(packet.itemStack, player)
    }

    private fun renderItem(item: ItemStack?, player: Player): ItemStack? {
        if (item == null) return null
        return SpigotConversionUtil.fromBukkitItemStack(
            SpigotConversionUtil.toBukkitItemStack(item).toDisplayMode(player)
        )
    }

    private fun recoverItem(item: ItemStack?, player: Player): ItemStack? {
        if (item == null) return null
        return SpigotConversionUtil.fromBukkitItemStack(
            SpigotConversionUtil.toBukkitItemStack(item).toRevertMode(player)
        )
    }
}

@Awake(LifeCycle.ENABLE)
private fun register() {
    try {
        PacketEvents.getAPI().eventManager.registerListener(PacketDisplay2())
    } catch (_: Throwable) {
    }
}