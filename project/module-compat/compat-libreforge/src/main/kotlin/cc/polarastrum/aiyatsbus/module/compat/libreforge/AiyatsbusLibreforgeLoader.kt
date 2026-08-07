/*
 * This file is part of EcoEnchants, licensed under the GPL-3.0 License.
 *
 *  Copyright (C) 2024 Auxilor
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
package cc.polarastrum.aiyatsbus.module.compat.libreforge

import cc.polarastrum.aiyatsbus.core.util.libreforgeEnabled
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeEnchantLevel
import cc.polarastrum.aiyatsbus.module.compat.libreforge.target.LibreforgeEnchantFinder
import cc.polarastrum.aiyatsbus.module.compat.libreforge.target.LibreforgeEnchantFinder.clearEnchantmentCache
import com.willfp.libreforge.registerHolderProvider
import org.bukkit.entity.LivingEntity
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.module.configuration.Configuration
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

/**
 * AiyatsbusLibreforgePlugin
 * com.mcstarrysky.aiyatsbus.libreforge.AiyatsbusLibreforgePlugin
 *
 * @author mical
 * @since 2024/7/20 22:30
 */
object AiyatsbusLibreforgeLoader {

    @Awake(LifeCycle.INIT)
    fun onInit() {
        LibreforgeDependencyLoader.load()
    }

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        // 在这之前先进行从 EcoEnchants 文件夹的复制操作
        if (!libreforgeEnabled) return
        val target = File(getDataFolder(), "enchants/Packet-EcoEnchants/")
        if (!target.exists()) {
            target.mkdirs()
            var source = File(getDataFolder().parent, "AiyatsbusLibreforge/enchantments/")
            if (source.exists()) {
                source.walk().filter { it.isFile }.forEach { file ->
                    val dest = File(target, file.relativeTo(source).path)
                    if (!dest.exists()) {
                        dest.parentFile.mkdirs()
                        file.copyTo(dest)
                    }
                    val newConfig = Configuration.loadFromFile(dest)
                    if (!newConfig.contains("alternative.is-eco") && !newConfig.contains("alternative.is_eco")) {
                        newConfig["alternative.is-eco"] = true
                        newConfig.saveToFile(dest)
                    }
                }
            } else {
                source = File(getDataFolder().parent, "EcoEnchants/enchants/")
                if (!source.exists()) {
                    return
                }
                source.walk().filter { it.isFile }.forEach { file ->
                    val dest = File(target, file.relativeTo(source).path)
                    if (!dest.exists()) {
                        dest.parentFile.mkdirs()
                        file.copyTo(dest)
                    }
                }
            }
        }
    }

    @Awake(LifeCycle.ENABLE)
    fun handleEnable() {
        if (!libreforgeEnabled) return
        registerHolderProvider(LibreforgeEnchantFinder.finder.toHolderProvider())

        registerLibreforgeFunction("registerRefreshFunction", 1) { arguments ->
            val entity = arguments[0]?.javaClass?.getMethod("getDispatcher")?.invoke(arguments[0])
            if (entity is LivingEntity) {
                entity.clearEnchantmentCache()
            }
            Unit
        }

        registerLibreforgeFunction("registerPlaceholderProvider", 2) { arguments ->
            val holder = arguments[0]?.javaClass?.getMethod("getHolder")?.invoke(arguments[0])
            if (holder !is LibreforgeEnchantLevel) {
                return@registerLibreforgeFunction emptyList<Any>()
            }
            val namedValueClass = Class.forName(
                "com.willfp.libreforge.NamedValue",
                true,
                javaClass.classLoader
            )
            listOf(namedValueClass.getConstructor(String::class.java, Any::class.java).newInstance("level", holder.level))
        }
    }

    private fun registerLibreforgeFunction(methodName: String, arity: Int, callback: (Array<out Any?>) -> Any?) {
        val holderProviderClass = Class.forName(
            "com.willfp.libreforge.HolderProviderKt",
            true,
            javaClass.classLoader
        )
        val method = holderProviderClass.methods.singleOrNull {
            it.name == methodName && it.parameterCount == 1
        } ?: error("Unsupported libreforge: HolderProviderKt.$methodName was not found")
        val functionType = method.parameterTypes.single()
        val function = Proxy.newProxyInstance(functionType.classLoader, arrayOf(functionType)) { proxy, calledMethod, arguments ->
            when {
                calledMethod.name == "invoke" && calledMethod.parameterCount == arity -> callback(arguments ?: emptyArray())
                calledMethod.name == "toString" && calledMethod.parameterCount == 0 -> "LibreforgeFunctionProxy($methodName)"
                calledMethod.name == "hashCode" && calledMethod.parameterCount == 0 -> System.identityHashCode(proxy)
                calledMethod.name == "equals" && calledMethod.parameterCount == 1 -> proxy === arguments?.firstOrNull()
                else -> error("Unsupported proxy method: $calledMethod")
            }
        }

        try {
            method.invoke(null, function)
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }
}
