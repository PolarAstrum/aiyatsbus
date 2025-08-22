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

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Minecraft 物品操作器接口
 *
 * 提供物品相关的底层操作功能，
 * 包括修复成本、物品损坏、物品创建、商人配方适配等跨版本兼容操作。
 *
 * @author mical
 * @since 2025/8/16 08:43
 */
interface MinecraftItemOperator {

    /**
     * 获取物品在铁砧上的操作数
     *
     * @param item 物品
     * @return 操作数
     */
    fun getRepairCost(item: ItemStack): Int

    /**
     * 设置物品在铁砧上的操作数
     *
     * @param item 物品
     * @param cost 操作数
     */
    fun setRepairCost(item: ItemStack, cost: Int) : ItemStack

    /**
     * 创建物品堆栈
     *
     * 1.18.2 以下版本（不包含 1.18.2）中 ItemFactory#createItemStack 不存在，
     * 此函数用以替代。
     *
     * @param material 材料名称
     * @param tag 标签
     * @return 物品堆栈
     * @throws IllegalStateException 如果创建失败
     */
    @Throws(IllegalStateException::class)
    fun createItemStack(material: String, tag: String?): ItemStack

    /**
     * 损坏物品堆栈
     *
     * 取代高版本 ItemStack#damage。
     *
     * @param item 物品堆栈
     * @param amount 损坏量
     * @param entity 实体
     * @return 损坏后的物品堆栈
     */
    fun damageItemStack(item: ItemStack, amount: Int, entity: LivingEntity): ItemStack

    /**
     * 为原版的 MerchantRecipeList 的物品显示更多附魔
     *
     * @param merchantRecipeList 商人配方列表
     * @param player 玩家
     */
    fun adaptMerchantRecipe(merchantRecipeList: Any, player: Player)
}