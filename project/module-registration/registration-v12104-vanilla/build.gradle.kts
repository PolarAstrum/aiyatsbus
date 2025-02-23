dependencies {
    compileOnly(project(":project:module-registration:registration-v12104-paper"))
    compileOnly("ink.ptms.core:v12104:12104:mapped")
    compileOnly("paper:v12104:12104:core")
//    compileOnly("spigot:v12104:12104:core")
    // Mojang API
    compileOnly("com.mojang:brigadier:1.0.18")
}

// 编译配置
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_HIGHER
    targetCompatibility = JavaVersion.VERSION_HIGHER
}

// 子模块
taboolib { subproject = true }