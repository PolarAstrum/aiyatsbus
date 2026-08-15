package cc.polarastrum.aiyatsbus.module.script.fluxon

import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.PrimitiveLoader
import taboolib.common.PrimitiveSettings
import taboolib.common.env.DependencyScope
import taboolib.common.env.JarRelocation
import taboolib.common.env.RuntimeEnv
import taboolib.common.inject.ClassVisitorHandler
import taboolib.common.io.runningClassMap
import taboolib.common.platform.Awake
import java.io.File

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonTestLoader
 *
 * @author mical
 * @since 2026/1/5 23:56
 */
object FluxonChecker {

    private const val CORE_PACKAGE = "cc.polarastrum.aiyatsbus.module.script.fluxon.core"
    private const val FLUXON_VERSION = "1.7.1"
    private const val FP_VERSION = "1.1.11"

    private var isCentral = false

    @Awake(LifeCycle.CONST)
    fun download() {
        if (Bukkit.getPluginManager().getPlugin("FluxonPlugin") != null) {
            isCentral = true
            return
        }
        val rel = ArrayList<JarRelocation>()
        rel.add(JarRelocation("!org.tabooproject.fluxon".substring(1), "!cc.polarastrum.aiyatsbus.module.script.fluxon.core".substring(1)))
        val scope = listOf(DependencyScope.RUNTIME, DependencyScope.COMPILE)
        load("!org.tabooproject.fluxon:core:$FLUXON_VERSION".substring(1), scope, rel)
        load("!org.tabooproject.fluxon:inst-core:$FLUXON_VERSION".substring(1), scope, rel)
        if (!PrimitiveSettings.IS_ISOLATED_MODE) {
            rel.add(JarRelocation(RuntimeEnv.KOTLIN_ID + ".", PrimitiveSettings.getRelocatedKotlinVersion() + "."))
            rel.add(JarRelocation(RuntimeEnv.KOTLIN_COROUTINES_ID + ".", PrimitiveSettings.getRelocatedKotlinCoroutinesVersion() + "."))
            rel.add(JarRelocation(PrimitiveSettings.ID, PrimitiveLoader.TABOOLIB_PACKAGE_NAME))
        }
        load("!org.tabooproject.fluxon.plugin:core:$FP_VERSION".substring(1), scope, rel)
        load("!org.tabooproject.fluxon.plugin:common:$FP_VERSION".substring(1), scope, rel)
        load("!org.tabooproject.fluxon.plugin:platform-bukkit:$FP_VERSION".substring(1), scope, rel)
    }

    private fun load(url: String, scope: List<DependencyScope>, rel: List<JarRelocation>) {
        RuntimeEnv.ENV_DEPENDENCY.loadDependency(
            url,
            File(PrimitiveSettings.FILE_LIBS),
            rel,
            "https://repo.tabooproject.org/repository/releases",
            false,
            false,
            false,
            scope,
            false
        )
    }

    @Awake(LifeCycle.INIT)
    fun init() {
        if (isCentral) return
        runningClassMap.filter { isSupportedRuntimeClass(it.key) && it.key.startsWith(CORE_PACKAGE) }
            .forEach { (_, clazz) ->
                if (ClassVisitorHandler.checkPlatform(clazz) && ClassVisitorHandler.checkRequires(clazz)) {
                    ClassVisitorHandler.injectAll(clazz)
                }
            }
    }

    private fun isSupportedRuntimeClass(name: String): Boolean {
        val requiredClass = when (name) {
            "$CORE_PACKAGE.platform.bukkit.function.bukkit.block.data.type.FnPointedDripstone" -> "org.bukkit.block.data.type.PointedDripstone\$Thickness"
            "$CORE_PACKAGE.platform.bukkit.function.bukkit.event.world.FnTimeSkipEvent" -> "org.bukkit.event.world.TimeSkipEvent\$SkipReason"
            else -> return true
        }
        return classExists(requiredClass)
    }

    private fun classExists(name: String): Boolean {
        return runCatching {
            Class.forName(name, false, ClassVisitorHandler::class.java.classLoader)
        }.isSuccess
    }
}
