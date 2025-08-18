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
@file:Suppress("LeakingThis")

package cc.polarastrum.aiyatsbus.core.data

import cc.polarastrum.aiyatsbus.core.StandardPriorities
import cc.polarastrum.aiyatsbus.core.sendLang
import cc.polarastrum.aiyatsbus.core.util.reloadable
import taboolib.common.LifeCycle
import taboolib.common.TabooLib
import taboolib.common.platform.function.console
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.severe
import taboolib.common.util.t
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import kotlin.system.measureTimeMillis

/**
 * 注册项基类
 *
 * 所有可注册项目的基类，包含配置节点和唯一标识符。
 * 用于统一管理各种配置项的加载和存储。
 * 提供基本的配置访问功能。
 *
 * @param root 配置根节点，包含该注册项的所有配置信息
 * @param id 注册项的唯一标识符，默认为配置节点名称
 *
 * @author mical
 * @since 2025/6/20 19:31
 */
abstract class RegistryItem(
    /** 配置根节点 */
    private val root: ConfigurationSection,
    /** 注册项的唯一标识符，默认为配置节点名称 */
    val id: String = root.name
)

/**
 * 注册器基类
 *
 * 提供统一的注册项管理功能，支持配置文件的自动加载和重载。
 * 实现了 Map 接口，可以直接通过键值对的方式访问注册项。
 * 支持依赖检查和生命周期管理。
 *
 * @param T 注册项类型，必须继承自 RegistryItem
 * @param registryId 注册器唯一标识符
 * @param itemFactory 用于创建注册项实例的工厂函数
 * @param registered 存储已注册项的并发哈希映射
 *
 * @author mical
 * @since 2025/6/20 19:31
 */
abstract class Registry<T : RegistryItem>(
    /** 注册器唯一标识符 */
    private val registryId: String,
    /** 注册项工厂函数，用于从配置节点创建注册项实例 */
    private val itemFactory: Function<ConfigurationSection, T>,
    /** 已注册项的存储容器，使用并发哈希映射保证线程安全 */
    val registered: ConcurrentHashMap<String, T> = ConcurrentHashMap()
) : Map<String, T> by registered {

    /** 注册器对应的配置文件 */
    abstract val config: Configuration

    /** 标记是否已经完成初始化加载 */
    private var isLoaded: Boolean = false

    init {
        // 注册生命周期任务，在插件启用时自动初始化
        reloadable {
            registerLifeCycleTask(StandardPriorities.getDataLifeCycle(registryId), StandardPriorities.getDataProperty(registryId)) {
                try {
                    initialize()
                    // 监听配置文件重载事件
                    if (!isLoaded) {
                        config.onReload {
                            measureTimeMillis { loadItem() }.let { console().sendLang("configuration-reload", config.file!!.name, it) }
                        }
                        isLoaded = true
                    }
                } catch (ex: Throwable) {
                    if (TabooLib.getCurrentLifeCycle() != LifeCycle.ACTIVE) {
                        severe("""
                            无法初始化附魔数据，为避免数据丢失，服务器将会被强制关闭！
                            Failed to initialize enchantment data. To avoid data loss, the server will be forced to shut down!
                        """.t())
                        ex.printStackTrace()
                        Runtime.getRuntime().halt(-1)
                    }
                }
            }
        }
    }

    /**
     * 初始化注册器
     *
     * 如果已经加载过，则重新加载配置文件；
     * 否则执行首次加载并标记为已加载状态。
     */
    fun initialize() {
        if (isLoaded) {
            config.reload()
            return
        }
        loadItem()
    }

    /**
     * 加载注册项
     *
     * 清空现有注册项，从配置文件中重新加载所有项目。
     * 对于实现了 Dependency 接口的注册项，会检查依赖条件。
     * 记录加载耗时并输出日志信息。
     */
    private fun loadItem() {
        val time = measureTimeMillis {
            registered.clear()
            // 遍历配置文件中的所有节点
            for (section in config.getKeys(false).map(config::getConfigurationSection)) {
                val item = itemFactory.apply(section!!)
                // 检查依赖条件
                if (item is Dependency) {
                    if (!item.dependencies.checkAvailable()) {
                        continue
                    }
                }
                registered += item.id to item
            }
        }
        console().sendLang("loading-$registryId", registered.size, time)
    }
}