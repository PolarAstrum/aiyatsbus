dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly(project(":project:module-compat:compat-libreforge"))
    compileOnly("paper:v12101:12101:core")
    compileOnly(project(":project:common"))
}

// 子模块
taboolib { subproject = true }