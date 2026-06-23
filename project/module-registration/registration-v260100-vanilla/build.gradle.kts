dependencies {
    compileOnly(project(":project:module-registration:registration-v260100-paper"))
    compileOnly("paper:v260101:260101:core")
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