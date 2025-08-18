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
import cc.polarastrum.aiyatsbus.core.data.registry.Group
import cc.polarastrum.aiyatsbus.core.data.registry.Rarity
import cc.polarastrum.aiyatsbus.core.data.registry.Target
import cc.polarastrum.aiyatsbus.core.data.trigger.Trigger
import cc.polarastrum.aiyatsbus.core.util.roman
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 附魔接口
 *
 * 定义了 Aiyatsbus 系统中所有附魔的基础属性和行为。
 * 包含附魔的标识、配置、品质、显示、限制、触发器等。
 *
 * @author mical
 * @since 2024/2/17 14:04
 */
interface AiyatsbusEnchantment {

    /**
     * 附魔唯一标识符
     * 
     * 用于在系统内唯一标识该附魔，通常为文件名。
     * 例如：`sharpness`、`flame`、`custom_enchant`
     */
    val id: String

    /**
     * 附魔的命名空间键
     * 
     * Bukkit/Minecraft 系统中的附魔标识，用于注册到游戏内。
     * 格式为 `namespace:key`，例如：`aiyatsbus:sharpness`
     */
    val enchantmentKey: NamespacedKey

    /**
     * 附魔配置文件
     * 
     * 存储附魔配置的 YAML 文件实例，包含所有附魔数据。
     */
    val file: File

    /**
     * 附魔配置对象
     * 
     * 解析后的配置文件内容，用于读取附魔的各项设置。
     */
    val config: Configuration

    /**
     * 附魔基本数据
     * 
     * 包含附魔的基础信息，如名称、描述、最大等级等。
     */
    val basicData: BasicData

    /**
     * 附魔额外数据
     * 
     * 包含附魔的扩展配置，如是否可获得、特殊标记等。
     */
    val alternativeData: AlternativeData

    /**
     * 附魔依赖信息
     * 
     * 包括必须为 MC 哪个版本才能使用、必须安装哪些数据包、必须安装哪些插件。
     * 用于确保附魔在正确的环境下运行。
     */
    val dependencies: Dependencies

    /**
     * Bukkit 附魔实例
     * 
     * 在注册后赋值，一般是 CraftEnchantment。
     * 在 1.20.2 及以下版本中，这个是 LegacyCraftEnchantment。
     * 在 1.20.2 以上版本中，Bukkit 更改了注册附魔的方式，这个一般是 AiyatsbusCraftEnchantment。
     */
    val enchantment: Enchantment

    /**
     * 附魔品质等级
     * 
     * 定义附魔的稀有度和颜色显示，影响附魔的获取概率和展示效果。
     */
    val rarity: Rarity

    /**
     * 附魔变量系统
     * 
     * 处理附魔显示文本中的变量替换，支持动态内容展示。
     */
    val variables: Variables

    /**
     * 附魔显示器
     * 
     * 负责附魔在物品上的显示效果，包括名称格式、颜色等。
     */
    val displayer: Displayer

    /**
     * 附魔适用目标
     * 
     * 定义附魔可以应用于哪些物品类型，如武器、工具、护甲等。
     */
    val targets: List<Target>

    /**
     * 附魔限制条件
     * 
     * 定义附魔的使用限制，如冲突检测、等级限制、环境要求等。
     */
    val limitations: Limitations

    /**
     * 附魔触发器
     * 
     * 定义附魔效果的触发条件和执行逻辑，为 null 表示不使用 Aiyatsbus 内置的触发器系统。
     */
    val trigger: Trigger?

    /** 是否不可获得 */
    val inaccessible: Boolean
        get() = alternativeData.inaccessible ||
                rarity.inaccessible ||
                Group.filter { enchantment.isInGroup(it.value) }.any { it.value.inaccessible }

    /**
     * 判断与另一个附魔是否冲突
     *
     * @param other 另一个附魔
     * @return 是否冲突
     */
    fun conflictsWith(other: Enchantment): Boolean {
        return limitations.conflictsWith(other)
    }

    /**
     * 检查物品是否可被该附魔附魔（支持粘液附魔机）
     *
     * @param item 物品
     * @return 是否可附魔
     */
    fun canEnchantItem(item: ItemStack): Boolean {
        return limitations.checkAvailable(CheckType.ANVIL, item).isSuccess
    }

    /**
     * 获取显示名称
     *
     * @param level 等级
     * @param roman 是否使用罗马数字
     * @return 附魔显示名称
     */
    fun displayName(level: Int? = null, roman: Boolean = true): String {
        val name = basicData.name.colored() + if (roman) (level?.roman(basicData.maxLevel == 1, true)
            ?: "") else if (basicData.maxLevel == 1) "" else level
        return if (basicData.nameHasColor) name else rarity.displayName(name)
    }
}