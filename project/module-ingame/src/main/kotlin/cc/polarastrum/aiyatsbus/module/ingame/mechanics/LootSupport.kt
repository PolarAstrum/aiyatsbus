@file:Suppress("DuplicatedCode")

package cc.polarastrum.aiyatsbus.module.ingame.mechanics

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.data.CheckType
import cc.polarastrum.aiyatsbus.core.util.MathUtils.preheatExpression
import cc.polarastrum.aiyatsbus.core.util.calcToDouble
import cc.polarastrum.aiyatsbus.core.util.calcToInt
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.world.LootGenerateEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.randomDouble
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.ingame.mechanics.LootSupport
 *
 * @author mical
 * @since 2024/5/1 23:20
 */
@ConfigNode(bind = "core/mechanisms/loot.yml")
object LootSupport {

    @Config("core/mechanisms/loot.yml", autoReload = true)
    lateinit var conf: Configuration
        private set

    @ConfigNode("enable")
    var enable = true

    @ConfigNode("fish_enable")
    var fishEnable = true

    /**
     * 开启悬停显示, 必出悬停原版附魔
     * 关闭时则关闭悬停显示, 一切附魔按权重随机
     */
    @ConfigNode("vanilla_mode")
    var vanillaMode = false

    @delegate:ConfigNode("more_enchant_chance")
    val moreEnchantChance by conversion<List<String>, List<String>> {
        this.onEach { it.preheatExpression() }
    }
    @delegate:ConfigNode("level_formula")
    val levelFormula by conversion<String, String> {
        preheatExpression()
    }
    @ConfigNode("privilege.full_level")
    var fullLevelPrivilege = "aiyatsbus.privilege.table.full"
    @ConfigNode("cost")
    var cost = 3
    @ConfigNode("bonus")
    var bonus = 16

    @delegate:ConfigNode("privilege.chance")
    val moreEnchantPrivilege by conversion<List<String>, Map<String, String>> {
        mapOf(*map { it.split(":")[0] to it.split(":")[1] }.toTypedArray())
    }

    @ConfigNode("max_level_limit")
    var maxLevelLimit = -1

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onLootGenerate(event: LootGenerateEvent) {
        if (!enable) return
        (event.entity as? Player)?.let {
            event.loot.replaceAll { item ->
                if (item.fastFixedEnchants.isNotEmpty()) doEnchant(it, item).second
                else item
            }
        } ?: event.loot.removeIf { it.fastFixedEnchants.isNotEmpty() }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerFish(event: PlayerFishEvent) {
        if (!fishEnable) return
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH || event.caught !is Item) return
        val item = event.caught as Item
        if (item.itemStack.fastFixedEnchants.isNotEmpty()) {
            item.itemStack = doEnchant(event.player, item.itemStack).second
        }
    }

    /**
     * 对物品进行附魔, 并返回新增的附魔列表和结果物品
     */
    private fun doEnchant(
        player: Player,
        originItem: ItemStack,
        cost: Int = this.cost,
        bonus: Int = this.bonus
    ): Pair<Map<AiyatsbusEnchantment, Int>, ItemStack> {
        val item = if (vanillaMode) originItem else ItemStack(originItem.type)
        val enchantsToAdd = mutableMapOf<AiyatsbusEnchantment, Int>()
        val result = item.clone()

        // 额外出的货的数量
        val amount = calculateAmount(player, cost)
        // 选取的附魔范围
        val pool = result.etsAvailable(CheckType.ATTAIN, player).filter { it.alternativeData.isDiscoverable }

        repeat(amount) {
            // 从特定附魔列表中根据品质和附魔的权重抽取一个附魔
            val enchant = pool.drawEt() ?: return@repeat
            val maxLevel = enchant.basicData.maxLevel
            val limit = enchant.alternativeData.getLootMaxLevelLimit(maxLevel, maxLevelLimit)

            val level = if (player.hasPermission(fullLevelPrivilege)) maxLevel else levelFormula.calcToInt(
                "bonus" to bonus,
                "max_level" to limit,
                "button" to cost
            ).coerceIn(1, limit)

            // 如果不与现有附魔冲突就添加
            if (enchant.limitations.checkAvailable(CheckType.ATTAIN, result, player).isSuccess) {
                result.addEt(enchant, level)
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
        return count + if (vanillaMode) 0 else 1
    }

    /**
     * 计算出货数量的概率, 应用特权
     */
    private fun calculateChance(origin: Double, player: Player) = moreEnchantPrivilege.maxOf { (perm, expression) ->
        if (player.hasPermission(perm)) expression.calcToDouble("chance" to origin) else origin
    }.coerceAtLeast(0.0)
}
