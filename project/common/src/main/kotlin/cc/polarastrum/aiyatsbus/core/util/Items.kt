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
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common.util.VariableReader
import taboolib.module.chat.colored
import taboolib.platform.util.isAir
import taboolib.platform.util.modifyMeta

/**
 * 物品工具类
 *
 * 提供物品操作相关的工具函数，包括变量替换、属性获取和修改等。
 * 支持物品名称、描述、耐久度等属性的便捷操作。
 *
 * @author mical
 * @since 2024/2/17 17:07
 */

/**
 * 物品在铁砧上的操作数
 * 
 * 获取或设置物品在铁砧上的修复成本。
 * 修复成本越高，在铁砧上操作时消耗的经验越多。
 * 
 * @example
 * ```kotlin
 * // 获取修复成本
 * val cost = item.repairCost
 * 
 * // 设置修复成本
 * item.repairCost = 5
 * 
 * // 用于限制物品使用次数
 * if (item.repairCost >= 39) {
 *     player.sendMessage("物品已达到最大使用次数")
 * }
 * ```
 */
var ItemStack.repairCost: Int
    get() = Aiyatsbus.api().getMinecraftAPI().getItemOperator().getRepairCost(this)
    set(value) = Aiyatsbus.api().getMinecraftAPI().getItemOperator().setRepairCost(this, value)

/**
 * 批量变量替换
 *
 * 对物品的显示名称和描述进行批量变量替换。
 * 支持多种变量读取器格式，自动应用颜色代码。
 *
 * @param reader 变量读取器，默认为大括号格式
 * @param func 变量替换函数
 * @return 替换后的物品
 * 
 * @example
 * ```kotlin
 * // 基本用法
 * item.variables { if (it == "name") listOf("Sword") else null }
 * 
 * // 使用自定义变量读取器
 * item.variables(VariableReaders.PERCENT) { 
 *     when (it) {
 *         "player" -> listOf(player.name)
 *         "level" -> listOf(player.level.toString())
 *         else -> null
 *     }
 * }
 * 
 * // 批量替换多个变量
 * item.variables { 
 *     when (it) {
 *         "name" -> listOf("Diamond Sword")
 *         "damage" -> listOf("7")
 *         "durability" -> listOf("1561")
 *         else -> null
 *     }
 * }
 * ```
 */
fun ItemStack.variables(reader: VariableReader = VariableReaders.BRACES, func: VariableFunction): ItemStack {
    return modifyMeta<ItemMeta> {
        setDisplayName(displayName.let {
            reader.replaceNested(it) { func.transfer(this)?.firstOrNull() ?: this }.colored()
        })
        lore = lore?.variables(reader, func)?.colored()
    }
}

/**
 * 单键变量替换
 *
 * 使用指定的键值对进行变量替换。
 * 适用于简单的单变量替换场景。
 *
 * @param key 变量键名
 * @param value 替换值集合
 * @param reader 变量读取器，默认为大括号格式
 * @return 替换后的物品
 * 
 * @example
 * ```kotlin
 * // 替换物品名称
 * item.variable("name", listOf("Diamond Sword"))
 * 
 * // 替换描述中的变量
 * item.variable("damage", listOf("7"))
 * 
 * // 使用自定义读取器
 * item.variable("player", listOf(player.name), VariableReaders.PERCENT)
 * ```
 */
fun ItemStack.variable(key: String, value: Collection<String>, reader: VariableReader = VariableReaders.BRACES): ItemStack {
    return variables(reader) { if (it == key) value else null }
}

/**
 * 单例变量替换
 *
 * 对物品进行单例变量替换。
 * 适用于只需要单个替换值的场景。
 *
 * @param reader 变量读取器，默认为大括号格式
 * @param func 单变量替换函数
 * @return 替换后的物品
 * 
 * @example
 * ```kotlin
 * // 基本用法
 * item.singletons { if (it == "name") "Sword" else null }
 * 
 * // 复杂替换逻辑
 * item.singletons { 
 *     when (it) {
 *         "name" -> "Diamond Sword"
 *         "damage" -> "7"
 *         "durability" -> dura.toString()
 *         else -> null
 *     }
 * }
 * ```
 */
fun ItemStack.singletons(reader: VariableReader = VariableReaders.BRACES, func: SingleVariableFunction): ItemStack {
    return variables(reader, func)
}

/**
 * 单键单例变量替换
 *
 * 使用指定的键值对进行单例变量替换。
 * 最简化的变量替换方式。
 *
 * @param key 变量键名
 * @param value 替换值
 * @param reader 变量读取器，默认为大括号格式
 * @return 替换后的物品
 * 
 * @example
 * ```kotlin
 * // 替换物品名称
 * item.singleton("name", "Diamond Sword")
 * 
 * // 替换描述变量
 * item.singleton("damage", "7")
 * 
 * // 使用自定义读取器
 * item.singleton("player", player.name, VariableReaders.PERCENT)
 * ```
 */
fun ItemStack.singleton(key: String, value: String, reader: VariableReader = VariableReaders.BRACES): ItemStack {
    return singletons(reader) { if (it == key) value else null }
}

/**
 * 判断物品是否为 null 或是空气方块
 * 
 * 检查物品是否为空或为空气方块。
 * 用于安全的物品检查，避免空指针异常。
 * 
 * @example
 * ```kotlin
 * // 检查物品是否有效
 * if (!item.isNull) {
 *     // 处理有效物品
 * }
 * 
 * // 在循环中安全处理
 * inventory.forEach { item ->
 *     if (!item.isNull) {
 *         processItem(item)
 *     }
 * }
 * ```
 */
val ItemStack?.isNull get() = this?.isAir ?: true

/**
 * 判断物品是否为附魔书
 * 
 * 检查物品的元数据是否为附魔书类型。
 * 用于区分普通物品和附魔书。
 * 
 * @example
 * ```kotlin
 * // 检查是否为附魔书
 * if (item.isEnchantedBook) {
 *     // 处理附魔书逻辑
 *     val meta = item.itemMeta as EnchantmentStorageMeta
 *     val enchantments = meta.storedEnchants
 * }
 * ```
 */
val ItemStack.isEnchantedBook get() = itemMeta is EnchantmentStorageMeta

/**
 * 获取/修改物品显示名称
 * 
 * 获取或设置物品的自定义显示名称。
 * 支持颜色代码和格式化。
 * 
 * @example
 * ```kotlin
 * // 获取显示名称
 * val displayName = item.name
 * 
 * // 设置显示名称
 * item.name = "&aDiamond Sword"
 * 
 * // 使用变量
 * item.name = "&e{player}'s Sword".replace("player" to player.name)
 * ```
 */
var ItemStack.name
    get() = itemMeta?.displayName
    set(value) {
        modifyMeta<ItemMeta> { setDisplayName(value) }
    }

/**
 * 获取/修改物品耐久
 * 
 * 获取或设置物品的损坏值。
 * 损坏值越高，物品越接近损坏。
 * 
 * @example
 * ```kotlin
 * // 获取损坏值
 * val damage = item.damage
 * 
 * // 设置损坏值
 * item.damage = 100
 * 
 * // 检查物品是否即将损坏
 * if (item.damage >= item.maxDurability - 100) {
 *     player.sendMessage("物品即将损坏！")
 * }
 * ```
 */
var ItemStack.damage
    get() = (itemMeta as? Damageable)?.damage ?: 0
    set(value) {
        modifyMeta<Damageable> { damage = value }
    }

/**
 * 物品最大耐久度
 * 
 * 获取物品类型的最大耐久度。
 * 不同物品类型有不同的最大耐久度。
 * 
 * @example
 * ```kotlin
 * // 获取最大耐久度
 * val maxDurability = item.maxDurability
 * 
 * // 计算耐久度百分比
 * val percentage = (item.dura.toDouble() / item.maxDurability.toDouble()) * 100
 * 
 * // 检查是否为无限耐久物品
 * if (item.maxDurability == 0) {
 *     println("这是无限耐久的物品")
 * }
 * ```
 */
val ItemStack.maxDurability: Int
    get() = this.type.maxDurability.toInt()

/**
 * 物品耐久度
 * 
 * 获取或设置物品的剩余耐久度。
 * 耐久度 = 最大耐久度 - 损坏值。
 * 
 * @example
 * ```kotlin
 * // 获取剩余耐久度
 * val remainingDurability = item.dura
 * 
 * // 设置剩余耐久度
 * item.dura = 1000
 * 
 * // 修复物品
 * item.dura = item.maxDurability
 * 
 * // 检查耐久度状态
 * when {
 *     item.dura <= 0 -> println("物品已损坏")
 *     item.dura < item.maxDurability / 4 -> println("物品耐久度低")
 *     else -> println("物品状态良好")
 * }
 * ```
 */
var ItemStack.dura: Int
    get() = this.maxDurability - damage
    set(value) {
        this.damage = this.maxDurability - value
    }