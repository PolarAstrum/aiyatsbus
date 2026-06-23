package cc.polarastrum.aiyatsbus.core.event

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.PacketReceiveEvent
import taboolib.platform.type.BukkitProxyEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * 表示与 弓 蓄力相关的事件。
 *
 * @author 坏黑
 */
class AiyatsbusBowChargeEvent {

    /**
     * 表示 弓 蓄力准备阶段的事件。
     *
     * @property player 执行蓄力操作的玩家。
     * @property itemStack 正在蓄力的物品堆。
     * @property hand 玩家使用的装备槽位。
     */
    class Prepare(val player: Player, val itemStack: ItemStack, val hand: EquipmentSlot) : BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false

        /**
         * 表示是否允许玩家进行蓄力操作。
         */
        var isAllowed = false

        /**
         * 触发此事件。
         */
        fun fire(): Prepare {
            call()
            return this
        }

        /**
         * 释放蓄力
         */
        fun release() {
            val info = ChargeHandler.chargeInfo[player.name] ?: return
            // 事件
            Released(player, info.itemStack, info.hand, info).call()
            // 结束蓄力
            ChargeHandler.cancel(player)
        }

        /**
         * 打断蓄力
         */
        fun interrupt() {
            val info = ChargeHandler.chargeInfo[player.name] ?: return
            Break(player, info, Break.Reason.DAMAGED, this).call()
            ChargeHandler.cancel(player)
        }
    }

    /**
     * 表示 弓 蓄力释放阶段的事件。
     *
     * @property player 释放蓄力的玩家。
     * @property itemStack 释放蓄力的物品堆。
     * @property chargeInfo 蓄力信息。
     */
    class Released(val player: Player, val itemStack: ItemStack, val hand: EquipmentSlot, val chargeInfo: ChargeInfo) : BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false

        /**
         * 蓄力开始的时间。
         */
        val startTime = chargeInfo.startTime

        /**
         * 蓄力持续的时间。
         */
        val chargeTime = chargeInfo.chargeTime
    }

    /**
     * 表示玩家在蓄力过程中被打断的事件。
     *
     * @property player 被打断蓄力的玩家。
     * @property chargeInfo 蓄力信息。
     * @property reason 打断蓄力的原因。
     * @property source 事件来源。
     */
    class Break(val player: Player, val chargeInfo: ChargeInfo, val reason: Reason, val source: Event?) : BukkitProxyEvent() {

        /**
         * 表示打断蓄力的原因。
         */
        enum class Reason {

            /**
             * 玩家受到伤害而被打断。
             */
            DAMAGED,

            /**
             * 玩家使用技能而被打断。
             */
            SKILL,

            /**
             * 插件主动打断。
             */
            PLUGIN
        }

        /**
         * 蓄力开始的时间。
         */
        val startTime = chargeInfo.startTime

        /**
         * 蓄力持续的时间。
         */
        val chargeTime = chargeInfo.chargeTime
    }

    /**
     * 表示玩家蓄力信息的数据类。
     *
     * @property player 执行蓄力操作的玩家。
     * @property itemStack 正在蓄力的物品堆。
     * @property hand 玩家使用的装备槽位。
     */
    class ChargeInfo(val player: Player, val itemStack: ItemStack, val hand: EquipmentSlot) {

        /**
         * 表示蓄力的开始时间。
         */
        val startTime = System.currentTimeMillis()

        /**
         * 表示蓄力的结束时间。
         */
        var stopTime = -1L

        /**
         * 获取已经蓄力的时间
         */
        val chargeTime: Long
            get() = if (stopTime == -1L) System.currentTimeMillis() - startTime else stopTime - startTime
    }
}

/**
 * @author 坏黑
 * @since 2024/4/15 15:20
 */
object ChargeHandler {

    /**
     * 存玩家的蓄力信息
     * 键为玩家名称，值为对应的 [ChargeInfo] 对象。
     */
    val chargeInfo = ConcurrentHashMap<String, AiyatsbusBowChargeEvent.ChargeInfo>()

    /**
     * 获取指定玩家的蓄力信息。
     *
     * @param player 要获取蓄力信息的玩家
     * @return 玩家的 [ChargeInfo] 对象，如果不存在则返回 null
     */
    operator fun get(player: Player): AiyatsbusBowChargeEvent.ChargeInfo? {
        return chargeInfo[player.name]
    }

    /**
     * 取消指定玩家的蓄力状态。
     *
     * @param player 要取消蓄力状态的玩家
     * @return 如果成功取消蓄力状态则返回 true，否则返回 false
     */
    fun cancel(player: Player): Boolean {
        val info = chargeInfo.remove(player.name)
        if (info != null) {
            info.stopTime = System.currentTimeMillis()
            setHandActive(player, false)
            return true
        }
        return false
    }

    /**
     * 此方法检查玩家手中的弓或弩是否可以进行蓄力操作。它会检查主手和副手的物品，
     * 并触发 AiyatsbusBowChargeEvent.Prepare 事件来确定是否允许蓄力。
     *
     * @param player 要检查的玩家
     * @return 如果玩家手中的弓或弩可以蓄力，则返回 EquipmentSlot.HAND 或 EquipmentSlot.OFF_HAND；否则返回 null
     */
    fun canCharge(player: Player): EquipmentSlot? {
        val itemInMainHand = player.inventory.itemInMainHand
        if (itemInMainHand.type == Material.BOW || itemInMainHand.type == Material.CROSSBOW) {
            if (AiyatsbusBowChargeEvent.Prepare(player, itemInMainHand, EquipmentSlot.HAND).fire().isAllowed) {
                return EquipmentSlot.HAND
            }
        }
        val itemInOffhand = player.inventory.itemInOffHand
        if (itemInOffhand.type == Material.BOW || itemInOffhand.type == Material.CROSSBOW) {
            if (AiyatsbusBowChargeEvent.Prepare(player, itemInOffhand, EquipmentSlot.OFF_HAND).fire().isAllowed) {
                return EquipmentSlot.OFF_HAND
            }
        }
        return null
    }

    /**
     * 更新玩家的手部活动状态
     *
     * @param player 要更新状态的玩家
     * @param isHandActive 是否激活手部状态
     */
    fun setHandActive(player: Player, isHandActive: Boolean) {
//        Adyeshach.api().getMinecraftAPI().getEntityOperator().updateEntityMetadata(
//            playersNearby(player),
//            player.entityId,
//            Adyeshach.api().getMinecraftAPI().getEntityMetadataHandler().buildMetadata(AdyHuman::class.java) {
//                it.isHandActive = isHandActive
//            }
//        )
        Aiyatsbus.api().getMinecraftAPI().getPacketHandler().setHandActive(player, isHandActive)
    }

    /**
     * 获取玩家附近的其他玩家列表
     *
     * @param player 中心玩家
     * @return 距离中心玩家 32 格以内的玩家列表
     */
    private fun playersNearby(player: Player): List<Player> {
        return player.world.players.filter { it.location.distance(player.location) <= 32 }
    }
}

/**
 * @author 坏黑
 */
object ChargeListener {

    /**
     * 处理接收到的数据包事件。
     * 此方法负责处理与蓄力相关的数据包，包括开始蓄力和释放蓄力。
     */
    @SubscribeEvent
    private fun onReceive(e: PacketReceiveEvent) {
        when (e.packet.nameInMojang) {
            // Prepare shoot
            // "PacketPlayInUseItem", "PacketPlayInUseEntity", "PacketPlayInBlockPlace" -> {
            "ServerboundUseItemOnPacket", "ServerboundInteractPacket", "ServerboundUseItemPacket" -> {
                // 手持弓
                val chargeHand = ChargeHandler.canCharge(e.player)
                if (chargeHand != null) {
                    e.isCancelled = true
                    // 设置手部活动状态
                    ChargeHandler.setHandActive(e.player, true)
                    // 储存信息（开始蓄力）
                    ChargeHandler.chargeInfo[e.player.name] = AiyatsbusBowChargeEvent.ChargeInfo(e.player, e.player.inventory.getItem(chargeHand)!!, chargeHand)
                }
            }
            // Release Item
//            "PacketPlayInBlockDig" -> {
            "ServerboundPlayerActionPacket" -> {
                // 判定动作
                if (e.packet.read<Any>("action").toString() == "RELEASE_USE_ITEM") {
                    val info = ChargeHandler.chargeInfo[e.player.name] ?: return
                    e.isCancelled = true
                    // 事件
                    AiyatsbusBowChargeEvent.Released(e.player, info.itemStack, info.hand, info).call()
                    // 结束蓄力
                    ChargeHandler.cancel(e.player)
                }
            }
        }
    }

    /**
     * 被攻击时打断蓄力
     */
    @SubscribeEvent
    private fun onDamaged(e: EntityDamageEvent) {
        val player = e.entity as? Player ?: return
        val info = ChargeHandler[player] ?: return
        if (AiyatsbusBowChargeEvent.Break(player, info, AiyatsbusBowChargeEvent.Break.Reason.DAMAGED, e).call()) {
            ChargeHandler.cancel(player)
        }
    }
}

