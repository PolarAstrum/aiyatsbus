package cc.polarastrum.aiyatsbus.core.data.registry

import cc.polarastrum.aiyatsbus.core.data.Dependencies
import cc.polarastrum.aiyatsbus.core.data.Dependency
import cc.polarastrum.aiyatsbus.core.data.Registry
import cc.polarastrum.aiyatsbus.core.data.RegistryItem
import org.bukkit.inventory.EquipmentSlot
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * 附魔目标类
 *
 * 定义附魔可以应用的目标物品类型和装备槽位。
 * 支持指定物品材质、装备槽位、最大附魔数量等配置。
 * 用于限制附魔只能应用在特定类型的物品上。
 *
 * @param root 配置根节点，包含所有附魔目标的配置信息
 * @param dependencies 依赖项配置，定义该附魔目标的前置条件
 * @param name 附魔目标名称
 * @param capability 最大附魔数量，限制该目标物品可以拥有的附魔数量
 * @param activeSlots 激活槽位列表，指定附魔在哪些装备槽位生效
 * @param types 支持的物品类型列表，指定附魔可以应用的物品材质
 * @param skull 头颅材质值，用于自定义头颅显示
 *
 * @author mical
 * @since 2025/6/20 20:25
 */
data class Target @JvmOverloads constructor(
    /** 配置根节点 */
    private val root: ConfigurationSection,
    /** 依赖项配置，定义该附魔目标的前置条件 */
    override val dependencies: Dependencies = Dependencies(root.getConfigurationSection("dependencies")),
    /** 附魔目标名称 */
    val name: String = root.getString("name")!!,
    /** 最大附魔数量，限制该目标物品可以拥有的附魔数量 */
    val capability: Int = root.getInt("max"),
    /** 激活槽位列表，指定附魔在哪些装备槽位生效 */
    val activeSlots: List<EquipmentSlot> = root.get("active-slots").asSlotNames().map { EquipmentSlot.valueOf(it) },
    /** 支持的物品类型列表，指定附魔可以应用的物品材质 */
    val types: List<TargetItemType> = root.get("types").asTargetTypes(capability),
    /** 头颅材质值，用于自定义头颅显示 */
    val skull: String = root.getString("skull", "")!!,
    /** 不同材质的最大附魔数 */
    val vanillaTypes: List<org.bukkit.Material> = types.mapNotNull { it.vanillaMaterial },
) : RegistryItem(root), Dependency {

    /**
     * 附魔目标注册器伴生对象
     *
     * 负责从配置文件中加载和管理附魔目标数据。
     * 提供全局访问点来获取和管理所有附魔目标。
     */
    companion object : Registry<Target>("target", { section -> Target(section) }) {

        /** 附魔目标配置文件，自动重载配置变更 */
        @Config("enchants/target.yml", autoReload = true)
        override lateinit var config: Configuration
            private set
    }
}

private fun Any?.asTargetTypes(defaultCapability: Int): List<TargetItemType> {
    val values = this as? Collection<*> ?: return emptyList()
    return values.mapNotNull { value ->
        val raw = value as? String ?: return@mapNotNull null
        val separator = raw.lastIndexOf('~')
        val suffix = if (separator == -1) null else raw.substring(separator + 1).toIntOrNull()
        val identifier = if (suffix == null) raw else raw.substring(0, separator)
        val capability = suffix ?: defaultCapability
        TargetItemSources.create(identifier, capability)
    }
}

private fun Any?.asSlotNames(): List<String> {
    val values = this as? Collection<*> ?: return emptyList()
    return values.mapNotNull { value ->
        when (value) {
            is String -> value
            is Map<*, *> -> (value["item"] ?: value["type"])?.toString()
            else -> null
        }
    }
}
