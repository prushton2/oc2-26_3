val modId = providers.gradleProperty("modId").get()
val minecraftVersion: String = libs.versions.minecraft.get()
val fabricLoaderVersion: String = libs.versions.fabric.loader.get()
val fabricApiVersion: String = libs.versions.fabric.api.get()
val architecturyVersion: String = libs.versions.architectury.get()
val forgeConfigPortVersion: String = libs.versions.fabric.forgeConfigPort.get()

val gameTestRuntime = configurations.create("gameTestRuntime")
val gameTestResultsDir = layout.buildDirectory.dir("test-results/gameTest")
val devOnlyMods = configurations.create("devOnlyMods")
val devOnlyModNames = provider { devOnlyMods.resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.name } }

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    runs {
        named("client") { runDirectory.set(file("run/client")) }
        named("server") { runDirectory.set(file("run/server")) }

        create("gameTest") {
            server()
            runDirectory.set(file("run/gametest"))
            systemProperties.put("fabric-api.gametest", "true")
            systemProperties.put("fabric-api.gametest.report-file", gameTestResultsDir.get().file("fabric-game-tests.xml").asFile.absolutePath)
            jvmArguments.add("-ea")
        }
    }
}

val gameTestBlobDirectories = listOf(
    file("run/gametest/world/oc2-blobs"),
    file("run/gametest/world/oc2-blobs-trash"),
)

val cleanGameTestResults = tasks.register<Delete>("cleanGameTestResults") {
    description = "Deletes game test results and the scratch world from previous runs."
    delete(gameTestResultsDir)
    delete(layout.projectDirectory.dir("run/gametest/world"))
}

val fixGameTestReport = tasks.register("fixGameTestReport") {
    val reportFile = gameTestResultsDir.map { it.file("fabric-game-tests.xml") }
    outputs.upToDateWhen { false }
    doLast {
        normalizeGameTestReport(reportFile.get().asFile)
    }
}

tasks.named<JavaExec>("runGameTest") {
    dependsOn(cleanGameTestResults)
    classpath += gameTestRuntime
    classpath = classpath.filter { file -> devOnlyModNames.get().none { file.name.startsWith("${it}-") } }
    finalizedBy(fixGameTestReport)
    doFirst {
        gameTestBlobDirectories.forEach { it.deleteRecursively() }
    }
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net/") }
        filter { includeGroup("teamreborn") }
    }
    exclusiveContent {
        forRepository { maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") }
        filter { includeGroup("fuzs.forgeconfigapiport") }
    }
}

configurations.named("runtimeOnly") { extendsFrom(devOnlyMods) }

dependencies {
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
    api(libs.fabric.architectury)

    compileOnly(libs.architectury.injectables)

    include(api(libs.fabric.energy.get().toString()) {
        exclude(group = "net.fabricmc.fabric-api")
    })

    implementation(libs.fabric.forgeConfigPort)

    bundledLibs().forEach {
        "shadowBundle"(it)
        implementation(it)
    }

    compileOnly(libs.cc.fabric.api)

    // Not used by mod, just for dev convenience.
    devOnlyMods(libs.jei.fabric)

    if (findProject(":gametest-fabric") != null) {
        runtimeOnly(project(path = ":instrumentation-fabric")) { isTransitive = false }
        "common"(project(path = ":instrumentation-common")) { isTransitive = false }
        gameTestRuntime(project(path = ":gametest-fabric")) { isTransitive = false }
    }
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "minecraftVersion" to minecraftVersion,
            "loaderVersion" to fabricLoaderVersion,
            "fabricApiVersion" to fabricApiVersion,
            "architecturyVersion" to architecturyVersion,
            "forgeConfigPortVersion" to forgeConfigPortVersion
        )
        inputs.properties(properties)
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }
}
