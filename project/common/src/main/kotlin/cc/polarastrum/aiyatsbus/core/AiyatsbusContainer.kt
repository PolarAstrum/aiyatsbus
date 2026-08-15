package cc.polarastrum.aiyatsbus.core

import org.bukkit.NamespacedKey

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.core.AiyatsbusContainer
 *
 * @author mical
 * @since 2026/8/15 13:05
 */
object AiyatsbusContainer {

    private val enchantmentContainers = HashMap<String, HashMap<Any?, Any?>>()

    val globalContainer = HashMap<Any, Any>()

    fun getContainer(enchantment: AiyatsbusEnchantment): HashMap<Any?, Any?> {
        return enchantmentContainers.computeIfAbsent(enchantment.id) { HashMap() }
    }

    fun getContainer(enchantment: NamespacedKey): HashMap<Any?, Any?> {
        return enchantmentContainers.computeIfAbsent(enchantment.key) { HashMap() }
    }

    fun getContainer(enchantment: String): HashMap<Any?, Any?> {
        return enchantmentContainers.computeIfAbsent(enchantment) { HashMap() }
    }
}