@file:Suppress("PropertyName", "SpellCheckingInspection")

import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.37" apply false
    id("org.jetbrains.kotlin.jvm") version "2.1.10" apply false
    id("org.jetbrains.dokka") version "1.8.20" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.izzel.taboolib")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // TabooLib 配置
    // 这里的配置是全局的，如果你的项目有多个模块，这里的配置会被所有模块共享
    // 为了降低理解难度，使用这种更加无脑的配置方式
    configure<TabooLibExtension> {
        description {
            name(rootProject.name)
        }
        env {
            install(
                "platform-bukkit",
                "platform-bukkit-impl",
                "bukkit-hook",
                "bukkit-nms-stable",
                "bukkit-nms-tag",
                "bukkit-nms-tag-modern",
                "bukkit-nms",
                "bukkit-util",
                "bukkit-xseries",
                "minecraft-chat",
                "minecraft-i18n",
                "basic-configuration",
                "bukkit-ui",
                "incision",
                "minecraft-metrics"
            )
            forceDownloadInDev = false
            disableOnSkippedVersion = false
            disableWhenPrimitiveLoaderError = true
            enableLegacyDependencyResolver = true
            debug = true
        }
        version {
            taboolib = "6.3.0-test2-20260822-13"
        }
    }

    // 仓库
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    // 依赖
    dependencies {
        compileOnly(kotlin("stdlib"))
    }

    // 编译配置
    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll("-Xjvm-default=all", "-Xextended-compiler-checks")
        }
    }
}

gradle.buildFinished {
    buildDir.deleteRecursively()
}
