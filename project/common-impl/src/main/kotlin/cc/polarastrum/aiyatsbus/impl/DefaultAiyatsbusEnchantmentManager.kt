package cc.polarastrum.aiyatsbus.impl

import cc.polarastrum.aiyatsbus.core.*
import cc.polarastrum.aiyatsbus.core.compat.EnchantRegistrationHooks
import cc.polarastrum.aiyatsbus.core.registration.modern.ModernEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.core.util.FileWatcher.isProcessingByWatcher
import cc.polarastrum.aiyatsbus.core.util.FileWatcher.unwatch
import cc.polarastrum.aiyatsbus.core.util.FileWatcher.watch
import cc.polarastrum.aiyatsbus.core.util.YamlUpdater
import cc.polarastrum.aiyatsbus.core.util.aiyatsbusLibreforgeEnabled
import cc.polarastrum.aiyatsbus.core.util.deepRead
import cc.polarastrum.aiyatsbus.core.util.libreforgeEnabled
import cc.polarastrum.aiyatsbus.core.util.reloadable
import cc.polarastrum.aiyatsbus.core.util.safeguard
import cc.polarastrum.aiyatsbus.impl.DefaultAiyatsbusAPI.Companion.proxy
import cc.polarastrum.aiyatsbus.impl.registration.legacy.DefaultLegacyEnchantmentRegisterer
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.LibreforgeEnchants
import cc.polarastrum.aiyatsbus.module.compat.libreforge.enchant.impl.HardcodedLibreforgeAiyatsbusEnchantment
import cc.polarastrum.aiyatsbus.module.compat.libreforge.feature.MigrateEcoEnchants
import org.bukkit.NamespacedKey
import taboolib.common.LifeCycle
import taboolib.common.TabooLib
import taboolib.common.io.newFolder
import taboolib.common.io.runningResourcesInJar
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.releaseResourceFile
import taboolib.common.util.replaceWithOrder
import taboolib.common.util.t
import taboolib.module.configuration.Configuration
import taboolib.module.nms.MinecraftVersion.versionId
import taboolib.platform.util.onlinePlayers
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 默认 Aiyatsbus 附魔管理器实现
 * 
 * 提供附魔的注册、加载、查询和管理功能。
 * 支持文件监听、自动重载、默认附魔释放等高级功能。
 * 使用并发哈希映射保证线程安全。
 *
 * @author mical
 * @since 2024/2/17 16:22
 */
class DefaultAiyatsbusEnchantmentManager : AiyatsbusEnchantmentManager {

    /** 按命名空间键存储的附魔映射 */
    private val byKeyMap = ConcurrentHashMap<NamespacedKey, AiyatsbusEnchantment>()
    /** 按字符串键存储的附魔映射 */
    private val byKeyStringMap = ConcurrentHashMap<String, AiyatsbusEnchantment>()
    /** 按名称存储的附魔映射 */
    private val byNameMap = ConcurrentHashMap<String, AiyatsbusEnchantment>()

    /** 等待被注册的附魔 */
    private val enchantmentsToRegister = CopyOnWriteArraySet<AiyatsbusEnchantmentBase>()

    override fun getEnchants(): Map<NamespacedKey, AiyatsbusEnchantment> {
        return byKeyMap
    }

    override fun getEnchant(key: NamespacedKey): AiyatsbusEnchantment? {
        return byKeyMap[key]
    }

    override fun getEnchant(key: String): AiyatsbusEnchantment? {
        return byKeyStringMap[key]
    }

    override fun getByName(name: String): AiyatsbusEnchantment? {
        return byNameMap[name]
    }

    override fun register(enchantment: AiyatsbusEnchantmentBase) {
        // 如果不是内置附魔, 就添加进待注册附魔
        // 不从列表中删除, 是为了防止重载丢失第三方附魔的情况出现
        if (enchantment !is InternalAiyatsbusEnchantmentBase) {
            enchantmentsToRegister += enchantment
        }
        // 在 LOAD 生命周期后调用本函数, 就注册该附魔
        if (TabooLib.getCurrentLifeCycle() != LifeCycle.LOAD) {
            val ench = Aiyatsbus.api().getEnchantmentRegisterer().register(enchantment) as AiyatsbusEnchantment
            byKeyMap[enchantment.enchantmentKey] = ench
            byKeyStringMap[enchantment.basicData.id] = ench
            byNameMap[enchantment.basicData.originName] = ench
            if (ench is LibreforgeAiyatsbusEnchantment) {
                LibreforgeEnchants[enchantment.enchantmentKey] = ench
            }
        }
        EnchantRegistrationHooks.registerHooks()
    }

    override fun unregister(enchantment: AiyatsbusEnchantment) {
        enchantment.mechanism?.close()
        enchantmentsToRegister.remove(enchantment)
        Aiyatsbus.api().getEnchantmentRegisterer().unregister(enchantment)
        byKeyMap -= enchantment.enchantmentKey
        byKeyStringMap -= enchantment.basicData.id
        byNameMap -= enchantment.basicData.originName
        if (enchantment is LibreforgeAiyatsbusEnchantment) {
            LibreforgeEnchants -= enchantment.enchantmentKey
            if (enchantment is HardcodedLibreforgeAiyatsbusEnchantment) {
                enchantment.remove()
            }
        }
    }

    override fun loadEnchantments() {
        // 如果是重载, 记录当前附魔数量, 如果不一致得刷新在线玩家的附魔表
        val enchantmentCount = byKeyMap.size
        clearEnchantments()

        val enchantsFolder = newFolder(getDataFolder(), "enchants")
        
        // 如果附魔文件夹为空，自动释放默认附魔
        if (enchantsFolder.listFiles()?.none { it.isDirectory } == true && AiyatsbusSettings.autoReleaseEnchants) {
            releaseDefaultEnchants()
        }

        val startTime = System.currentTimeMillis()
        
        // 加载所有附魔文件
        enchantsFolder.listFiles { file, _ -> file.isDirectory }?.toList()?.let { directories ->
            directories.flatMap { it.deepRead("yml") }
                .forEach { file -> loadFromFile(file) }
        }

        // 如果是重载, 就再注册一次防止附属附魔丢失
        enchantmentsToRegister.forEach { register(it) }

        console().sendLang("loading-enchantments", byKeyMap.size, System.currentTimeMillis() - startTime)

        if (enchantmentCount < byKeyMap.size) {
            onlinePlayers.forEach {
                Aiyatsbus.api().getMinecraftAPI().getPacketHandler()
                    .synchronizeRegistries(it)
            }
        }
    }

    override fun loadFromFile(file: File) {
        val relativePath = file.path.substring(file.path.indexOf("enchants" + File.separator), file.path.length)
        val config = YamlUpdater.loadFromFile(relativePath, AiyatsbusSettings.enableUpdater, AiyatsbusSettings.updateContents)

        val enchant = loadEnchant(file, config) ?: return
        if (!enchant.dependencies.checkAvailable()) return

        register(enchant)
        if (enchant is HardcodedLibreforgeAiyatsbusEnchantment) {
            enchant.register()
        }
        enchant.mechanism?.init()
        setupFileWatcher(file, relativePath, NamespacedKey.minecraft(enchant.id), enchant.id)
    }

    fun loadEnchant(file: File, originalConfig: Configuration): AiyatsbusEnchantmentBase? {
        var config = originalConfig
        // 迁移 libreforge 附魔
        if (!config.contains("basic") && config.contains("max-level")) {
            if (!libreforgeEnabled) return null
            MigrateEcoEnchants.migrate(file, file.nameWithoutExtension, config)
            config = Configuration.loadFromFile(file)
        }

        val id = config["basic.id"].toString()
        val isVanilla = (config["alternative.is-vanilla"] ?: config["alternative.is_vanilla"]) == true
        val isEco = (config["alternative.is-eco"] ?: config["alternative.is_eco"]) == true

        if (file.name.startsWith("_")) return null // 在这里判断是为了让 _ 开头的附魔也能正常迁移

        return when {
            isVanilla -> VanillaAiyatsbusEnchantmentBase(id, file, config)
            // 必须要同时判断
            // 如果不存在 libreforge，需要把其当成普通附魔加载
            // 否则会丢附魔
            isEco && libreforgeEnabled -> {
                if (aiyatsbusLibreforgeEnabled) return null
                if (MigrateEcoEnchants.isMissingPlugins(file)) {
                    return null
                }
                LibreforgeAiyatsbusEnchantment.newEnchant(id, file, config)
            }
            else -> InternalAiyatsbusEnchantmentBase(id, file, config)
        }
    }

    /**
     * 释放默认附魔文件
     * 
     * 从插件 JAR 包中释放内置的默认附魔配置文件
     */
    private fun releaseDefaultEnchants() {
        runningResourcesInJar.keys
            .filter { path ->
                path.endsWith(".yml") &&
                path.startsWith("enchants/") &&
                path.count { it == '/' } >= 2
            }
            .forEach { releaseResourceFile(it) }
    }

    /**
     * 设置文件监听器
     * 
     * 监听附魔配置文件的变更，支持热重载功能
     * 
     * @param file 要监听的文件
     * @param relativePath 相对路径
     * @param key 附魔的命名空间键
     * @param id 附魔 ID
     */
    private fun setupFileWatcher(file: File, relativePath: String, key: NamespacedKey, id: String) {
        file.watch { watchedFile ->
            if (watchedFile.isProcessingByWatcher) {
                watchedFile.isProcessingByWatcher = false
                return@watch
            }

            val startTime = System.currentTimeMillis()
            
            // 尝试从资源文件更新配置
            val resourceStream = javaClass.classLoader.getResourceAsStream(relativePath.replace('\\', '/')) // 真傻逼啊
            if (AiyatsbusSettings.enableUpdater && resourceStream != null) {
                console().sendLang("enchantment-reload-failed", id)
                watchedFile.isProcessingByWatcher = true
                YamlUpdater.loadFromFile(
                    relativePath, 
                    AiyatsbusSettings.enableUpdater, 
                    AiyatsbusSettings.updateContents, 
                    Configuration.loadFromInputStream(resourceStream)
                )
                return@watch
            }

            // 重新加载附魔
            reloadEnchantment(watchedFile, key, id, startTime)
        }
    }

    /**
     * 重新加载单个附魔
     * 
     * 注销旧附魔并注册新附魔，更新在线玩家的背包
     * 
     * @param file 附魔配置文件
     * @param key 附魔的命名空间键
     * @param id 附魔 ID
     * @param startTime 开始时间戳
     */
    private fun reloadEnchantment(file: File, key: NamespacedKey, id: String, startTime: Long) {
        val oldEnchant = getEnchant(key) ?: return
        oldEnchant.mechanism?.close()
        unregister(oldEnchant)

        if (file.exists()) {
            val config = Configuration.loadFromFile(file)
            val newEnchant = loadEnchant(file, config) ?: return
            if (!newEnchant.dependencies.checkAvailable()) return

            register(newEnchant)
            if (newEnchant is HardcodedLibreforgeAiyatsbusEnchantment) {
                newEnchant.register()
            }
            getEnchant(key)!!.mechanism?.init()
        }
        
        console().sendLang("enchantment-reload", id, System.currentTimeMillis() - startTime)
        EnchantRegistrationHooks.unregisterHooks()
        EnchantRegistrationHooks.registerHooks()
    }

    override fun clearEnchantments() {
        for (enchant in byKeyMap.values) {
            // 不卸载外部附魔
            if (enchant in enchantmentsToRegister) continue
            enchant.file?.isProcessingByWatcher = false
            enchant.file?.unwatch()
            unregister(enchant)
        }
    }

    /**
     * 附魔管理器初始化伴生对象
     */
    companion object {

        private const val PACKAGE = "cc.polarastrum.aiyatsbus.impl.registration.v{0}_nms.DefaultModernEnchantmentRegisterer"

        /**
         * 初始化附魔管理器
         * 
         * 在系统常量阶段注册 API，设置现代注册器的生命周期任务。
         * 对于现代注册器，在启用阶段按优先级加载附魔。
         */
        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<AiyatsbusEnchantmentManager>(DefaultAiyatsbusEnchantmentManager())

            val registerer = when {
                versionId >= 260100 -> modern(260100)
                versionId >= 12104 -> modern(12104)
                versionId >= 12102 -> modern(12103)
                versionId >= 12100 -> modern(12100)
                versionId >= 12005 -> error("""
                    Aiyatsbus 不支持 Minecraft 1.20.5 或 1.20.6。
                    Aiyatsbus doesn't support Minecraft 1.20.5 or 1.20.6.
                """.t())
                versionId >= 12003 -> modern(12004)
                else -> DefaultLegacyEnchantmentRegisterer
            }
            DefaultAiyatsbusAPI.registerer = registerer

            if (registerer is ModernEnchantmentRegisterer) {
                safeguard("注册表替换", "registry replacer") { registerer.replaceRegistry() }
                registerLifeCycleTask(LifeCycle.ACTIVE) {
                    registerer.replaceRegistry()
                }
                registerLifeCycleTask(LifeCycle.DISABLE) {
                    Aiyatsbus.api().getEnchantmentManager().clearEnchantments()
                }
            }
            reloadable {
                registerLifeCycleTask(LifeCycle.ENABLE, StandardPriorities.ENCHANTMENT) {
                    safeguard("附魔", "enchantments") { Aiyatsbus.api().getEnchantmentManager().loadEnchantments() }
                }
            }
        }

        private fun modern(versionId: Int): ModernEnchantmentRegisterer {
            return proxy(PACKAGE.replaceWithOrder(versionId))
        }
    }
}