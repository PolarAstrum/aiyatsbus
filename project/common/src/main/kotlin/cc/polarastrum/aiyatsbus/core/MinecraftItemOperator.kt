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

    /**
     * 获取物品上的附魔并转换为 Aiyatsbus 附魔映射
     *
     * @param item 目标物品
     * @return Aiyatsbus 附魔到等级的映射
     */
    fun getEnchants(item: ItemStack): Map<AiyatsbusEnchantment, Int>

    /**
     * Array<Array<AiyatsbusEnchantment and Int>>
     */
    fun getFastEnchants(item: ItemStack): Array<Array<Any>>

    /**
     * 获取物品上指定附魔的等级
     *
     * @param item 目标物品
     * @param enchant 要查询的附魔
     * @return 等级，不存在则返回 null
     */
    fun getEnchantLevel(item: ItemStack, enchant: AiyatsbusEnchantment): Int?

    /**
     * 判断物品是否不可损坏
     *
     * @param item 目标物品
     * @return 是否为不可损坏物品
     */
    fun isUnbreakable(item: ItemStack): Boolean

    /**
     * 判断物品是否为空气
     *
     * @param item 目标物品
     * @return 为空或 AIR 时返回 true
     */
    fun isAir(item: ItemStack?): Boolean

    /** 清理物品对应的附魔台工作副本 */
    fun clearEnchantingSession(item: ItemStack)
}
