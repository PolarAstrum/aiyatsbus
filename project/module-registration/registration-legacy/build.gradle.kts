dependencies {
    compileOnly(project(":project:common"))
    compileOnly("ink.ptms.core:v12002:12002:mapped")
}

// 子模块
taboolib { subproject = true }