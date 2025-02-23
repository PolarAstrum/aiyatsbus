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
package cc.polarastrum.aiyatsbus.module.kether

import taboolib.common.inject.ClassVisitor.findInstance
import taboolib.common.platform.function.warning
import taboolib.library.reflex.ClassMethod
import taboolib.library.reflex.ReflexClass
import taboolib.module.kether.Kether
import taboolib.module.kether.KetherLoader
import taboolib.module.kether.ScriptActionParser

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.AiyatsbusRegistry
 *
 * @author Lanscarlos
 * @since 2023-03-19 22:08
 */
object AiyatsbusKetherRegistry : ClassInjector() {

    /**
     * 访问函数
     * */
    override fun visit(method: ClassMethod, owner: ReflexClass) {
        registerAction(method, owner)
    }

    /**
     * 访问类
     * */
    override fun visitStart(owner: ReflexClass) {
        registerProperty(owner)
    }

    /**
     * 函数式语句注册
     * */
    private fun registerAction(method: ClassMethod, owner: ReflexClass) {
        if (!method.isAnnotationPresent(AiyatsbusParser::class.java) || method.returnType != ScriptActionParser::class.java) {
            return
        }

        // 加载注解
        val annotation = method.getAnnotation(AiyatsbusParser::class.java)
        val value = annotation.property<Any>("value") ?: return

        // FIXME 奇怪的问题, annotation 为什么能获取到 List?
        val values = if (value is List<*>) value.toTypedArray() else value as Array<*>

        // 尝试获取实例
        val instance = findInstance(owner)

        // 获取语句对象
        val parser = if (instance != null) {
            method.invoke(instance) as ScriptActionParser<*>
        } else {
            method.invokeStatic() as ScriptActionParser<*>
        }

        val shared = annotation.property("shared", false)
        if (shared) {
            // 注册语句 公有
            KetherLoader.registerParser(parser, values.map { it.toString() }.toTypedArray(), "aiyatsbus", shared)
        } else {
            // 注册语句 私有
            for (name in values) {
                Kether.scriptRegistry.registerAction("aiyatsbus", name.toString(), parser)
            }
        }
    }

    /**
     * 类式属性注册
     * */
    private fun registerProperty(owner: ReflexClass) {
        if (!owner.hasAnnotation(AiyatsbusProperty::class.java)) {
            return
        }

//        if (!AiyatsbusGenericProperty::class.java.assignableFrom(owner)) {
//            return
//        }

        // 加载注解
        val annotation = owner.getAnnotation(AiyatsbusProperty::class.java)

        // 尝试获取实例
        val instance = findInstance(owner)

        // 获取属性对象
        val property = if (instance != null) {
            instance as AiyatsbusGenericProperty<*>
        } else try {
            // 尝试实例化
            owner.structure.owner.instance?.getDeclaredConstructor()?.newInstance() as AiyatsbusGenericProperty<*>
        } catch (ex: Exception) {
            warning("Property \"${owner.name}\" must have a empty constructor.")
            return
        }

        // 注册属性 私有
        try {
            Kether.registeredScriptProperty.computeIfAbsent(annotation.type("bind").instance as Class<*>) { HashMap() }[property.id] = property
        } catch (_: ClassNotFoundException) {
        }
    }

    /**
     * 供外部调用的属性注册函数
     */
    @JvmStatic
    fun <T : AiyatsbusGenericProperty<*>> registerProperty(instance: T) {
        val clazz = instance.javaClass
        if (!clazz.isAnnotationPresent(AiyatsbusProperty::class.java)) {
            return
        }

        if (!AiyatsbusGenericProperty::class.java.isAssignableFrom(clazz)) {
            return
        }

        // 加载注解
        val annotation = clazz.getAnnotation(AiyatsbusProperty::class.java)

        // 注册属性 私有
        Kether.registeredScriptProperty.computeIfAbsent(annotation.bind.java) { HashMap() }[instance.id] = instance
    }
}