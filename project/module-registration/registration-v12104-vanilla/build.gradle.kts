dependencies {
    compileOnly(project(":project:module-registration:registration-v12104-paper"))
    compileOnly("ink.ptms.core:v12104:12104:mapped")
    compileOnly("ink.ptms.core:v12111:12111:mapped")
    compileOnly("paper:v12104:12104:core")
    // Mojang API
    compileOnly("com.mojang:brigadier:1.0.18")
}

// 子模块
taboolib { subproject = true }