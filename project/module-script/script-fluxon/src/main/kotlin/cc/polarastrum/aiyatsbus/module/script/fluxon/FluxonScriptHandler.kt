package cc.polarastrum.aiyatsbus.module.script.fluxon

import cc.polarastrum.aiyatsbus.core.script.ScriptHandler
import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.tabooproject.fluxon.Fluxon
import org.tabooproject.fluxon.interpreter.Interpreter
import org.tabooproject.fluxon.interpreter.ReturnValue
import org.tabooproject.fluxon.parser.ParseResult
import org.tabooproject.fluxon.runtime.Environment
import org.tabooproject.fluxon.runtime.FluxonRuntime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class FluxonScriptHandler : ScriptHandler {

    private val scriptCache = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build<String, List<ParseResult>>()

    init {

    }

    /**
     * 执行脚本
     *
     * @param script      脚本文本
     * @param useCache    是否使用缓存，如果脚本修改频繁建议不使用缓存
     * @param env         脚本执行环境
     */
    fun invoke(script: String, useCache: Boolean = true, env: Environment.() -> Unit = {}): Any? {
        // 构建脚本环境
        val environment = FluxonRuntime.getInstance().newEnvironment().also(env)
        // 解析脚本（如果有缓存则跳过解析过程）
        val parsed = if (useCache) {
            scriptCache.get(script) { Fluxon.parse(script.removePrefix(";"), environment) }
        } else {
            Fluxon.parse(script.removePrefix(";"), environment)
        }
        val interpreter = Interpreter(environment)
        return try {
            interpreter.execute(parsed)
        } catch (ex: ReturnValue) {
            ex.value
        }
    }

    override fun invoke(
        source: String,
        sender: CommandSender?,
        variables: Map<String, Any?>
    ): CompletableFuture<Any?>? {
        return CompletableFuture.supplyAsync {
            invoke(source, true) {
                variables.forEach { (key, value) -> defineRootVariable(key, value) }
                defineRootVariable("sender", sender)
                if (sender is Player) {
                    defineRootVariable("player", sender)
                }
            }
        }
    }

    override fun preheat(source: String) {
    }
}