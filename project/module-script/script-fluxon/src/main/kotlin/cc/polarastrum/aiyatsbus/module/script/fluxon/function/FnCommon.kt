package cc.polarastrum.aiyatsbus.module.script.fluxon.function

import cc.polarastrum.aiyatsbus.core.sendDebug
import cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
import cc.polarastrum.aiyatsbus.module.script.fluxon.relocate.FluxonRelocate
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.FunctionSignature.returns
import org.tabooproject.fluxon.runtime.Type
import org.tabooproject.fluxon.runtime.java.Export
import taboolib.common.LifeCycle
import taboolib.common.Requires
import taboolib.common.platform.Awake
import taboolib.module.chat.component

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.function.FunctionVariables
 *
 * @author lynn
 * @since 2025/7/5
 */
@Requires(missingClasses = ["!org.tabooproject.fluxon.ParseScript"])
@FluxonRelocate
object FnCommon {

    val TYPE = Type.fromClass(FnCommon::class.java)!!

    @Awake(LifeCycle.LOAD)
    fun init() {
        FluxonScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT += "aiy:common"
        with(FluxonRuntime.getInstance()) {
            registerFunction("aiy:common", "common", returns(TYPE).noParams()) { it.returnRef = FnCommon }
            exportRegistry.registerClass(FnCommon::class.java, "aiy:common")
        }
    }

    @Export
    fun debug(str: String) {
        sendDebug(str.component().buildColored().toLegacyText())
    }
}