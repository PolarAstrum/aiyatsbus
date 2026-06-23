package cc.polarastrum.aiyatsbus.module.script.fluxon.handler

import cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
import cc.polarastrum.aiyatsbus.module.script.fluxon.relocate.FluxonRelocate
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tabooproject.fluxon.Fluxon
import org.tabooproject.fluxon.FluxonPlugin
import org.tabooproject.fluxon.compiler.CompilationContext
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.RuntimeScriptBase
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError
import org.tabooproject.fluxon.util.printError
import taboolib.common.Requires
import taboolib.common.io.newFile
import taboolib.common.io.newFolder
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.warning
import taboolib.platform.BukkitPlugin
import java.nio.file.Files
import java.text.ParseException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 本类无实际意义，供 ASM 生成
 *
 * @author mical
 * @since 2026/1/3 13:55
 */
@Requires(missingClasses = ["!org.tabooproject.fluxon.ParseScript"])
@FluxonRelocate
object Fluxon : FluxonHandler {

    // 服了脚本
    private val compiledScripts = ConcurrentHashMap<String, RuntimeScriptBase>()
    private val classLoader = FluxonClassLoader(BukkitPlugin::class.java.classLoader)
    private val environment = FluxonRuntime.getInstance().newEnvironment()

    init {
        FluxonPlugin.DEFAULT_ALLOW_EXECUTE_TASK_ON_NON_SCRIPT_ENV = true
    }

    override fun invoke(
        source: String,
        id: String,
        sender: CommandSender?,
        variables: Map<String, Any?>
    ): Any? {
        if (!compiledScripts.containsKey(id)) preheat(source, id)

        val scriptBase = compiledScripts[id] ?: return null

        val environment = FluxonRuntime.getInstance().newEnvironment()
        variables.forEach { (key, value) -> environment.defineRootVariable(key, value) }
        environment.defineRootVariable("sender", sender)
        if (sender is Player) {
            environment.defineRootVariable("player", sender)
        }

        return try {
            scriptBase.eval(environment)?.exceptFluxonCompletableFutureError()
        } catch (ex: FluxonRuntimeError) {
            ex.printError()
            null
        } catch (ex: Throwable) {
            ex.printStackTrace()
            null
        }
    }

    override fun preheat(source: String, id: String) {
        try {
            val result = Fluxon.compile(
                environment,
                CompilationContext(source).apply { packageAutoImport += FluxonScriptHandler.DEFAULT_PACKAGE_AUTO_IMPORT },
                id + System.currentTimeMillis(), // 这里要加一个时间，因为不能加载一样名称的类
                classLoader
            )
            Files.write(newFile(newFolder(getDataFolder(), "classes"), "${id}.class").toPath(), result.mainClass)
            compiledScripts[id] = result.createInstance(classLoader) as RuntimeScriptBase
        } catch (ex: ParseException) {
            warning("编译脚本 $source 时发生错误:")
            ex.printStackTrace()
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    fun Any?.exceptFluxonCompletableFutureError(): Any? {
        return if (this is CompletableFuture<*>) {
            this.exceptionally { ex ->
                if (ex is FluxonRuntimeError) {
                    ex.printError()
                }
                null
            }
        } else {
            this
        }
    }
}