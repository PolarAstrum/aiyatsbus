package cc.polarastrum.aiyatsbus.core

import cc.polarastrum.aiyatsbus.core.data.trigger.Mechanism
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 内置配置驱动的附魔实现
 *
 * 读取文件定义的内置附魔，附带机制配置。
 *
 * @author mical
 * @date 2024/8/21 17:43
 */
interface InternalAiyatsbusEnchantment : AiyatsbusEnchantment

open class InternalAiyatsbusEnchantmentBase(
    id: String,
    file: File?,
    config: Configuration
) : AiyatsbusEnchantmentBase(id, file, config), InternalAiyatsbusEnchantment {

    /**
     * 附魔机制
     */
    override val mechanism: Mechanism? = Mechanism(config.getConfigurationSection("mechanisms"), this)
}