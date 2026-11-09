val modId = providers.gradleProperty("modId").get()
val minecraftVersion: String = libs.versions.minecraft.get()
val neoforgeVersion: String = libs.versions.neoforge.platform.get()
val neoforgeLoaderVersion: String = libs.versions.neoforge.loader.get()
val architecturyVersion: String = libs.versions.architectury.get()

val gameTestRuntime = configurations.create("gameTestRuntime")
val gameTestResultsDir = layout.buildDirectory.dir("test-results/gameTest")
val devOnlyMods = configurations.create("devOnlyMods")
val devOnlyModNames = provider { devOnlyMods.resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.name } }

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    neoForge {
        convertAccessWideners(tasks.named<Jar>("shadowJar"), "${modId}.accesswidener")
    }

    runs {
        named("client") { runDirectory.set(file("run/client")) }
        named("server") { runDirectory.set(file("run/server")) }

        create("gameTestServer") {
            server()
            runDirectory.set(file("run/gametest"))
            systemProperties.put("neoforge.gameTestServer", "true")
            systemProperties.put("neoforge.enabledGameTestNamespaces", "oc2_gametest")
            systemProperties.put("oc2.gameTest.junitDir", gameTestResultsDir.get().asFile.absolutePath)
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
    val reportFile = gameTestResultsDir.map { it.file("neoforge-game-tests.xml") }
    outputs.upToDateWhen { false }
    doLast {
        normalizeGameTestReport(reportFile.get().asFile)
    }
}

tasks.named<JavaExec>("runGameTestServer") {
    dependsOn(cleanGameTestResults)
    classpath += gameTestRuntime
    classpath = classpath.filter { file -> devOnlyModNames.get().none { file.name.startsWith("${it}-") } }
    finalizedBy(fixGameTestReport)
    doFirst {
        gameTestBlobDirectories.forEach { it.deleteRecursively() }
    }
}

repositories {
    maven("https://maven.neoforged.net/releases")
}

configurations.named("runtimeOnly") { extendsFrom(devOnlyMods) }

dependencies {
    neoForge(libs.neoforge.platform)
    implementation(libs.neoforge.architectury)

    compileOnly(libs.architectury.injectables)

    compileOnly(libs.cc.neoforge.api)

    // Not used by mod, just for dev convenience.
    devOnlyMods(libs.jei.neoforge)

    if (findProject(":gametest-neoforge") != null) {
        gameTestRuntime(project(":gametest-neoforge"))
        runtimeOnly(project(":instrumentation-neoforge"))
    }

    bundledLibs().forEach {
        "shadowBundle"(it)
        forgeRuntimeLibrary(it)
    }
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "minecraftVersion" to minecraftVersion,
            "loaderVersion" to neoforgeLoaderVersion,
            "neoforgeVersion" to neoforgeVersion,
            "architecturyVersion" to architecturyVersion
        )
        inputs.properties(properties)
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(properties)
        }
    }
}
