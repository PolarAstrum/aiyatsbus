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
package cc.polarastrum.aiyatsbus.core.script

import org.bukkit.command.CommandSender
import java.util.concurrent.CompletableFuture

/**
 * 脚本处理器接口
 *
 * 负责执行各种类型的脚本，如 Kether、JavaScript、Fluxon 等。
 * 提供脚本的调用、预热和变量传递功能。
 * 支持异步执行和批量脚本处理。
 *
 * @author mical
 * @since 2025/6/22 13:16
 */
interface ScriptHandler {

    /**
     * 执行脚本（字符串版本）
     *
     * 执行单个脚本字符串，支持变量传递和异步执行。
     *
     * @param source 脚本源代码
     * @param sender 命令发送者，用于权限检查和上下文
     * @param variables 传递给脚本的变量映射
     * @return 脚本执行结果的 CompletableFuture，如果执行失败则返回 null
     */
    fun invoke(source: String, sender: CommandSender?, variables: Map<String, Any?> = emptyMap()): CompletableFuture<Any?>?

    /**
     * 预热脚本（字符串版本）
     *
     * 预编译脚本以提高后续执行性能。
     *
     * @param source 脚本源代码
     */
    fun preheat(source: String)
}