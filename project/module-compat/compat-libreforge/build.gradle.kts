import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    // 引入 API
    compileOnly(project(":project:common"))

    // 服务端
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("paper:v12111:12111:core")
    compileOnly("ink.ptms.core:v11605:11605")

    // eco, libreforge
    // FIXME 用一个特别旧的版本，防止 kotlin 版本不一致兼容错误
    //       等迫不得已再更新
    compileOnly("com.willfp:eco:6.67.1")
    compileOnly("com.willfp:libreforge:4.56.4:shadow")

    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.5")
    compileOnly("me.lucko:jar-relocator:1.7")
}

// 子模块
taboolib { subproject = true }