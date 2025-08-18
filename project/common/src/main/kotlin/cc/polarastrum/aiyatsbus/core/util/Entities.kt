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
package cc.polarastrum.aiyatsbus.core.util

import cc.polarastrum.aiyatsbus.core.Aiyatsbus
import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.compat.NPCChecker
import dev.lone.itemsadder.api.CustomBlock
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import kotlin.math.PI

/**
 * 实体工具类
 *
 * 提供实体操作相关的工具函数，包括 NPC 检测、位置判断、伤害处理、方块操作等。
 * 支持 ItemsAdder 插件集成和真实的伤害计算。
 *
 * @author mical
 * @since 2024/7/14 15:30
 */

/**
 * 检查实体是否为 NPC
 *
 * 使用 NPC 检查器判断实体是否为 NPC。
 * 支持多种 NPC 插件的检测，如 Citizens、MythicMobs 等。
 *
 * @return 如果是 NPC 则返回 true
 * 
 * @example
 * ```kotlin
 * // 基本检测
 * if (entity.checkIfIsNPC()) {
 *     // 实体是 NPC，跳过处理
 *     return
 * }
 * 
 * // 在事件中使用
 * @EventHandler
 * fun onEntityDamage(event: EntityDamageEvent) {
 *     if (event.entity.checkIfIsNPC()) {
 *         event.isCancelled = true
 *     }
 * }
 * 
 * // 批量检查
 * world.entities.filter { !it.checkIfIsNPC() }.forEach { entity ->
 *     // 只处理真实玩家和生物
 * }
 * ```
 */
fun Entity?.checkIfIsNPC(): Boolean {
    return NPCChecker.checkIfIsNPC(this ?: return false)
}

/**
 * 判断实体是否在另一个实体后方
 *
 * 通过角度计算判断一个实体是否在另一个实体的后方（120 度范围内）。
 * 用于背刺、偷袭等游戏机制。
 *
 * @param entity 目标实体
 * @return 如果在后方则返回 true
 * 
 * @example
 * ```kotlin
 * // 基本检测
 * if (player.isBehind(target)) {
 *     // 玩家在目标后方，触发背刺效果
 *     player.sendMessage("背刺成功！")
 *     target.realDamage(10.0, player)
 * }
 * 
 * // 在 PvP 系统中使用
 * @EventHandler
 * fun onPlayerAttack(event: EntityDamageByEntityEvent) {
 *     if (event.damager is Player && event.entity is Player) {
 *         val attacker = event.damager as Player
 *         val victim = event.entity as Player
 *         
 *         if (attacker.isBehind(victim)) {
 *             // 背刺攻击，增加伤害
 *             event.damage *= 1.5
 *         }
 *     }
 * }
 * ```
 */
fun LivingEntity.isBehind(entity: LivingEntity): Boolean {
    if (world.name != entity.world.name) {
        return false
    }
    val directionSelf = entity.location.clone().subtract(location).toVector()
    directionSelf.setY(0)
    val direction = entity.location.direction
    direction.setY(0)
    return directionSelf.angle(direction) < PI / 3
}

/**
 * 获取实体装备的物品
 *
 * 获取生物实体所有装备槽位的物品。
 * 支持头部、胸部、腿部、脚部、主手、副手等所有装备槽位。
 *
 * @return 装备槽位到物品的映射表
 * 
 * @example
 * ```kotlin
 * // 获取所有装备
 * val equipment = entity.equippedItems
 * val helmet = equipment[EquipmentSlot.HEAD]
 * val chestplate = equipment[EquipmentSlot.CHEST]
 * 
 * // 检查特定装备
 * if (equipment[EquipmentSlot.HEAD]?.type == Material.DIAMOND_HELMET) {
 *     // 实体戴着钻石头盔
 * }
 * 
 * // 统计装备数量
 * val equippedCount = equipment.values.count { !it.isNull }
 * println("实体装备了 $equippedCount 件物品")
 * ```
 */
val Entity.equippedItems: Map<EquipmentSlot, ItemStack>
    get() = if (this is LivingEntity) EquipmentSlot.values()
        .associateWith { (equipment?.getItem(it) ?: ItemStack(Material.AIR)) } else emptyMap()

/**
 * 对生物实体造成真实伤害
 *
 * 造成无法被插件和原版减伤的真实伤害。
 * 绕过护甲、保护附魔等减伤机制。
 *
 * @param amount 伤害值
 * @param who 伤害来源实体
 * 
 * @example
 * ```kotlin
 * // 基本用法
 * player.realDamage(10.0, attacker)
 * 
 * // 在技能系统中使用
 * fun executeSkill(caster: Player, target: LivingEntity) {
 *     val damage = calculateSkillDamage(caster)
 *     target.realDamage(damage, caster)
 *     caster.sendMessage("技能造成 $damage 点真实伤害")
 * }
 * 
 * // 用于特殊武器
 * fun onWeaponHit(weapon: ItemStack, attacker: Player, target: LivingEntity) {
 *     if (weapon.hasEnchantment(Enchantment.DAMAGE_ALL)) {
 *         // 真实伤害武器
 *         target.realDamage(5.0, attacker)
 *     }
 * }
 * ```
 */
fun LivingEntity.realDamage(amount: Double, who: Entity? = null) {
    health = maxOf(0.1, health - amount + 0.5)
    damage(0.5, who)
    if (isDead) health = 0.0
}

/**
 * 令玩家放置方块
 *
 * 模拟玩家放置方块的操作，触发相应事件。
 * 支持事件监听和取消机制。
 *
 * @param placedBlock 要放置的方块
 * @param itemInHand 手中的物品
 * @return 如果放置成功则返回 true
 * 
 * @example
 * ```kotlin
 * // 基本用法
 * val success = player.placeBlock(block, item)
 * if (success) {
 *     player.sendMessage("方块放置成功")
 * } else {
 *     player.sendMessage("方块放置被取消")
 * }
 * 
 * // 在插件中使用
 * fun placeStructure(player: Player, structure: List<Block>) {
 *     structure.forEach { block ->
 *         if (!player.placeBlock(block)) {
 *             player.sendMessage("结构放置失败")
 *             return
 *         }
 *     }
 *     player.sendMessage("结构放置完成")
 * }
 * ```
 */
fun Player.placeBlock(placedBlock: Block, itemInHand: ItemStack = this.itemInHand): Boolean {
    val blockAgainst = placedBlock.getRelative(0, 1, 0)
    val event = BlockPlaceEvent(placedBlock, placedBlock.state, blockAgainst, itemInHand, this, true)
    return event.callEvent()
}

/**
 * 令玩家破坏方块
 *
 * 处理玩家破坏方块的逻辑，包括 ItemsAdder 支持。
 * 支持自定义方块的特殊掉落和破坏效果。
 *
 * @param block 要破坏的方块
 * 
 * @example
 * ```kotlin
 * // 基本用法
 * player.doBreakBlock(block)
 * 
 * // 在工具系统中使用
 * fun onToolUse(player: Player, block: Block, tool: ItemStack) {
 *     if (tool.hasEnchantment(Enchantment.DIG_SPEED)) {
 *         // 快速挖掘工具
 *         player.doBreakBlock(block)
 *         tool.damage += 1
 *     }
 * }
 * 
 * // 批量破坏
 * fun breakArea(player: Player, blocks: List<Block>) {
 *     blocks.forEach { block ->
 *         player.doBreakBlock(block)
 *     }
 * }
 * ```
 */
fun Player.doBreakBlock(block: Block) {
    try {
        block.mark("block-ignored")
        Aiyatsbus.api().getMinecraftAPI().getWorldOperator().breakBlock(this, block)
    } catch (ex: Throwable) {
        ex.printStackTrace()
    } finally {
        if (block.type != Material.AIR) {
            if (AiyatsbusSettings.supportItemsAdder && itemsAdderEnabled) {
                if (CustomBlock.byAlreadyPlaced(block) != null) {
                    CustomBlock.getLoot(block, inventory.itemInMainHand, true).forEach {
                        world.dropItem(block.location, it)
                    }
                    CustomBlock.remove(block.location)
                } else {
                    block.breakNaturally(inventory.itemInMainHand)
                }
            } else {
                block.breakNaturally(inventory.itemInMainHand)
            }
        }
        block.unmark("block-ignored")
    }
}