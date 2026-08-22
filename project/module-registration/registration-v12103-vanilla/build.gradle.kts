dependencies {
    compileOnly(project(":project:module-registration:registration-v12103-paper"))
    compileOnly("ink.ptms.core:v12103:12103:mapped")
    // Mojang API
    compileOnly("com.mojang:brigadier:1.0.18")
}

// 子模块
taboolib { subproject = true }