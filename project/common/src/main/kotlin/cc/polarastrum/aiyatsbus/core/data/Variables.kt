package cc.polarastrum.aiyatsbus.core.data

import cc.polarastrum.aiyatsbus.core.AiyatsbusSettings
import cc.polarastrum.aiyatsbus.core.util.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import taboolib.common5.cint
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.util.asMap
import taboolib.module.nms.getItemTag
import taboolib.module.nms.setItemTag
import taboolib.platform.util.modifyMeta
import java.util.function.IntFunction

/**
 * 附魔变量类型枚举
 *
 * 定义了四种不同类型的变量：
 * - leveled：与等级有关的变量，例如等级越高触发概率越大，公式需要带入等级计算，可嵌套其他变量但严禁互相嵌套
 * - modifiable：与物品强相关的数据，需要写在物品的 PDC 里，如武器击杀次数累积多少触发什么东西
 * - ordinary：常量，也可以理解为配置项，不提供任何计算
 * - custom：开发者自定义的变量，通过函数计算得出结果
 *
 * @author mical
 * @since 2024/2/17 22:29
 */
enum class VariableType {
    /** 与等级有关的变量 */
    LEVELED,

    /** 与物品强相关的数据 */
    MODIFIABLE,

    /** 常量配置项 */
    ORDINARY,

    /** 开发者添加的变量 */
    CUSTOM
}

/**
 * 附魔变量管理类
 *
 * 负责管理附魔系统中的各种变量，包括变量的解析、计算和存储。
 * 支持四种类型的变量：等级相关变量、物品相关变量、常量变量和自定义变量。
 * 提供变量计算、修改和批量处理功能。
 *
 * @param root 配置根节点，包含所有变量的配置信息
 *
 * @author mical
 * @since 2024/2/17 22:29
 */
class Variables(
    root: ConfigurationSection?
) {

    /**
     * 存储变量的类型映射，用于判断变量类型。
     * 键为变量名，值为变量类型。
     */
    private val variables: MutableMap<String, VariableType> = HashMap()

    /**
     * 等级相关变量映射。
     * 键为变量名，值为 Pair(单位, 等级-公式映射)。
     * 例如：{"damage" to ("点" to mapOf(1 to "level*2+1"))}
     */
    val leveled: MutableMap<String, Pair1<String, Map<Int, String>>> = HashMap()

    /**
     * 物品相关变量映射。
     * 键为变量名，值为 Pair(存储键, 初始值)。
     * 存储键可为 PDC 字段或以 (NBT) 开头的 NBT 路径。
     */
    val modifiable: MutableMap<String, Pair1<String, String>> = HashMap()

    /**
     * 常量变量映射。
     * 键为变量名，值为常量内容。
     */
    val ordinary: MutableMap<String, Any?> = HashMap()

    /**
     * 自定义变量映射。
     * 键为变量名，值为函数，函数参数为等级，返回变量值。
     * 
     * 避免操作本 map，使用 addCustom 和 removeCustom 方法。
     */
    val custom: MutableMap<String, IntFunction<Any?>> = HashMap()

    init {
        // 解析等级相关变量
        root?.getConfigurationSection("leveled").asMap().forEach { (variable, section) ->
            // 如果分级配置了不同的值
            if (section is ConfigurationSection) {
                leveled[variable] = (section["unit"]?.toString().orEmpty()) to1 section.asMap()
                    .map { it.key.cint to it.value.toString() }.toMap()
            } else {
                val (unit, formula) = section.toString().split(":", limit = 2)
                // 只存 1 级，任何等级都能获取到
                leveled[variable] = unit to1 mapOf(1 to formula)
            }
            // 存储该变量的类型
            variables[variable] = VariableType.LEVELED
        }
        // 解析物品相关变量
        root?.getConfigurationSection("modifiable").asMap().forEach { (variable, expression) ->
            val parts = expression.toString().split('=')
            modifiable[variable] = parts[0] to1 parts[1]
            variables[variable] = VariableType.MODIFIABLE
        }
        // 解析常量变量
        root?.getConfigurationSection("ordinary").asMap().forEach { (variable, value) ->
            ordinary[variable] = value
            variables[variable] = VariableType.ORDINARY
        }
    }

    /**
     * 注册自定义变量。
     * 必须通过本方法添加自定义变量，避免直接操作 custom map。
     *
     * @param variable 变量名
     * @param func 计算函数，参数为等级，返回变量值
     */
    fun addCustom(variable: String, func: IntFunction<Any?>) {
        variables += variable to VariableType.CUSTOM
        custom += variable to func
    }

    /**
     * 移除自定义变量。
     * 必须通过本方法移除自定义变量，避免直接操作 custom map。
     *
     * @param variable 变量名
     * @return 是否成功移除
     */
    fun removeCustom(variable: String): Boolean {
        return if (variables[variable] == VariableType.CUSTOM) {
            variables.remove(variable)
            custom.remove(variable)
            true
        } else {
            false
        }
    }

    /**
     * 计算等级相关变量。
     *
     * 根据当前等级选择最接近的配置，支持嵌套变量解析和单位显示。
     *
     * @param variable 变量名
     * @param level 当前等级
     * @param withUnit 是否包含单位
     * @return 计算结果，类型为 Int 或 Double 或带单位的字符串
     */
    fun leveled(variable: String, level: Int, withUnit: Boolean): Any {
        val v = leveled[variable]!! // 获取变量
        return v.second
            .filter { it.key <= level } // 过滤掉等级高于当前等级的参数
            .minBy { level - it.key }.value // 取当前变量与该变量的差的最小值的变量，当然也就是最高的那个等级配置
            .singletons(VariableReaders.DOUBLE_BRACES) { leveled(it, level, false).toString() } // 尝试解析其中的嵌套变量（双括号）
            .calcToDouble("level" to level)
            .let {
                // 如果是小数形式的整数则只保留整数位
                if (it.isInteger()) it.toInt() else it.toBigDecimal()
                    .setScale(AiyatsbusSettings.variableRoundingScale, AiyatsbusSettings.variableRoundingMode)
                    .toDouble()
            }
            .let { if (withUnit) it.toString() + v.first else it }
    }

    /**
     * 获取物品相关变量的值。
     *
     * 优先从物品的 PDC 获取，若变量名以 (NBT) 开头则从 NBT 获取。
     * 若物品为空或变量不存在，返回默认值。
     *
     * @param variable 变量名
     * @param item 物品堆
     * @return 变量值，若物品为空返回 "?"
     */
    fun modifiable(variable: String, item: ItemStack?): Any {
        if (item == null) return "?"
        val v = modifiable[variable]!!
        val usingNBT = v.first.startsWith("(NBT)")
        if (usingNBT) {
            return item.getItemTag().getDeep(v.first.removePrefix("(NBT)"))?.unsafeData() ?: v.second
        }
        return item.itemMeta[v.first, PersistentDataType.STRING] ?: v.second
    }

    /**
     * 修改物品相关变量。
     *
     * 根据变量配置决定使用 PDC 还是 NBT 存储，支持字符串类型变量修改。
     *
     * @param item 要修改的物品
     * @param variable 变量名
     * @param value 新值
     * @return 修改后的物品
     */
    fun modifyVariable(item: ItemStack, variable: String, value: String): ItemStack {
        val v = modifiable[variable]!!
        val usingNBT = v.first.startsWith("(NBT)")
        if (usingNBT) {
            val tag = item.getItemTag()
            tag.putDeep(v.first.removePrefix("(NBT)"), value)
            item.setItemTag(tag)
            return item
        }
        return item.modifyMeta<ItemMeta> {
            this[modifiable[variable]!!.first, PersistentDataType.STRING] = value
        }
    }

    /**
     * 获取常量变量的值。
     *
     * 直接返回配置中定义的常量值。
     *
     * @param variable 变量名
     * @return 常量值
     */
    fun ordinary(variable: String): Any? = ordinary[variable]

    /**
     * 计算自定义变量。
     *
     * 调用开发者注册的自定义函数，参数为等级。
     *
     * @param variable 变量名
     * @param level 当前等级
     * @return 计算结果，若变量不存在返回 null
     */
    fun custom(variable: String, level: Int): Any? = custom[variable]?.apply(level)

    /**
     * 通用变量计算接口。
     *
     * 根据变量类型自动选择相应的计算方法。
     *
     * @param variable 变量名
     * @param level 等级（用于等级相关、自定义变量）
     * @param item 物品（用于物品相关变量）
     * @param withUnit 是否带单位
     * @return 计算结果
     */
    fun variable(variable: String, level: Int, item: ItemStack? = null, withUnit: Boolean = false): Any? {
        return when (variables[variable]!!) {
            VariableType.LEVELED -> leveled(variable, level, withUnit)
            VariableType.MODIFIABLE -> modifiable(variable, item)
            VariableType.ORDINARY -> ordinary(variable)
            VariableType.CUSTOM -> custom(variable, level)
        }
    }

    /**
     * 批量计算所有变量。
     *
     * 计算所有已注册变量，返回变量名到计算结果的映射。
     *
     * @param level 等级
     * @param item 物品
     * @param withUnit 是否带单位
     * @return 变量名到计算结果的映射
     */
    fun variables(level: Int, item: ItemStack? = null, withUnit: Boolean = false): Map<String, Any?> {
        return variables.keys.associateWith { variable(it, level, item, withUnit) }
    }
}
