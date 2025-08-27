@file:Suppress("DuplicatedCode")

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
package cc.polarastrum.aiyatsbus.module.ingame.mechanics

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.util.MathUtils.preheatExpression
import cc.polarastrum.aiyatsbus.core.util.MathUtils.selectByWeight
import cc.polarastrum.aiyatsbus.core.util.calcToDouble
import cc.polarastrum.aiyatsbus.core.util.calcToInt
import cc.polarastrum.aiyatsbus.core.util.serialized
import com.google.common.collect.HashBasedTable
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentOffer
import org.bukkit.entity.Player
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import taboolib.common.LifeCycle
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.submit
import taboolib.common.util.randomDouble
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.PacketSendEvent
import taboolib.module.ui.InventoryViewProxy
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.serializeToByteArray
import java.util.UUID
import kotlin.random.Random

@ConfigNode(bind = "core/mechanisms/enchanting_table.yml")
object EnchantingTableSupport {

    /**
     * 是否为数据驱动附魔
     */
    private val dataDrivenEnchantment = MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_21)

    /**
     * 记录的等级 位置 to 等级
     */
    private val shelfAmount = mutableMapOf<String, Int>()

    /**
     * 记录附魔台三个选项的附魔
     * 位置 to whichButton to (Enchantment to level)
     */
    private val enchantmentOffers = HashBasedTable.create<UUID, String, List<EnchantmentOffer?>>()

    @Config("core/mechanisms/enchanting_table.yml", autoReload = true)
    lateinit var conf: Configuration
        private set

    /**
     * 是否开启从附魔台获得更多附魔
     */
    @ConfigNode("enable")
    var enable = true

    /**
     * 开启悬停显示, 必出悬停原版附魔
     * 关闭时则关闭悬停显示, 一切附魔按权重随机
     *
     * 该选项在 1.21 以上版本无效
     */
    @ConfigNode("vanilla_mode")
    var vanillaMode = false

    /**
     * 出货数量概率
     */
    @delegate:ConfigNode("more_enchant_chance")
    val moreEnchantChance by conversion<List<String>, List<String>> {
        this.onEach { it.preheatExpression() }
    }

    /**
     * 出货等级公示
     */
    @delegate:ConfigNode("level_formula")
    val levelFormula by conversion<String, String> {
        preheatExpression()
    }

    /**
     * 有此权限的玩家附魔必出满级附魔
     */
    @ConfigNode("privilege.full_level")
    var fullLevelPrivilege = "aiyatsbus.privilege.table.full"

    /**
     * 更多附魔数量的特权
     */
    @delegate:ConfigNode("privilege.chance")
    val moreEnchantPrivilege by conversion<List<String>, Map<String, String>> {
        mapOf(*map { it.split(":")[0] to it.split(":")[1] }.toTypedArray())
    }

    /**
     * 出金播报
     */
    @delegate:ConfigNode("celebrate-notice")
    val celebrateNotice by conversion<ConfigurationSection, Map<String, List<String>>>(defaultValue = emptyMap()) {
        mapOf(*getKeys(false).map { it to getStringList(it) }.toTypedArray())
    }

    @ConfigNode("max_level_limit")
    var maxLevelLimit = -1

    init {
        registerLifeCycleTask(LifeCycle.ENABLE) {
            conf.onReload {
                console().sendLang("configuration-reload", conf.file!!.name, 0)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.MONITOR)
    fun e(e: PacketSendEvent) {
        if (!enable || vanillaMode) return
        /**
         * Spigot.Deobf -> PacketPlayerOutWindowData
         * Paper -> ClientboundContainerSetDataPacket
         *
         * NOTICE 2025/01/07 以下注释有误，containerId 实际为自增数值。
         * 由于不确定该内容在未来是否仍具有一定参考价值，该段注释暂时保留。
         * containerId (Enchanting Table):
         * Field:
         *   1.16 -> a
         *   1.17 -> 1.21.1 -> containerId
         *   paper (universal craftbukkit) -> containerId (remap = false)
         * Value:
         *   1.16 ~ 1.20.2 -> 12 https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Inventory?oldid=2765391
         *   1.20.3 ~ 1.21.2 -> 13 (+ Crafter since 23w42a) https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Inventory?oldid=2765394
         * 以上注释有误。
         *
         * id:
         * 1.16 -> b
         * 1.17 ~ 1.21.1 -> id (remap = true)
         * paper (universal craftbukkit) -> id (remap = false)
         *
         * value:
         * 1.16 -> c
         * 1.17 ~ 1.21 -> value (remap = true)
         * paper (universal craftbukkit) -> value (remap = false)
         *
         * https://wiki.vg/Protocol#Set_Container_Property
         */

        // 1.20.4 以上版本不隐藏附魔选项
        if (dataDrivenEnchantment) {
            return
        }
        if (e.packet.name == "PacketPlayOutWindowData" || e.packet.name == "ClientboundContainerSetDataPacket") {
            try {
//                val containerId = e.packet.read<Int>(if (MinecraftVersion.isUniversal) "containerId" else "a", MinecraftVersion.isUniversal)
                if (InventoryViewProxy.getTopInventory(e.player.openInventory).type != InventoryType.ENCHANTING) return
                val id = e.packet.read<Int>(if (MinecraftVersion.isUniversal) "id" else "b", MinecraftVersion.isUniversal)
                if (id in 4..6) {
                    e.packet.write(if (MinecraftVersion.isUniversal) "value" else "c", -1)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

//    private fun checkEnchantingTable(containerId: Int?): Boolean {
//        return MinecraftVersion.versionId <= 12002 && containerId == 12 ||
//                MinecraftVersion.versionId >= 12003 && containerId == 13
//    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun prepareEnchant(event: PrepareItemEnchantEvent) {
        if (!enable)
            return
        val location = event.enchantBlock.location.serialized
        // 记录附魔台的书架等级
        val bonus = event.enchantmentBonus.coerceAtMost(16)
        shelfAmount[location] = bonus
        // 记录附魔台三个附魔选项
        enchantmentOffers.put(event.enchanter.uniqueId, location, event.offers.toList())
        if (dataDrivenEnchantment) {
            // 预先为所有附魔项生成一个附魔
            val enchants = doPrepareEnchant(event.enchanter, event.item, bonus)
            for (i in 0..2) {
                event.offers[i]?.enchantment = enchants[i]!!.first.enchantment
                event.offers[i]?.enchantmentLevel = enchants[i]!!.second
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun doEnchant(event: EnchantItemEvent) {
        if (!enable)
            return

        val location = event.enchantBlock.location.serialized

        val player = event.enchanter
        val item = event.item.clone()
        val cost = event.whichButton() + 1
        val bonus = shelfAmount[location] ?: 1
        val enchantmentOfferHint = enchantmentOffers.get(event.enchanter.uniqueId, location)?.get(event.whichButton()) ?: return

        // 书附魔完变成附魔书
        if (item.type == Material.BOOK) item.type = Material.ENCHANTED_BOOK

        // 首先获取附魔悬停信息上显示的附魔和等级, 并向物品添加, 因为这是必得的附魔
        val enchantmentHintLevel = if (player.hasPermission(fullLevelPrivilege)) {
            enchantmentOfferHint.enchantment.maxLevel
        } else enchantmentOfferHint.enchantmentLevel
        if (vanillaMode || dataDrivenEnchantment) {
            item.addEt(enchantmentOfferHint.enchantment.aiyatsbusEt, enchantmentHintLevel)
        }

        // 附魔
        val result = doEnchant(player, item, cost, bonus)

        // 清空附魔列表
        event.enchantsToAdd.clear()
        // 添加悬停信息上的附魔
        if (vanillaMode || dataDrivenEnchantment) {
            event.enchantsToAdd += enchantmentOfferHint.enchantment to enchantmentHintLevel
        }
        // 添加随机出来的附魔
        event.enchantsToAdd.putAll(result.first.mapKeys { it.key as Enchantment })

        // 对书的附魔，必须手动进行，因为原版处理会掉特殊附魔
        // 也许可以用更好的方法兼容，submit 有一定风险 FIXME
        if (item.type == Material.ENCHANTED_BOOK) {
            submit {
                event.inventory.setItem(0, result.second)
            }
        }

        // 出金播报
        event.enchantsToAdd.forEach { (enchant, level) ->
            val rarity = (enchant as AiyatsbusEnchantment).rarity
            (celebrateNotice[rarity.name] ?: celebrateNotice[rarity.id])?.let { lines ->
                lines.forEach { line ->
                    val type = line.substringBefore(":")
                    onlinePlayers.forEach {
                        val text = it.asLangOrNull(line.substringAfter(":"), event.enchanter.name to "player", enchant.displayName(level, true) to "enchant") ?: return@forEach
                        when (type) {
                            "actionbar" -> it.sendActionBar(text)
                            "message" -> it.sendMessage(text)
                            "title" -> it.sendTitle(text.split(";")[0], text.split(";")[1])
                        }
                    }
                }
            }
        }
    }

    /**
     * 预先生成一个附魔
     */
    private fun doPrepareEnchant(
        player: Player,
        item: ItemStack,
        bonus: Int
    ): Map<Int, Pair<AiyatsbusEnchantment, Int>> {
        fun Collection<AiyatsbusEnchantment>.drawEt(random: Random): AiyatsbusEnchantment? {
            return groupBy { it.rarity }
                .mapValues { (_, v) -> v.sumOf { it.rarity.weight } }
                .selectByWeight(random)
                ?.let { targetRarity ->
                    filter { it.rarity == targetRarity }
                        .associateWith { it.alternativeData.weight }
                        .selectByWeight(random)
                }
        }

        val random = Random(item.serializeToByteArray().sum() + player.world.seed + player.enchantmentSeed )
        val pool = item.etsAvailable(CheckType.ATTAIN, player).filterNot { it.alternativeData.isTreasure }
        val result = LinkedHashMap<Int, Pair<AiyatsbusEnchantment, Int>>()
        for (i in 0..2) {
            // 从特定附魔列表中根据品质和附魔的权重抽取一个附魔
            while (true) {
                val enchant = pool.drawEt(random) ?: continue
                val maxLevel = enchant.basicData.maxLevel
                val limit = enchant.alternativeData.getEnchantMaxLevelLimit(maxLevel, maxLevelLimit)

                val level = if (player.hasPermission(fullLevelPrivilege)) maxLevel else levelFormula.calcToInt(
                    "bonus" to bonus,
                    "max_level" to limit,
                    "button" to i + 1
                ).coerceIn(1, limit)

//                if (result.values.any { it.first == enchant && it.second == level }) {
//                    continue
//                } 原版就会重复

                if (enchant.limitations.checkAvailable(CheckType.ATTAIN, item, player).isFailure) {
                    continue
                }
                result += i to (enchant to level)
                break
            }
        }
        return result
    }

    /**
     * 对物品进行附魔, 并返回新增的附魔列表和结果物品
     */
    private fun doEnchant(
        player: Player,
        item: ItemStack,
        cost: Int,
        bonus: Int
    ): Pair<Map<AiyatsbusEnchantment, Int>, ItemStack> {
        val enchantsToAdd = mutableMapOf<AiyatsbusEnchantment, Int>()
        val result = item.clone()

        // 额外出的货的数量
        val amount = calculateAmount(player, cost)
        // 选取的附魔范围
        val pool = result.etsAvailable(CheckType.ATTAIN, player).filterNot { it.alternativeData.isTreasure }

        repeat(amount) {
            // 从特定附魔列表中根据品质和附魔的权重抽取一个附魔
            val enchant = pool.drawEt() ?: return@repeat
            val maxLevel = enchant.basicData.maxLevel
            val limit = enchant.alternativeData.getEnchantMaxLevelLimit(maxLevel, maxLevelLimit)

            val level = if (player.hasPermission(fullLevelPrivilege)) maxLevel else levelFormula.calcToInt(
                "bonus" to bonus,
                "max_level" to limit,
                "button" to cost
            ).coerceIn(1, limit)

            // 如果不与现有附魔冲突就添加
            if (enchant.limitations.checkAvailable(CheckType.ATTAIN, result, player).isSuccess) {
                result.addEt(enchant)
                enchantsToAdd[enchant] = level
            }
        }

        return enchantsToAdd to result
    }

    /**
     * 计算额外出的货的数量
     */
    private fun calculateAmount(player: Player, cost: Int): Int {
        var count = 0
        for (formula in moreEnchantChance) {
            if (randomDouble() <= calculateChance(formula.calcToDouble("button" to cost), player)) {
                count++
            } else {
                break
            }
        }
        return count + if (vanillaMode || dataDrivenEnchantment) 0 else 1
    }

    /**
     * 计算出货数量的概率, 应用特权
     */
    private fun calculateChance(origin: Double, player: Player) = moreEnchantPrivilege.maxOf { (perm, expression) ->
        if (player.hasPermission(perm)) expression.calcToDouble("chance" to origin) else origin
    }.coerceAtLeast(0.0)
}