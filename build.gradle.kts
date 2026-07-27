@file:Suppress("PropertyName", "SpellCheckingInspection")

import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.37" apply false
    id("org.jetbrains.kotlin.jvm") version "1.8.22" apply false
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
                CommandHelper,
                Bukkit,
                BukkitHook,
                BukkitNMSUtil,
                BukkitUI,
                BukkitUtil,
                I18n,
                Incision,
                MinecraftChat,
                MinecraftEffect,
                Metrics,
            )
            forceDownloadInDev = false
            disableOnSkippedVersion = false
            disableWhenPrimitiveLoaderError = true
            enableLegacyDependencyResolver = true
        }
        version {
            taboolib = "6.3.0-test-260728-1"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjvm-default=all", "-Xextended-compiler-checks")
        }
    }
}

gradle.buildFinished {
    buildDir.deleteRecursively()
}
