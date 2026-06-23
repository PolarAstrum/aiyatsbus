package cc.polarastrum.aiyatsbus.core

import org.bukkit.ChatColor
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * Minecraft 世界操作器接口
 *
 * 提供世界相关的底层操作功能，
 * 包括方块破坏等跨版本兼容的世界交互操作。
 *
 * @author mical
 * @since 2025/8/16 08:44
 */
interface MinecraftWorldOperator {

    /**
     * 破坏方块
     *
     * 取代高版本 player.breakBlock 的函数，会触发 BlockBreakEvent。
     *
     * @param player 玩家
     * @param block 方块
     * @return 是否成功破坏
     */
    fun breakBlock(player: Player, block: Block): Boolean

    fun setEntityGlowing(entity: Entity, receiver: Player, color: ChatColor)

    fun unsetEntityGlowing(entity: Entity, receiver: Player)

    fun setBlockGlowing(block: Block, receiver: Player, color: ChatColor)

    fun unsetBlockGlowing(block: Block, receiver: Player)
}