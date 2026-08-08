@file:Suppress("UnstableApiUsage")

package cc.polarastrum.aiyatsbus.module.compat.libreforge

import cc.polarastrum.aiyatsbus.core.util.libreforgeEnabled
import com.google.gson.JsonParser
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.function.registerLifeCycleTask
import taboolib.common.platform.function.severe
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Finds and synchronously loads Libreforge when it is not already available.
 * The caller is responsible for choosing a safe Bukkit lifecycle phase.
 */
object LibreforgeDependencyLoader {

    const val FALLBACK_VERSION = "2026.31"

    private const val HOLDER_CLASS = "com.willfp.libreforge.Holder"
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Auxilor/libreforge/releases/latest"
    private const val MAVEN_REPOSITORY = "https://repo.auxilor.io/repository/maven-public"
    private const val BUFFER_SIZE = 64 * 1024
    private const val API_CONNECT_TIMEOUT = 1_000
    private const val API_READ_TIMEOUT = 2_000
    private const val DOWNLOAD_CONNECT_TIMEOUT = 2_000
    private const val DOWNLOAD_READ_TIMEOUT = 10_000

    /**
     * 从 Paper PluginInitializerManager 检查插件
     * 这个一定会存在，因为在开服的一瞬间就会提示存在的插件列表，例如：
     *
     * [01:14:06 INFO]: [PluginInitializerManager] Initializing plugins...
     * [01:14:06 INFO]: [PluginInitializerManager] Initialized 3 plugins
     * [01:14:06 INFO]: [PluginInitializerManager] Bukkit plugins (3):
     *  - Aiyatsbus (1.4.0), eco (2026.31), libreforge (2026.31)
     */
    private val plugins: List<String> = LaunchEntryPointHandler.INSTANCE.storage.values.flatMap { it.registeredProviders }.map { it.meta.name }

    private val relocation by unsafeLazy {
        Class.forName("!me.lucko.jarrelocator15.Relocation".substring(1))
    }
    private val jarRelocator by unsafeLazy {
        Class.forName("!me.lucko.jarrelocator15.JarRelocator".substring(1))
    }

    private fun checkPluginExist(name: String): Boolean {
        return name in plugins
    }

    /**
     * @return the newly loaded Bukkit plugin, or null when Libreforge was already visible.
     */
    @JvmStatic
    fun load() {
        // 拦截一下忘记删除 AiyatsbusLibreforge 的用户
        if (checkPluginExist("AiyatsbusLibreforge")) {
            """
                ——————————————————————————————————————————————————————————————————————
                自 Aiyatsbus 1.4.0 起，内置 EcoEnchants 附魔兼容，
                所以，AiyatsbusLibreforge 已停止维护。
                
                目前 EcoEnchants 附魔仍由 AiyatsbusLibreforge 驱动，
                但这并非长久之计，我们建议您迁移全部附魔到 Aiyatsbus 1.4.0。
                
                迁移方式：
                - 关闭服务器
                - 保留 AiyatsbusLibreforge 插件文件夹
                - 删除 AiyatsbusLibreforge 插件 (jar)
                - 保留 eco
                - 启动服务器
                
                迁移后的附魔在 plugins/Aiyatsbus/enchants/Packet-EcoEnchants/ 目录下。
                ——————————————————————————————————————————————————————————————————————
            """.trimIndent().lines().forEach { severe(it) }
            libreforgeEnabled = true
            return
        }

        if (!checkPluginExist("eco")) return
        if (checkPluginExist("libreforge")) {
            log("检测到 Libreforge 已经存在，不需要重复加载。")
            libreforgeEnabled = true
            return
        }

        log("未检测到 com.willfp.libreforge.Holder，开始查找 Libreforge。")

        val pluginsFolder = File("plugins")
        val currentJar = runCatching {
            File(javaClass.protectionDomain.codeSource.location.toURI()).canonicalFile
        }.getOrNull()
        val files = pluginsFolder.listFiles()
            ?.filter { it.isFile && it.canonicalFile != currentJar }
            .orEmpty()
        log("开始扫描 plugins 文件夹，共发现 ${files.size} 个文件。")

        findNestedLibreforge(files.filter { it.extension.equals("jar", ignoreCase = true) })
            ?.let {
                log("在 ${it.first.name} 中检测到内置 Libreforge，由该插件负责加载，不进行释放。")
                libreforgeEnabled = true
                return
            }

        log("其他 Jar 中没有找到 Libreforge，开始扫描非 Jar 扩展名文件。")
        findNestedLibreforge(files.filterNot { it.extension.equals("jar", ignoreCase = true) })
            ?.let {
                log("在非 Jar 扩展名文件 ${it.first.name} 中找到 ${it.second.name}，准备释放并加载。")
                loadPlugin(extract(it.first, it.second, pluginsFolder))
                return
            }

        log("本地没有找到 Libreforge，开始请求 GitHub 最新 Release 版本。")
        val version = runCatching { fetchLatestVersion() }
            .onSuccess { log("GitHub 返回 Libreforge 版本：$it。") }
            .onFailure { log("GitHub API 获取失败：${it.message}，使用回退版本 $FALLBACK_VERSION。") }
            .getOrDefault(FALLBACK_VERSION)
        log("准备下载 Libreforge 版本：$version。")
        val file = download(version, pluginsFolder)
        loadPlugin(file)
    }

    /**
     * 这个是检测插件里是否包含 libreforge 的
     */
    private fun isStandaloneLibreforge(file: File): Boolean {
        return file.name.startsWith("libreforge", ignoreCase = true) &&
            file.name.endsWith(".jar", ignoreCase = true)
    }

    /**
     * 这个是检测插件里是否包含 libreforge 的
     */
    private fun findNestedLibreforge(files: List<File>): Pair<File, JarEntry>? {
        for (file in files) {
            val entry = runCatching {
                JarFile(file).use { jar ->
                    jar.entries().asSequence().firstOrNull {
                        !it.isDirectory && isStandaloneLibreforge(File(it.name.substringAfterLast('/')))
                    }?.let { JarEntry(it) }
                }
            }.getOrNull()
            if (entry != null) {
                return file to entry
            }
        }
        return null
    }

    private fun extract(container: File, entry: JarEntry, pluginsFolder: File): File {
        val target = File(pluginsFolder, entry.name.substringAfterLast('/'))
        val temporary = File(pluginsFolder, ".${target.name}.tmp")
        try {
            JarFile(container).use { jar ->
                val source = jar.getJarEntry(entry.name)
                    ?: error("Nested Libreforge disappeared from ${container.name}")
                jar.getInputStream(source).buffered(BUFFER_SIZE).use { input ->
                    temporary.outputStream().buffered(BUFFER_SIZE).use(input::copyTo)
                }
            }
            moveIntoPlace(temporary, target)
            log("Libreforge 已释放到：${target.absolutePath}。")
            return target
        } catch (ex: Throwable) {
            temporary.delete()
            log("释放 Libreforge 失败：${ex.message}。")
            throw ex
        }
    }

    private fun fetchLatestVersion(): String {
        val connection = openConnection(LATEST_RELEASE_URL, API_CONNECT_TIMEOUT, API_READ_TIMEOUT)
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Aiyatsbus")
        try {
            check(connection.responseCode in 200..299) {
                "GitHub releases API returned HTTP ${connection.responseCode}"
            }
            val tag = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                JsonParser().parse(reader.readText()).asJsonObject.get("tag_name")?.asString
            }?.trim()?.removePrefix("v")
            check(!tag.isNullOrBlank()) { "GitHub release does not contain a version" }
            check(tag.matches(Regex("[A-Za-z0-9._-]+"))) { "Invalid Libreforge version: $tag" }
            return tag
        } finally {
            connection.disconnect()
        }
    }

    private fun download(version: String, pluginsFolder: File): File {
        val fileName = "libreforge-$version-shadow.jar"
        val target = File(pluginsFolder, fileName)
        if (target.isFile) {
            return target
        }

        val temporary = File(pluginsFolder, ".$fileName.tmp")
        val copyFile = File(pluginsFolder, target.nameWithoutExtension + ".bak.jar")
        val artifactUrl = "$MAVEN_REPOSITORY/com/willfp/libreforge/$version/$fileName"
        val connection = openConnection(artifactUrl, DOWNLOAD_CONNECT_TIMEOUT, DOWNLOAD_READ_TIMEOUT)
        connection.setRequestProperty("User-Agent", "Aiyatsbus")
        try {
            check(connection.responseCode in 200..299) {
                "Libreforge download returned HTTP ${connection.responseCode} for version $version"
            }
            connection.inputStream.buffered(BUFFER_SIZE).use { input ->
                temporary.outputStream().buffered(BUFFER_SIZE).use(input::copyTo)
            }
            check(temporary.length() > 0L) { "Downloaded Libreforge jar is empty" }
            moveIntoPlace(temporary, target)
            log("Libreforge 下载完成：${target.absolutePath}。")
        } catch (ex: Throwable) {
            temporary.delete()
            log("Libreforge 下载失败：${ex.message}。")
            throw ex
        } finally {
            connection.disconnect()
        }
        target.copyTo(copyFile)
        try {
            log("正在重定向 libreforge 中。")
            relocate(target, copyFile)
            copyFile.delete()
            log("成功重定向 libreforge。")
            return target
        } catch (ex: Throwable) {
            target.delete()
            copyFile.delete()
            log("libreforge 重定向失败：${ex.message}。")
            throw ex
        }
    }

    private fun relocate(file: File, copyFile: File) {
        val rel = arrayListOf<Any>()
        // 我已力竭
        rel += relocation.invokeConstructor("!kotlin".substring(1), "!com.willfp.eco.libs.kotlin".substring(1))
        rel += relocation.invokeConstructor("!org.jetbrains.kotlin".substring(1), "!com.willfp.eco.libs.kotlin.jetbrains".substring(1))
        jarRelocator.invokeConstructor(copyFile, file, rel).invokeMethod<Any>("run")
    }

    private fun openConnection(url: String, connectTimeout: Int, readTimeout: Int): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            requestMethod = "GET"
            this.connectTimeout = connectTimeout
            this.readTimeout = readTimeout
            useCaches = false
        }
    }

    private fun moveIntoPlace(source: File, target: File) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun loadPlugin(file: File) {
        registerLifeCycleTask(LifeCycle.LOAD, -999) {
            log("开始调用 Bukkit 加载 Libreforge：${file.name}。")
            try {
                Bukkit.getPluginManager().loadPlugin(file)
                    ?: error("Bukkit 无法加载 ${file.name}。")
            } catch (ex: Throwable) {
                log("Bukkit 加载 Libreforge 失败：${ex.message}。")
                throw ex
            }
            check(Bukkit.getPluginManager().getPlugin("libreforge") != null) {
                "Bukkit 已加载 ${file.name}，但仍然无法找到 $HOLDER_CLASS。"
            }
            log("Libreforge 加载成功，Holder 已可用。")
            libreforgeEnabled = true
        }
    }

    private fun log(message: String) {
        println("[AiyatsbusLibreforge] $message")
    }
}
