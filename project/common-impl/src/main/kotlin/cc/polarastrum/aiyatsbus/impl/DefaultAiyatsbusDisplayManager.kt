@file:Suppress("deprecation")

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
package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.registry.Rarity
import cc.polarastrum.aiyatsbus.core.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.console
import taboolib.module.chat.component
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.modifyMeta
import taboolib.platform.util.onlinePlayers
import kotlin.system.measureTimeMillis

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.DefaultAiyatsbusDisplayManager
 *
 * @author mical
 * @since 2024/2/17 22:00
 */
class DefaultAiyatsbusDisplayManager : AiyatsbusDisplayManager {

    override fun getSettings(): AiyatsbusDisplayManager.Settings {
        return AiyatsbusDisplaySettings
    }

    /**
     * 按照配置中设定的品质排行顺序整理附魔
     */
    override fun sortEnchants(enchants: Map<AiyatsbusEnchantment, Int>): LinkedHashMap<AiyatsbusEnchantment, Int> {
        return linkedMapOf(*enchants.toList().sortedBy { (enchant, level) ->
            getSettings().rarityOrder.indexOf(enchant.rarity.id) * 100000 + (if (getSettings().sortByLevel) level else 0)
        }.toTypedArray())
    }

    /**
     * 生成展示物品的 Lore
     */
    private fun generateLore(item: ItemStack? = null, player: Player? = null): List<String> {
        val settings = getSettings()

        /** 判断附魔是否需要在最后单独显示 */
        fun isSpecial(enchant: AiyatsbusEnchantment): Boolean {
            return settings.separateSpecial && !enchant.displayer.isDefaultDisplay()
        }

        // 首先确保物品必须存在
        if (item == null) return emptyList()
        // 整理附魔
        val sortedEnchants = item.fixedEnchants.ifEmpty { return emptyList() }.filter { it.key.displayer.display }.let(::sortEnchants)
        return buildList {
            // 如果合并模式已打开, 且达到附魔数量的最低要求
            if (settings.combine && sortedEnchants.size >= settings.combineMinimal) {
                val combineAmount = settings.combineAmount
                val combineLayout = settings.combineLayout
                // 普通附魔
                val commonEnchants = sortedEnchants.filterNot { isSpecial(it.key) }.toList()
                // 特殊附魔, 需要单独显示的
                val specialEnchants = sortedEnchants.filter { isSpecial(it.key) }.toList()
                // 按照每 combineAmount 处理数量个数据将普通附魔分割, 并分别处理
                commonEnchants.chunked(combineAmount).forEach { chunk ->
                    var layout = combineLayout[chunk.size - 1]
                    chunk.forEachIndexed { index, (enchant, level) ->
                        layout = layout.replace(enchant.displayer.displays(level, player, item, index + 1))
                    }
                    add(layout)
                }
                // 最后再处理特殊附魔, 在末尾添加
                specialEnchants.forEach { (enchant, level) ->
                    add(combineLayout[0].replace(enchant.displayer.displays(level, player, item, 1)))
                }
            } else {
                // 没有打开附魔合并, 直接按照整理好的顺序添加附魔即可, 调用 Displayer 生成附魔的显示
                addAll(sortedEnchants.map { (enchant, level) ->
                    enchant.displayer.display(level, player, item)
                })
            }
        }.flatMap { it.split("\n") } // 处理换行
    }


    override fun display(item: ItemStack, player: Player): ItemStack {
        val settings = getSettings()
        // 如果没有打开附魔显示模块就不显示了
        if (!settings.enable) return item
        // 首先确保物品必须存在
        if (item.isNull) return item

        // 必须 **克隆** 物品, 不得修改原物品
        return item.clone().modifyMeta<ItemMeta> {
            // 如果没有任何附魔则不进行任何处理
            item.fixedEnchants.ifEmpty { return@modifyMeta }
            // 已经展示过了就不再展示 (不是重新展示)
            // NOTICE 从 0.7 版本, 我遵循白熊的嘱托, 废除了 display_mark, 以节省性能
            val loreIndex = this["lore_index", PersistentDataType.INTEGER_ARRAY]
            if (loreIndex != null && loreIndex.isNotEmpty()) {
                return@modifyMeta
            }
            // 注意, 附魔书对应的隐藏附魔 flag 是 HIDE POTION EFFECTS 而不是 HIDE ENCHANTS (1.18- 是这样, 1.19+ 未知)
            if (item.isEnchantedBook)
                if (isBookEnchantsHidden(this)) return@modifyMeta
                else hideBookEnchants(this)
            else
                if (hasItemFlag(ItemFlag.HIDE_ENCHANTS)) return@modifyMeta
                else addItemFlags(ItemFlag.HIDE_ENCHANTS)
            // 记录原始 Lore 在展示后的物品里对应的索引范围
            var firstIndex = 0
            var lastIndex = 0
            // 生成附魔展示 Lore
            val generatedLore = generateLore(item, player)
            // 物品的原始 Lore
            val originLore = lore() ?: emptyList()
            // 获取附魔显示格式
            val loreFormation =
                if (originLore.isNotEmpty()) settings.hasLoreFormation else settings.withoutLoreFormation
            // 处理最终 Lore
            val result = buildList<Component> {
                loreFormation.forEach { line ->
                    when (line) {
                        "{enchant_lore}" -> addAll(
                            generatedLore.toBuiltComponent().map { componentFromRaw(it.toRawMessage()) })
                        "{capability_line}" ->
                            add(
                                componentFromRaw(
                                    settings.capabilityLine
                                        .replace("capability" to item.type.capability - item.fixedEnchants.size)
                                        .component().buildColored().toRawMessage()
                                )
                            )
                        "{item_lore}" -> {
                            // 如果是插入物品原 Lore 就在插入前后记录索引
                            firstIndex = size
                            addAll(originLore)
                            lastIndex = size
                        }
                        else -> add(componentFromRaw(line.component().buildColored().toRawMessage()))
                    }
                }
            }
            // 设置显示 Lore
            lore(result)
            if (item.type == Material.ENCHANTED_BOOK && !hasCustomModelData()) {
                val rarity = item.fixedEnchants.minBy { it.key.rarity.weight }.key.rarity
                if (rarity.isCustomModelBookEnabled) {
                    setCustomModelData(rarity.customModelBook)
                    this["custom_book", PersistentDataType.STRING] = "true" // 记录这物品被打上了自定义模型
                }
            }
            this["lore_index", PersistentDataType.INTEGER_ARRAY] = intArrayOf(firstIndex, lastIndex)
            // 加附魔序列化数据
            // TODO: 尝试减少字符串的拼接与分割操作
            this["enchants_serialized", PersistentDataType.STRING] =
                item.fixedEnchants.map { (enchant, level) -> "${enchant.basicData.id}:$level" }.joinToString("|")
        }
    }

    override fun undisplay(item: ItemStack, player: Player): ItemStack {
        val settings = getSettings()
        // 如果没开启就不处理
        if (!settings.enable) return item
        // 物品为空就不处理
        if (item.isNull) return item
        return item.clone().modifyMeta<ItemMeta> {
            // 判断是不是没有处理过
            val loreIndex = this["lore_index", PersistentDataType.INTEGER_ARRAY]
            if (loreIndex == null || loreIndex.isEmpty()) {
                return@modifyMeta
            }
            removeItemFlags(ItemFlag.HIDE_ENCHANTS)
            if (item.isEnchantedBook) removeBookEnchantsHidden(this)

            // 清理掉打上的自定义模型
            // TODO: 不清楚会不会有清理掉物品原模型数据的 Bug, 后期需要再看看这一块的逻辑
            val custom = this["custom_book", PersistentDataType.STRING] == "true"
            if (custom) setCustomModelData(null)

            // 创造模式的额外处理，需要重新给物品附魔
            // 这是因为创造模式下客户端会重新设置背包物品，而重新设置的物品中非原版附魔会消失
            // 这是由于客户端没有注册更多附魔
            // FIXME: 不知道 1.21 是不是不需要这步啊, 到时候测试一下
            if (player.gameMode == GameMode.CREATIVE) {
                this["enchants_serialized", PersistentDataType.STRING]!!.split("|").forEach { pair ->
                    aiyatsbusEt(pair.split(":")[0])?.let { enchant ->
                        addEt(enchant, pair.split(":")[1].toInt())
                    }
                }
            }
            // 除去 enchant lore
            val (first, last) = loreIndex
            lore(lore()?.subList(first, last))
            remove("enchants_serialized")
            remove("lore_index")
            remove("custom_book")
        }
    }

    /**
     * Spigot: ItemFlag.HIDE_ADDITIONAL_TOOLTIP
     */
    private fun hideBookEnchants(item: ItemMeta) {
        item.addItemFlags(
            if (MinecraftVersion.versionId >= 12005) ItemFlag.valueOf("HIDE_STORED_ENCHANTS")
            else ItemFlag.HIDE_POTION_EFFECTS
        )
    }

    private fun isBookEnchantsHidden(item: ItemMeta): Boolean {
        return item.hasItemFlag(
            if (MinecraftVersion.versionId >= 12005) ItemFlag.valueOf("HIDE_STORED_ENCHANTS")
            else ItemFlag.HIDE_POTION_EFFECTS
        )
    }

    private fun removeBookEnchantsHidden(item: ItemMeta) {
        item.removeItemFlags(
            if (MinecraftVersion.versionId >= 12005) ItemFlag.valueOf("HIDE_STORED_ENCHANTS")
            else ItemFlag.HIDE_POTION_EFFECTS
        )
    }

    private fun componentFromRaw(raw: String) : Component {
        return GsonComponentSerializer.gson().deserialize(raw).decoration(TextDecoration.ITALIC, false)
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusDisplayManager>(DefaultAiyatsbusDisplayManager())
        }
    }

    @ConfigNode(bind = "enchants/display.yml")
    object AiyatsbusDisplaySettings : AiyatsbusDisplayManager.Settings {

        @Config("enchants/display.yml", autoReload = true)
        override lateinit var conf: Configuration

        @ConfigNode("enable")
        override var enable = true

        @ConfigNode("format.default_previous")
        override var defaultPrevious = "{enchant_display_roman}"

        @ConfigNode("format.default_subsequent")
        override var defaultSubsequent = "\n&8| &7{description}"

        @ConfigNode("capability_line")
        override var capabilityLine = "&8| &7附魔词条数空余: &e{capability}"

        @ConfigNode("sort.level")
        override var sortByLevel = true

        @delegate:ConfigNode("sort.rarity.order")
        override val rarityOrder by conversion<List<String>, List<String>> {
            toMutableList().also { it += Rarity.keys.filterNot(this::contains) }
        }

        @ConfigNode("combine.enable")
        override var combine = false

        @ConfigNode("combine.min")
        override var combineMinimal = 8

        @ConfigNode("combine.amount")
        override var combineAmount = 2

        @ConfigNode("combine.layout")
        override var combineLayout = listOf<String>()

        @ConfigNode("combine.separate_special")
        override var separateSpecial = true

        @ConfigNode("lore_formation.has_lore")
        override var hasLoreFormation = listOf<String>()

        @ConfigNode("lore_formation.without_lore")
        override var withoutLoreFormation = listOf<String>()

        @Awake(LifeCycle.ENABLE)
        fun init() {
            conf.onReload {
                measureTimeMillis { onlinePlayers.forEach(Player::updateInventory) }
                    .let { console().sendLang("configuration-reload", conf.file!!.name, it) }
            }
        }
    }
}