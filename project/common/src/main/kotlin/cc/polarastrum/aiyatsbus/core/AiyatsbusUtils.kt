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

import cc.polarastrum.aiyatsbus.core.data.*
import cc.polarastrum.aiyatsbus.core.data.registry.*
import cc.polarastrum.aiyatsbus.core.data.registry.Target
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common5.RandomList
import taboolib.platform.util.modifyMeta

/**
 * Aiyatsbus 工具类
 *
 * 提供 Aiyatsbus 插件的各种实用工具函数和扩展方法。
 * 包含语言系统、附魔管理、物品操作、玩家数据等功能的便捷访问方法。
 * 所有函数都通过扩展方法的形式提供，便于在代码中使用。
 *
 * @author mical
 * @since 2024/2/17 22:12
 */

/**
 * 使用 AiyatsbusLanguage 发送语言文件
 *
 * 向命令发送者发送指定节点的语言文本，支持参数替换。
 *
 * @param node 语言节点名称
 * @param args 要替换的参数，格式为 key to value
 * @example
 * ```kotlin
 * player.sendLang("enchant-success", "enchant" to "锋利", "level" to 5)
 * ```
 */
fun CommandSender.sendLang(node: String, vararg args: Any) {
    Aiyatsbus.api().getLanguage().sendLang(this, node, *args)
}

/**
 * 使用 AiyatsbusLanguage 获取语言文件（可空版本）
 *
 * 获取指定节点的语言文本，如果节点不存在则返回 null。
 *
 * @param node 语言节点名称
 * @param args 要替换的参数
 * @return 语言文本，如果节点不存在则返回 null
 */
fun CommandSender.asLangOrNull(node: String, vararg args: Any): String? {
    return Aiyatsbus.api().getLanguage().getLangOrNull(this, node, *args)
}

/**
 * 使用 AiyatsbusLanguage 获取语言文件
 *
 * 获取指定节点的语言文本，如果节点不存在则返回节点名称。
 *
 * @param node 语言节点名称
 * @param args 要替换的参数
 * @return 语言文本
 */
fun CommandSender.asLang(node: String, vararg args: Any): String {
    return Aiyatsbus.api().getLanguage().getLang(this, node, *args)
}

/**
 * 使用 AiyatsbusLanguage 获取语言文件列表
 *
 * 获取指定节点的语言文本列表，通常用于多行文本。
 *
 * @param node 语言节点名称
 * @param args 要替换的参数
 * @return 语言文本列表
 */
fun CommandSender.asLangList(node: String, vararg args: Any): List<String> {
    return Aiyatsbus.api().getLanguage().getLangList(this, node, *args)
}

/**
 * 使用 AiyatsbusLanguage 发送语言文件（代理命令发送者版本）
 *
 * @param node 语言节点名称
 * @param args 要替换的参数
 */
fun ProxyCommandSender.sendLang(node: String, vararg args: Any) {
    cast<CommandSender>().sendLang(node, *args)
}

/**
 * 使用 AiyatsbusLanguage 获取语言文件（可空版本，代理命令发送者版本）
 *
 * @param node 语言节点名称
 * @param args 要替换的参数
 * @return 语言文本，如果节点不存在则返回 null
 */
fun ProxyCommandSender.asLangOrNull(node: String, vararg args: Any): String? {
    return cast<CommandSender>().asLangOrNull(node, *args)
}

/**
 * 使用 AiyatsbusLanguage 获取语言文件（代理命令发送者版本）
 *
 * @param node 语言节点名称
 * @param args 要替换的参数
 * @return 语言文本
 */
fun ProxyCommandSender.asLang(node: String, vararg args: Any): String {
    return cast<CommandSender>().asLang(node, *args)
}

/**
 * 使用 AiyatsbusLanguage 获取语言文件列表（代理命令发送者版本）
 *
 * @param node 语言节点名称
 * @param args 要替换的参数
 * @return 语言文本列表
 */
fun ProxyCommandSender.asLangList(node: String, vararg args: Any): List<String> {
    return cast<CommandSender>().asLangList(node, *args)
}

/**
 * 将 Enchantment 转换为 AiyatsbusEnchantment
 *
 * 奇妙的 Et 命名来自白熊，是 enchant 的缩写。
 * 将 Bukkit 的 Enchantment 对象转换为 Aiyatsbus 的附魔对象。
 *
 * @return AiyatsbusEnchantment 对象
 * @throws IllegalStateException 如果附魔未找到（可能是原版附魔，请确保所有原版附魔文件完整）
 */
val Enchantment.aiyatsbusEt: AiyatsbusEnchantment
    get() = Aiyatsbus.api().getEnchantmentManager().getEnchant(key) ?: throw IllegalStateException("Enchantment ${key.key} not found. (Maybe it's a vanilla enchantment. Please ensure all of the vanilla enchantment's files are complete.)")

/**
 * 发送调试信息
 */
fun sendDebug(message: String) {
    if (AiyatsbusSettings.debug) {
        AiyatsbusSettings.debugUsers.mapNotNull(::getProxyPlayer).forEach { t -> t.sendMessage(message) }
    }
}

/**
 * 根据名称或 Key 获取 AiyatsbusEnchantment 附魔对象
 *
 * 优先通过名称查找，如果找不到则通过 Key 查找。
 *
 * @param identifier 附魔标识符（名称或 Key）
 * @return AiyatsbusEnchantment 对象，如果未找到则返回 null
 */
fun aiyatsbusEt(identifier: String) = with(Aiyatsbus.api().getEnchantmentManager()) {
    getByName(identifier) ?: getEnchant(identifier)
}

/**
 * 根据 Key 获取 AiyatsbusEnchantment 附魔对象
 *
 * @param key 附魔的 NamespacedKey
 * @return AiyatsbusEnchantment 对象，如果未找到则返回 null
 */
fun aiyatsbusEt(key: NamespacedKey) = aiyatsbusEt(key.key)

/**
 * 获取某个品质对应的所有 AiyatsbusEnchantment 附魔
 *
 * @param rarity 附魔品质
 * @return 该品质下的所有附魔列表
 */
fun aiyatsbusEts(rarity: Rarity) = Aiyatsbus.api().getEnchantmentManager().getEnchants().values.filter { it.rarity == rarity }

/**
 * 将物品转换为显示模式
 *
 * 将物品转换为适合显示的格式，包含自定义名称、描述等。
 *
 * @param player 查看物品的玩家
 * @return 显示模式下的物品
 */
fun ItemStack.toDisplayMode(player: Player): ItemStack {
    return Aiyatsbus.api().getDisplayManager().display(this, player)
}

/**
 * 将物品转换为还原模式
 *
 * 将显示模式的物品还原为原始格式。
 *
 * @param player 查看物品的玩家
 * @return 还原模式下的物品
 */
fun ItemStack.toRevertMode(player: Player): ItemStack {
    return Aiyatsbus.api().getDisplayManager().undisplay(this, player)
}

/**
 * 获取附魔的附魔书
 *
 * 创建一个包含指定附魔的附魔书物品。
 *
 * @param level 附魔等级，默认为附魔的最大等级
 * @return 包含指定附魔的附魔书
 */
fun AiyatsbusEnchantment.book(level: Int = basicData.maxLevel) = ItemStack(Material.ENCHANTED_BOOK).modifyMeta<ItemMeta> { addEt(this@book, level) }

/**
 * 获取附魔的附魔书（Enchantment 版本）
 *
 * @param level 附魔等级，默认为附魔的最大等级
 * @return 包含指定附魔的附魔书
 */
fun Enchantment.book(level: Int = maxLevel) = (this as AiyatsbusEnchantment).book(level)

/**
 * 获取该物品可用的所有附魔
 *
 * 根据检查类型和玩家权限，获取物品可以获得的附魔列表。
 *
 * @param checkType 检查类型，默认为铁砧合成
 * @param player 玩家，用于权限检查，默认为 null
 * @return 可用的附魔列表
 */
fun ItemStack.etsAvailable(
    checkType: CheckType = CheckType.ANVIL,
    player: Player? = null
): List<AiyatsbusEnchantment> = Aiyatsbus.api().getEnchantmentManager().getEnchants().values.filter { !it.inaccessible && it.limitations.checkAvailable(checkType, this, player).isSuccess }

/**
 * 从特定品质中根据附魔权重抽取一个附魔
 *
 * 根据附魔的权重进行随机抽取。
 *
 * @return 随机抽取的附魔，如果没有该品质的附魔则返回 null
 */
fun Rarity.drawEt(): AiyatsbusEnchantment? = RandomList(*aiyatsbusEts(this).associateWith { it.alternativeData.weight }.toList().toTypedArray()).random()

/**
 * 从物品元数据获取附魔并自动转换为 AiyatsbusEnchantment
 *
 * 获取物品上的所有附魔，并自动转换为 AiyatsbusEnchantment 格式。
 * 支持附魔书和普通物品。
 */
var ItemMeta.fixedEnchants: Map<AiyatsbusEnchantment, Int>
    get() = (if (this is EnchantmentStorageMeta) storedEnchants else enchants).map { (enchant, level) -> enchant.aiyatsbusEt to level }.toMap()
    set(value) {
        clearEts()
        if (this is EnchantmentStorageMeta) value.forEach { (enchant, level) -> addStoredEnchant(enchant.enchantment, level, true) }
        else value.forEach { (enchant, level) -> addEnchant(enchant.enchantment, level, true) }
    }

/**
 * 从物品获取附魔并自动转换为 AiyatsbusEnchantment
 *
 * 获取物品上的所有附魔，并自动转换为 AiyatsbusEnchantment 格式。
 */
var ItemStack?.fixedEnchants: Map<AiyatsbusEnchantment, Int>
    get() = this?.itemMeta?.fixedEnchants ?: emptyMap()
    set(value) { this?.modifyMeta<ItemMeta> { fixedEnchants = value } }

/**
 * 获取附魔等级，若不存在则为 -1
 *
 * @param enchant 要查询的附魔
 * @return 附魔等级，如果不存在则返回 -1
 */
fun ItemMeta.etLevel(enchant: AiyatsbusEnchantment): Int {
    return fixedEnchants[enchant.enchantment as AiyatsbusEnchantment] ?: -1
}

/**
 * 获取附魔等级，若不存在则为 -1
 *
 * @param enchant 要查询的附魔
 * @return 附魔等级，如果不存在则返回 -1
 */
fun ItemStack.etLevel(enchant: AiyatsbusEnchantment) = itemMeta.etLevel(enchant)

/**
 * 添加附魔
 *
 * 向物品元数据添加指定附魔，如果已存在则先移除再添加。
 *
 * @param enchant 要添加的附魔
 * @param level 附魔等级，默认为附魔的最大等级
 */
fun ItemMeta.addEt(enchant: AiyatsbusEnchantment, level: Int = enchant.basicData.maxLevel) {
    removeEt(enchant)
    if (this is EnchantmentStorageMeta) addStoredEnchant(enchant.enchantment, level, true)
    else addEnchant(enchant.enchantment, level, true)
}

/**
 * 添加附魔
 *
 * 向物品添加指定附魔。
 *
 * @param enchant 要添加的附魔
 * @param level 附魔等级，默认为附魔的最大等级
 */
fun ItemStack.addEt(enchant: AiyatsbusEnchantment, level: Int = enchant.basicData.maxLevel) {
    modifyMeta<ItemMeta> { addEt(enchant, level) }
}

/**
 * 删除附魔
 *
 * 从物品元数据中移除指定附魔。
 *
 * @param enchant 要删除的附魔
 */
fun ItemMeta.removeEt(enchant: AiyatsbusEnchantment) {
    if (this is EnchantmentStorageMeta) removeStoredEnchant(enchant.enchantment)
    else removeEnchant(enchant.enchantment)
}

/**
 * 删除附魔
 *
 * 从物品中移除指定附魔。
 *
 * @param enchant 要删除的附魔
 */
fun ItemStack.removeEt(enchant: AiyatsbusEnchantment) {
    modifyMeta<ItemMeta> { removeEt(enchant) }
}

/**
 * 清除物品的所有附魔
 *
 * 移除物品元数据上的所有附魔。
 */
fun ItemMeta.clearEts() {
    if (this is EnchantmentStorageMeta) storedEnchants.forEach { removeStoredEnchant(it.key) }
    else enchants.forEach { removeEnchant(it.key) }
}

/**
 * 清除物品的所有附魔
 *
 * 移除物品上的所有附魔。
 */
fun ItemStack.clearEts() {
    modifyMeta<ItemMeta> { clearEts() }
}

/**
 * 从特定附魔列表中根据品质和附魔的权重抽取一个附魔
 *
 * 先根据品质权重抽取品质，再从该品质中根据附魔权重抽取附魔。
 *
 * @return 随机抽取的附魔，如果没有可用附魔则返回 null
 */
fun Collection<AiyatsbusEnchantment>.drawEt(): AiyatsbusEnchantment? {
    val rarity = RandomList(*associate { it.rarity to it.rarity.weight }.toList().toTypedArray()).random()
    return RandomList(*filter { rarity == it.rarity }.associateWith { it.alternativeData.weight }.toList().toTypedArray()).random()
}

/**
 * 检查某个物品是否属于某一类物品
 *
 * @param target 目标类型
 * @return 如果物品属于该目标类型则返回 true
 */
fun Material.isInTarget(target: Target?): Boolean = target?.types?.contains(this) ?: false

/**
 * 获取这个物品所属类别
 *
 * @return 物品所属的所有目标类型列表
 */
val Material.belongedTargets get() = Target.values.filter(::isInTarget)

/**
 * 获取这个物品所能附魔的最大附魔词条数
 *
 * @return 最大附魔数量，默认为 32
 */
val Material.capability get() = belongedTargets.minOfOrNull { it.capability } ?: 32

/**
 * 检查附魔是否处于某个分组
 *
 * @param name 分组名称
 * @return 如果附魔属于该分组则返回 true
 */
fun Enchantment.isInGroup(name: String): Boolean = isInGroup(aiyatsbusGroup(name))

/**
 * 检查附魔是否处于某个分组
 *
 * @param group 分组对象
 * @return 如果附魔属于该分组则返回 true
 */
fun Enchantment.isInGroup(group: Group?): Boolean = group?.enchantments?.find { it.enchantmentKey == key } != null

/**
 * 玩家的菜单类型
 *
 * 获取或设置玩家的菜单显示模式。
 */
var Player.menuMode
    get() = Aiyatsbus.api().getPlayerDataHandler().get(this).menuMode
    set(value) {
        Aiyatsbus.api().getPlayerDataHandler().get(this).menuMode = value
    }

/**
 * 玩家的收藏夹
 *
 * 获取玩家的附魔收藏列表。
 */
val Player.favorites get() = Aiyatsbus.api().getPlayerDataHandler().get(this).favorites

/**
 * 玩家的过滤器
 *
 * 获取玩家的附魔过滤器设置。
 */
val Player.filters get() = Aiyatsbus.api().getPlayerDataHandler().get(this).filters

/**
 * 玩家的冷却列表
 *
 * 获取玩家的附魔冷却时间列表。
 */
val Player.cooldown get() = Aiyatsbus.api().getPlayerDataHandler().get(this).cooldown

/**
 * 获取分组
 *
 * 根据标识符获取附魔分组。
 *
 * @param identifier 分组标识符
 * @return 分组对象，如果未找到则返回 null
 */
fun aiyatsbusGroup(identifier: String): Group? = Group[identifier]

/**
 * 获取品质
 *
 * 根据标识符获取附魔品质。
 *
 * @param identifier 品质标识符
 * @return 品质对象，如果未找到则返回 null
 */
fun aiyatsbusRarity(identifier: String): Rarity? = Rarity[identifier] ?: Rarity.values.firstOrNull { it.name == identifier }

/**
 * 获取目标类型
 *
 * 根据标识符获取附魔目标类型。
 *
 * @param identifier 目标类型标识符
 * @return 目标类型对象，如果未找到则返回 null
 */
fun aiyatsbusTarget(identifier: String): Target? = Target[identifier] ?: Target.values.firstOrNull { it.name == identifier }