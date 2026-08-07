package cc.polarastrum.aiyatsbus.core.data

import cc.polarastrum.aiyatsbus.core.util.coerceBoolean
import cc.polarastrum.aiyatsbus.core.util.coerceInt
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration

/**
 * 附魔额外数据类
 *
 * 包含附魔的可选配置信息，这些数据一般不需要填写，使用默认值即可。
 * 定义了附魔的各种行为属性，如是否可移除、权重、是否宝藏附魔等。
 *
 * @author mical
 * @since 2024/2/17 15:11
 */
data class AlternativeData(
    /** 配置根节点，可为空 */
    private val root: ConfigurationSection?,

    /** 附魔权重，影响获取概率，默认为 100 */
    val weight: Int = root?.getInt("weight", 100).coerceInt(100),

    /** 是否为原版附魔，用于检测原版附魔，默认为 false */
    /** 3.0 的检测原版附魔的方法有点弱智，把检测原版放到这里其实是更好的选择 */
    val isVanilla: Boolean = (root?.getBoolean("is-vanilla", false) ?: root?.getBoolean("is_vanilla", false)).coerceBoolean(false),

    /** 是否可通过砂轮移除，默认为 true */
    val grindstoneable: Boolean = root?.getBoolean("grindstoneable", true).coerceBoolean(true),
    /** 是否为宝藏附魔，默认为 false */
    val isTreasure: Boolean = (root?.getBoolean("is-treasure", false) ?: root?.getBoolean("is_treasure", false)).coerceBoolean(false),
    /** 是否为诅咒附魔，默认为 false */
    val isCursed: Boolean = (root?.getBoolean("is-cursed", false) ?: root?.getBoolean("is_cursed", false)).coerceBoolean(false),
    /** 是否可通过交易获得，默认为 true */
    val isTradeable: Boolean = (root?.getBoolean("is-tradeable", true) ?: root?.getBoolean("is_tradeable", true)).coerceBoolean(true),
    /** 是否可通过附魔台发现，默认为 true */
    val isDiscoverable: Boolean = (root?.getBoolean("is-discoverable", true) ?: root?.getBoolean("is_discoverable", true)).coerceBoolean(true),

    /** 交易最大等级限制，-1 表示无限制，默认为 -1 */
    val tradeMaxLevel: Int = (root?.getInt("trade-max-level", -1) ?: root?.getInt("trade_max_level", -1)).coerceInt(-1),
    /** 附魔台最大等级限制，-1 表示无限制，默认为 -1 */
    val enchantMaxLevel: Int = (root?.getInt("enchant-max-level", -1) ?: root?.getInt("enchant-max-level", -1)).coerceInt(-1),
    /** 战利品最大等级限制，-1 表示无限制，默认为 -1 */
    val lootMaxLevel: Int = (root?.getInt("loot-max-level", -1) ?: root?.getInt("loot_max_level", -1)).coerceInt(-1),

    /** 是否不可获得，为 true 时玩家无法获得该附魔，默认为 false */
    val inaccessible: Boolean = root?.getBoolean("inaccessible", false).coerceBoolean(false),

    /** 是否是 EcoEnchants 触发器形式的附魔 */
    val isEco: Boolean = root?.getBoolean("is-eco", false).coerceBoolean(false)
) {

    /**
     * 获取交易等级限制
     *
     * 优先使用附魔自身的交易等级限制，其次使用全局限制，最后使用最大等级。
     * 返回的值不会超过附魔的最大等级。
     *
     * @param maxLevel 附魔的最大等级
     * @param globalLimit 全局等级限制
     * @return 实际可用的交易等级限制
     *
     * @example
     * ```kotlin
     * val limit = alternativeData.getTradeLevelLimit(5, 3) // 返回 3
     * ```
     */
    fun getTradeLevelLimit(maxLevel: Int, globalLimit: Int): Int {
        return (if (tradeMaxLevel != -1) tradeMaxLevel else if (globalLimit != -1) globalLimit else maxLevel).coerceAtMost(maxLevel)
    }

    /**
     * 获取附魔台等级限制
     *
     * 优先使用附魔自身的附魔台等级限制，其次使用全局限制，最后使用最大等级。
     * 返回的值不会超过附魔的最大等级。
     *
     * @param maxLevel 附魔的最大等级
     * @param globalLimit 全局等级限制
     * @return 实际可用的附魔台等级限制
     *
     * @example
     * ```kotlin
     * val limit = alternativeData.getEnchantMaxLevelLimit(5, 3) // 返回 3
     * ```
     */
    fun getEnchantMaxLevelLimit(maxLevel: Int, globalLimit: Int): Int {
        return (if (enchantMaxLevel != -1) enchantMaxLevel else if (globalLimit != -1) globalLimit else maxLevel).coerceAtMost(maxLevel)
    }

    /**
     * 获取战利品等级限制
     *
     * 优先使用附魔自身的战利品等级限制，其次使用全局限制，最后使用最大等级。
     * 返回的值不会超过附魔的最大等级。
     *
     * @param maxLevel 附魔的最大等级
     * @param globalLimit 全局等级限制
     * @return 实际可用的战利品等级限制
     *
     * @example
     * ```kotlin
     * val limit = alternativeData.getLootMaxLevelLimit(5, 3) // 返回 3
     * ```
     */
    fun getLootMaxLevelLimit(maxLevel: Int, globalLimit: Int): Int {
        return (if (lootMaxLevel != -1) lootMaxLevel else if (globalLimit != -1) globalLimit else maxLevel).coerceAtMost(maxLevel)
    }

    fun serialize(): ConfigurationSection {
        return Configuration.empty().apply {
            set("weight", weight)
            set("is-vanilla", isVanilla)
            set("grindstoneable", grindstoneable)
            set("is-treasure", isTreasure)
            set("is-cursed", isCursed)
            set("is-tradeable", isTradeable)
            set("is-discoverable", isDiscoverable)
            set("trade-max-level", tradeMaxLevel)
            set("enchant-max-level", enchantMaxLevel)
            set("loot-max-level", lootMaxLevel)
            set("inaccessible", inaccessible)
            set("is-eco", isEco)
        }
    }

    class Builder {

        private var weight: Int = 100
        private var isVanilla: Boolean = false
        private var grindstoneable: Boolean = true
        private var isTreasure: Boolean = false
        private var isCursed: Boolean = false
        private var isTradeable: Boolean = true
        private var isDiscoverable: Boolean = true
        private var tradeMaxLevel: Int = -1
        private var enchantMaxLevel: Int = -1
        private var lootMaxLevel: Int = -1
        private var inaccessible: Boolean = false
        private var hardcoded: Boolean = false
        private var isEco: Boolean = false

        fun weight(weight: Int): Builder {
            this.weight = weight
            return this
        }

        fun isVanilla(isVanilla: Boolean): Builder {
            this.isVanilla = isVanilla
            return this
        }

        fun grindstoneable(grindstoneable: Boolean): Builder {
            this.grindstoneable = grindstoneable
            return this
        }

        fun isTreasure(isTreasure: Boolean): Builder {
            this.isTreasure = isTreasure
            return this
        }

        fun isCursed(isCursed: Boolean): Builder {
            this.isCursed = isCursed
            return this
        }

        fun isTradeable(isTradeable: Boolean): Builder {
            this.isTradeable = isTradeable
            return this
        }

        fun isDiscoverable(isDiscoverable: Boolean): Builder {
            this.isDiscoverable = isDiscoverable
            return this
        }

        fun tradeMaxLevel(tradeMaxLevel: Int): Builder {
            this.tradeMaxLevel = tradeMaxLevel
            return this
        }

        fun enchantMaxLevel(enchantMaxLevel: Int): Builder {
            this.enchantMaxLevel = enchantMaxLevel
            return this
        }

        fun lootMaxLevel(lootMaxLevel: Int): Builder {
            this.lootMaxLevel = lootMaxLevel
            return this
        }

        fun inaccessible(inaccessible: Boolean): Builder {
            this.inaccessible = inaccessible
            return this
        }

        fun isEco(isEco: Boolean): Builder {
            this.isEco = isEco
            return this
        }

        fun hardcoded(hardcoded: Boolean): Builder {
            this.hardcoded = hardcoded
            return this
        }

        fun build(): AlternativeData {
            return AlternativeData(
                Configuration.empty(),
                weight,
                isVanilla,
                grindstoneable,
                isTreasure,
                isCursed,
                isTradeable,
                isDiscoverable,
                tradeMaxLevel,
                enchantMaxLevel,
                lootMaxLevel,
                inaccessible,
                isEco
            )
        }
    }

    companion object {

        @JvmStatic
        fun builder() = Builder()
    }
}
