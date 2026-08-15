package cc.polarastrum.aiyatsbus.impl.registration.legacy

import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.BuiltinAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.InternalAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.core.VanillaAiyatsbusEnchantmentBase
import cc.polarastrum.aiyatsbus.core.registration.AiyatsbusEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeAiyatsbusEnchantBase
import org.bukkit.enchantments.Enchantment
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.nmsProxyClass

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.impl.registration.legacy.DefaultLegacyEnchantmentRegisterer
 *
 * @author mical
 * @since 2024/2/17 18:51
 */
object DefaultLegacyEnchantmentRegisterer : AiyatsbusEnchantmentRegisterer {

    val clazzLegacyVanillaCraftEnchantment =
        nmsProxyClass<Enchantment>(DefaultLegacyEnchantmentRegisterer::class.java.packageName + ".LegacyVanillaCraftEnchantment")

    @Awake(LifeCycle.CONST)
    fun init() {
        if (MinecraftVersion.versionId <= 12002) {
            Enchantment::class.java.setProperty("acceptingNew", value = true, isStatic = true)
        }
    }

    @Awake(LifeCycle.DISABLE)
    fun exit() {
        if (MinecraftVersion.versionId <= 12002) {
            Enchantment::class.java.setProperty("acceptingNew", value = false, isStatic = true)
        }
    }

    override fun register(enchant: AiyatsbusEnchantmentBase): Enchantment {
        val enchantment = if (enchant.alternativeData.isVanilla) {
            if (enchant !is VanillaAiyatsbusEnchantmentBase) throw IllegalArgumentException("Enchant ${enchant.id} must be an impl of VanillaAiyatsbusEnchantment!")
            val bukkitEnchantment = Enchantment.getByKey(enchant.enchantmentKey)!!
            clazzLegacyVanillaCraftEnchantment
                .getConstructor(VanillaAiyatsbusEnchantmentBase::class.java, Enchantment::class.java)
                .newInstance(enchant, bukkitEnchantment).also {
                    Enchantment::class.java.getProperty<HashMap<*, *>>("byKey", true)!!.remove(bukkitEnchantment.key)
                    Enchantment::class.java.getProperty<HashMap<*, *>>("byName", true)!!.remove(bukkitEnchantment.name)
                }
        } else {
            when (enchant) {
                is LibreforgeAiyatsbusEnchantBase -> LegacyLibreforgeAiyatsbusCraftEnchantment(enchant)
                is BuiltinAiyatsbusEnchantmentBase -> LegacyBuiltinAiyatsbusCraftEnchantment(enchant)
                is InternalAiyatsbusEnchantmentBase -> LegacyInternalAiyatsbusCraftEnchantment(enchant)
                else -> LegacyAiyatsbusCraftEnchantment(enchant)
            }
        }
        Enchantment.registerEnchantment(enchantment)
        return enchantment
    }

    override fun unregister(enchant: AiyatsbusEnchantment) {
        // 肯定不能卸载原版附魔啊, 想什么呢?
        if (!enchant.alternativeData.isVanilla) {
            Enchantment::class.java.getProperty<HashMap<*, *>>("byKey", true)!!.remove(enchant.enchantmentKey)
            Enchantment::class.java.getProperty<HashMap<*, *>>("byName", true)!!.remove(enchant.id.uppercase())
        } else {
            // 强兼低版本
            // 高版本的任何附魔都已经不能卸载了，所以没必要做支持
            // 理论上也可以通过在注册时缓存原来的附魔实例，然后卸载时再替换回去来解决
            (enchant as? VanillaAiyatsbusEnchantment)?.injector?.enable = false
        }
    }
}