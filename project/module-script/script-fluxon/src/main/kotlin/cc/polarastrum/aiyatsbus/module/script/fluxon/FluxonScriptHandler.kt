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
package cc.polarastrum.aiyatsbus.module.script.fluxon

import cc.polarastrum.aiyatsbus.core.script.ScriptHandler
import cc.polarastrum.aiyatsbus.core.util.coerceInt
import org.bukkit.command.CommandSender
import org.tabooproject.fluxon.Fluxon
import org.tabooproject.fluxon.interpreter.bytecode.FluxonClassLoader
import org.tabooproject.fluxon.runtime.FluxonRuntime
import org.tabooproject.fluxon.runtime.RuntimeScriptBase
import taboolib.common.classloader.IsolatedClassLoader
import taboolib.platform.BukkitPlugin
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Aiyatsbus
 * cc.polarastrum.aiyatsbus.module.script.fluxon.FluxonScriptHandler
 *
 * @author mical
 * @since 2025/6/22 13:24
 */
//@RuntimeDependency(
//    repository = "file:///Users/lynn/.m2/repository",
//    value = "!org.tabooproject.fluxon:core:1.0-SNAPSHOT",
//    test = "!org.tabooproject.fluxon.Fluxon"
//)
class FluxonScriptHandler : ScriptHandler {

    // 服了脚本
    private val compiledScripts = ConcurrentHashMap<UUID, RuntimeScriptBase>()
    private val classLoader = FluxonClassLoader(BukkitPlugin::class.java.classLoader)

    override fun invoke(
        source: String,
        sender: CommandSender?,
        variables: Map<String, Any?>
    ): CompletableFuture<Any?>? {
        val uuid = UUID.nameUUIDFromBytes(source.toByteArray())
        if (!compiledScripts.containsKey(uuid)) preheat(source)

        val base = compiledScripts[uuid] ?: return null
        return CompletableFuture.supplyAsync {
            base.eval(FluxonRuntime.getInstance().newEnvironment().apply {
                variables.forEach { (key, value) -> defineVariable(key, value) }
            })
        }
    }

    override fun invoke(
        source: List<String>,
        sender: CommandSender?,
        variables: Map<String, Any?>
    ): CompletableFuture<Any?>? {
        return invoke(source.joinToString("\n"), sender, variables)
    }

    override fun preheat(source: String) {
        // 生成一个唯一的标识
        // 脚本无变动, 则无需重复预热
        val uuid = UUID.nameUUIDFromBytes(source.toByteArray())
        if (compiledScripts.containsKey(uuid)) return

        var className = uuid.toString().replace("-", "")
        // 生成唯一的类名
        // 如果开头是数字的话, 就要加一个字母以防无效类名
        // 我喜欢 T
        if (className.get(0).coerceInt(-1) >= 0) {
            className = "T$className"
        }

        val result = Fluxon.compile(source, className)
        println(Fluxon::class.java.classLoader.name)
        val compiledScript = result.defineClass(classLoader)
        println(compiledScript)
        compiledScripts[uuid] = compiledScript.newInstance() as RuntimeScriptBase
    }

    override fun preheat(source: List<String>) {
        return preheat(source.joinToString("\n"))
    }
}