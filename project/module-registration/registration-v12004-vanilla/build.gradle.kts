dependencies {
    compileOnly(project(":project:module-registration:registration-v12004-paper"))
    compileOnly(project(":project:module-compat:compat-libreforge"))
    compileOnly("ink.ptms.core:v12004:12004:mapped")
}

// 子模块
taboolib { subproject = true }