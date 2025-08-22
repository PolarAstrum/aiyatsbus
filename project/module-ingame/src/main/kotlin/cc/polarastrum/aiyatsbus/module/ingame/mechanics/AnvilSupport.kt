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
import cc.polarastrum.aiyatsbus.core.event.AiyatsbusPrepareAnvilEvent
import cc.polarastrum.aiyatsbus.core.util.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import taboolib.common.LifeCycle
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.module.chat.uncolored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.conversion
import taboolib.platform.util.modifyMeta
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.ingame.mechanics.AnvilSupport
 *
 * @author mical
 * @since 2024/5/1 23:20
 */
@ConfigNode(bind = "core/mechanisms/anvil.yml")
object AnvilSupport {

    @Config("core/mechanisms/anvil.yml", autoReload = true)
    lateinit var conf: Configuration
        private set

    @ConfigNode("use_rework_penalty")
    var useReworkPenalty = false

    @ConfigNode("rework_penalty")
    var reworkPenalty = "({repairCost}+1)*2-1"

    @ConfigNode("limit.unsafe_level")
    var allowUnsafeLevel = true

    @ConfigNode("limit.block_on_unsafe_level")
    var blockOnUnsafeLevel = true

    @ConfigNode("limit.unsafe_combine")
    var allowUnsafeCombine = false

    @ConfigNode("limit.enchant")
    var enchantLimit = 16

    @ConfigNode("max_cost")
    var maxCost = 100

    @ConfigNode("rename_cost")
    var renameCost = 3

    @ConfigNode("repair_cost")
    var repairCost = 5

    @ConfigNode("combine_repair_cost")
    var combineRepairCost = 2

    @ConfigNode("repair_combine_value")
    var repairCombineValue = "{right}+{max}*0.12"

    @ConfigNode("enchant_cost.new_extra")
    var newEnchantExtraCost = 2

    @ConfigNode("enchant_cost.per_level")
    var enchantCostPerLevel = "6.0/{max_level}"

    @ConfigNode("allow_different_material")
    var allowDifferentMaterial = false

    @delegate:ConfigNode("privilege")
    val privilege by conversion<List<String>, Map<String, String>> {
        mapOf(*toTypedArray().map { it.split(":")[0] to it.split(":")[1] }.toTypedArray())
    }

    init {
        registerLifeCycleTask(LifeCycle.ENABLE) {
            conf.onReload {
                console().sendLang("configuration-reload", conf.file!!.name, 0)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onAnvil(e: PrepareAnvilEvent) {
        e.inventory.maximumRepairCost = maxCost
        val renameText = e.inventory.renameText ?: ""

        val result = doMerge(e.inventory.firstItem ?: return, e.inventory.secondItem, renameText, e.viewers[0] as Player)

        // 失败
        if (result == AnvilResult.Failed) {
            return
        }

        var resultItem = result.item
        if (useReworkPenalty && !result.onlyEditName && resultItem != null) {
            resultItem = resultItem.setRepairCost(reworkPenalty.calcToInt("repairCost" to resultItem.getRepairCost()))
        }
        e.inventory.repairCost = result.experience
        e.inventory.repairCostAmount = result.costItemAmount
        e.result = resultItem
        e.inventory.result = resultItem
    }

    fun doMerge(left: ItemStack, right: ItemStack?, name: String?, player: Player): AnvilResult {
        var experience = if (useReworkPenalty) left.getRepairCost().toDouble() else 0.0
        var costItemAmount = 0
        var result: ItemStack? = left.clone()
        var renameText = name
        // 这是一个标记, 合并附魔有可能造成左面的物品和结果物品一样, 这里做个标记防止给拦了
        var mergedEnchants = false
        var onlyEditName = true

        // 新增事件, 处理 烙印诅咒(Permanence Curse) 这样的附魔
        val event = AiyatsbusPrepareAnvilEvent(left, right, result, renameText, player)
        event.call()
        if (event.isCancelled) return AnvilResult.Failed

        result = event.result
        renameText = event.name

        experience += renameCost

        // 若改名框内没有文本, 且不是物品合并, 就是清除物品上的自定义名称
        if ((renameText.isNullOrEmpty() || renameText.isBlank()) && right == null) {
            if (result?.itemMeta?.displayName() == null) {
                return AnvilResult.Failed
            }
            result.modifyMeta<ItemMeta> {
                displayName(null)
            }
            return AnvilResult.Successful(result, experience.cint, 0, true)
        }

        // 改名, 用了自己写的一个扩展属性
        // 是否没改名, 也就是改名框内的名字和原物品是一样的
        val nameIsEqual = left.name?.uncolored() == renameText?.uncolored()
        if (!nameIsEqual) {
            result?.name = renameText
        }

        // 如果右面物品不存在, 就只是改名, 可以直接返回结果了
        if (right.isNull) {
            // 如果没改名, 改名框内的名字和原物品是一样的
            if (nameIsEqual) return AnvilResult.Failed
            return AnvilResult.Successful(result, experience.cint, 0, true)
        }

        // 如果右面物品存在, 就是物品合成 or 物品修补
        // 如果右面的物品和左面的物品不一样, 大概就是右面的是材料修复左面的物品
        if (left.type != right!!.type) {
            // 如果左面的物品可以被损耗(有消耗耐久度这个属性)且可被右面的物品修复
            if (left.itemMeta is Damageable && right.canRepair(left)) {
                // 原版的物品修复, 一个材料可以恢复物品的 1/4 耐久, 先计算这一个物品可以恢复多少耐久, 向上取整
                val per = ceil(left.type.maxDurability / 4.0).cint
                // 用物品的总耐久度除以一个物品恢复的耐久数, 就可以得到恢复总耐久度需要多少个物品了, 向上取整
                val max = ceil(left.damage.cdouble / per).cint
                // 最后计算需要花费几个物品, 取右面物品数量与「恢复总耐久度所需材料数量」的最小值
                costItemAmount += min(max, right.amount)

                // 计算出错
                if (costItemAmount <= 0) {
                    return AnvilResult.Failed
                } else {
                    result?.modifyMeta<Damageable> {
                        // damage 这个数值其实记录的是「损耗了多少耐久」
                        // 也就是说当物品满耐久的时候, 这个数其实是 0, 大概就是这个意思
                        // 所以要想恢复这个物品的耐久, 得用减法
                        // 需要消耗的右面物品的数量乘以右面每个物品可以恢复的耐久度, 得到的数即为所求
                        val toDamage = damage - per * costItemAmount
                        damage = toDamage.coerceIn(0..result.type.maxDurability.toInt())
                    }
                    experience += repairCost
                    onlyEditName = false
                }
            } else {
                // 右面的物品不可以修复左面的物品, 或者左面的物品没有耐久度这个属性, 那么分两种情况:
                // - 右面的物品是附魔书, 要交给后面的逻辑代码处理
                // - 两个物品材质不一样但想合并附魔, 如果开启了允许不同材质合并, 那么也交给后面的逻辑代码处理
                if (right.type != Material.ENCHANTED_BOOK) {
                    if (!(allowDifferentMaterial && right.type.belongedTargets.any { it !in left.type.belongedTargets })) {
                        return AnvilResult.Failed
                    }
                }
            }
        }

        // 两个同样的物品间进行修复
        // 附魔书既是 EnchantmentStorageMeta 也是 Damageable(CraftMetaItem) 但显然附魔书不能用来修复物品, 要进行特判
        if (left.itemMeta is Damageable && right.itemMeta is Damageable && costItemAmount == 0 && right.itemMeta !is EnchantmentStorageMeta) {
            result?.dura =
                (left.dura + ceil(repairCombineValue.calcToDouble("right" to right.dura, "max" to left.type.maxDurability)).cint)
                    .coerceIn(0..left.type.maxDurability.toInt())
            experience += combineRepairCost
            // 同样的物品合成也要设置 costItemAmount 为 1，不然就会出现第二个物品留在铁砧的情况
            costItemAmount += 1
            onlyEditName = false
        }

        // 剩下的情况就是两边物品一样, 等待合并的情况, 或者右面是附魔书的情况, 或者是不同材质合并的情况了, 这些情况可以集中统一处理
        // 这里需要向左面的物品的克隆中添加附魔, 来检测附魔冲突
        val tempLeftItem = left.clone()
        val leftEnchants = left.fixedEnchants
        val rightEnchants = right.fixedEnchants
        val outEnchants = leftEnchants.toMutableMap()

        for ((rightEnchant, level) in rightEnchants) {
            val maxLevel = rightEnchant.basicData.maxLevel
            val previousLevel = outEnchants.remove(rightEnchant)
            // 如果左面物品附魔已有要添加的附魔, 则视为合并
            if (previousLevel != null) {
                // 假如是两个锋利 5 合并, 且允许的话
                outEnchants += if (previousLevel == level) {
                    if (level >= maxLevel && allowUnsafeCombine) {
                        rightEnchant to level + 1
                    } else rightEnchant to (level + 1).coerceAtMost(maxLevel)
                } else {
                    // 如果左右两个附魔有任意一个超出最大等级
                    // 比如锋利 100 和锋利 5, 或者锋利 5 和 锋利 100 合并
                    if (previousLevel > maxLevel || level > maxLevel) {
                        // 如果允许的话
                        if (allowUnsafeLevel) rightEnchant to max(previousLevel, level)
                        // 如果不允许的话
                        // 情况一: 直接阻止这种合并
                        else if (blockOnUnsafeLevel) return AnvilResult.Failed
                        // 情况二: 合并后等级为该附魔等级最大值
                        else rightEnchant to max(previousLevel, level).coerceAtMost(maxLevel)
                    } else {
                        // 正常的合并而已~
                        // 取两个附魔中等级较大的那个为结果等级
                        rightEnchant to max(previousLevel, level)
                    }
                }
            } else {
                // 如果左面物品的附魔里没有这一个附魔的话, 就是添加
                // 首先判断是否超过了允许的最多的附魔数量, 可以减少判断
                if (enchantLimit != -1 && outEnchants.size >= enchantLimit) break
                // 其次判断该附魔是否与已有附魔冲突
                if (rightEnchant.limitations.checkAvailable(CheckType.ANVIL, tempLeftItem, player).isFailure) {
                    continue
                }
                // 新增附魔, 有额外消费
                experience += newEnchantExtraCost
                // 向临时的左面的物品中添加附魔, 便于后续检测
                tempLeftItem.addEt(rightEnchant, level)
                outEnchants += rightEnchant to level
            }
        }

        if (leftEnchants != outEnchants) {
            // 上标记
            mergedEnchants = true
            onlyEditName = false

            // 向结果物品中增加附魔, 同时计算经验等级
            for ((outEnchant, level) in outEnchants) {
                result?.addEt(outEnchant, level)
                val previousLevel = leftEnchants[outEnchant] ?: 0
                experience += enchantCostPerLevel.calcToDouble("max_level" to outEnchant.basicData.maxLevel) * (level - previousLevel)
            }

            // 暂时不清楚是否需要
            if (costItemAmount == 0) costItemAmount += 1
        }

        // 如果只改了名字就不让过, 因为只改名字的情况已经在前面返回了
        if (onlyEditName) return AnvilResult.Failed
        // 如果最后产出的物品跟原来的一模一样, 且没有合并附魔, 就是压根没改
        if (left == result && !mergedEnchants) return AnvilResult.Failed

        return AnvilResult.Successful(result, finalCost(experience, player), costItemAmount, false)
    }

    private fun finalCost(origin: Double, player: Player): Int = privilege.minOf { (perm, expression) ->
        if (player.hasPermission(perm)) expression.calcToInt("expCost" to origin)
        else origin.roundToInt()
    }.coerceAtMost(maxCost).coerceAtLeast(1)
}

sealed class AnvilResult(val item: ItemStack?, val experience: Int, val costItemAmount: Int, val onlyEditName: Boolean) {

    class Successful(item: ItemStack?, experience: Int, costItemAmount: Int, onlyEditName: Boolean) : AnvilResult(item, experience, costItemAmount, onlyEditName)

    object Failed : AnvilResult(null, 0, 0, false)
}