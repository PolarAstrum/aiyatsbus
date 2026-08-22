dependencies {
    // 引入 API
    compileOnly(project(":project:common"))
    compileOnly(project(":project:module-script:script-fluxon"))
//    compileOnly(project(":project:module-script:script-kether"))
}

// 子模块
taboolib { subproject = true }
