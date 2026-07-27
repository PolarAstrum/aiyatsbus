package cc.polarastrum.aiyatsbus.core

import org.bukkit.entity.Player
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

    /**
     * 同步客户端注册表
     *
     * 在自定义附魔注册后向玩家同步注册表，确保客户端识别自定义附魔。
     *
     * @param player 目标玩家
     */
    fun synchronizeRegistries(player: Player)

    /** 发送手部活动数据 */
    fun setHandActive(player: Player, isHandActive: Boolean)

    /** 发送玩家能力数据包 */
    fun sendPlayerAbilities(player: Player, isCreativeMode: Boolean)

    /** 发送铁砧窗口数据包 */
    fun sendAnvilWindowProperty(player: Player, windowId: Int, cost: Int)
}
