package cc.polarastrum.aiyatsbus.module.script.fluxon.relocate

import cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
import cc.polarastrum.aiyatsbus.module.script.fluxon.handler.Fluxon
import cc.polarastrum.aiyatsbus.module.script.fluxon.handler.FluxonHandler
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.inject.ClassVisitorHandler
import taboolib.common.io.runningClassMapInJar
import taboolib.common.platform.Awake
import taboolib.library.reflex.ReflexClass
import kotlin.collections.iterator

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.relocate.FluxonRelocateLoader
 *
 * @author mical
 * @since 2026/1/3 14:02
 */
object FluxonRelocateLoader {

    private var propertySetted = false
    var needToTranslate = false

    @Awake(LifeCycle.INIT)
    fun init() {
        if (!propertySetted) {
            if (Bukkit.getServer().pluginManager.getPlugin("FluxonPlugin") != null) {
                propertySetted = true
                needToTranslate = true
            } else {
                FluxonScriptHandler.Companion.fluxonHandler = Fluxon
                propertySetted = true
            }
        }
        if (needToTranslate) {
            for ((_, clazz) in runningClassMapInJar) {
                if (clazz.structure.isAnnotationPresent(FluxonRelocate::class.java)) {
                    val newClazz = ReflexClass.of(AsmClassTranslation.createNewClass(clazz.name!!))
                    ClassVisitorHandler.injectAll(newClazz)
                    if (clazz.name == "cc.polarastrum.aiyatsbus.module.script.fluxon.handler.Fluxon") {
                        ClassVisitor.findInstance(newClazz).let { FluxonScriptHandler.Companion.fluxonHandler = it as FluxonHandler }
                    }
                }
            }
        }
    }
}
