/*
 * This file is part of Vulpecula, licensed under the MIT License.
 *
 *  Copyright (c) 2018 Bkm016
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package cc.polarastrum.aiyatsbus.module.script.kether.action.game.item

import taboolib.library.reflex.Reflex.Companion.invokeMethod

/**
 * Vulpecula
 * top.lanscarlos.vulpecula.bacikal.action.item
 *
 * @author Lanscarlos
 * @since 2023-03-24 23:43
 */
object ActionItemUnbreakable : ActionItem.Resolver {

    override val name: Array<String> = arrayOf("unbreakable", "unbreak")

    /**
     * item unbreakable &item to true/false
     * */
    override fun resolve(reader: ActionItem.Reader): ActionItem.Handler<out Any?> {
        return reader.transfer {
            combine(
                source(),
                trim("to", then = bool(false))
            ) { item, unbreakable ->
                val meta = item.itemMeta ?: return@combine item

                try {
                    meta.isUnbreakable = unbreakable
                } catch (ex: NoSuchMethodError) {
                    try {
                        meta.invokeMethod<Any>("spigot")!!.invokeMethod<Any>("setUnbreakable", unbreakable)
                    } catch (ignored: NoSuchMethodException) {
                        // 忽略
                    }
                }

                item.also { it.itemMeta = meta }
            }
        }
    }
}