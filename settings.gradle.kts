pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://maven.architectury.dev") }
            filter {
                includeGroup("architectury-plugin")
                includeGroupByRegex("dev\\.architectury.*")
                includeGroup("com.mojang")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.fabricmc.net") }
            filter {
                includeGroupByRegex("net\\.fabricmc.*")
                includeGroup("fabric-loom")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.minecraftforge.net") }
            filter {
                includeGroupByRegex("net\\.minecraftforge.*")
                includeGroup("de.oceanlabs.mcp")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.neoforged.net/releases") }
            filter { includeGroupByRegex("net\\.neoforged.*") }
        }
        gradlePluginPortal()
    }
}

fun substituteLocal(propertyName: String, libraryName: String) {
    val configured = providers.gradleProperty(propertyName).orNull?.takeIf { it.isNotBlank() } ?: return
    val path = rootDir.resolve(configured)
    require(path.isDirectory) { "[$propertyName] is set to [$configured], which is not a directory." }
    println("Substituting [$libraryName] from local [${path.canonicalPath}]")
    includeBuild(path) {
        dependencySubstitution {
            substitute(module(libraryName)).using(project(":"))
        }
    }
}

substituteLocal("buildrootDir", "li.cil.sedna:sedna-buildroot")
substituteLocal("ceresDir", "li.cil.ceres:ceres")
substituteLocal("openos2Dir", "li.cil.oc2:oc-openos2")
substituteLocal("sednaDir", "li.cil.sedna:sedna")
substituteLocal("sednaCpmDir", "li.cil.sedna:sedna-cpm")
substituteLocal("vox2mcDir", "li.cil.vox2mc:vox2mc")

if (JavaVersion.current() < JavaVersion.VERSION_25) {
    throw GradleException("Minecraft 26.x requires Gradle to run on Java 25 or newer (found ${JavaVersion.current()}).")
}

include("common")

val enabledPlatforms = providers.gradleProperty("enabledPlatforms").get()
for (enabledPlatform in enabledPlatforms.split(",")) {
    include(enabledPlatform)
}

// Dev-only modules (game tests and test instrumentation). Not required to build the mod itself.
val devModules = providers.gradleProperty("devModules").map { it.toBoolean() }.getOrElse(false)
if (devModules) {
    for (module in listOf("common") + enabledPlatforms.split(",")) {
        include("instrumentation-$module")
        project(":instrumentation-$module").projectDir = file("tools/instrumentation/$module")

        include("gametest-$module")
        project(":gametest-$module").projectDir = file("gametest/$module")
    }
}

val modId = providers.gradleProperty("modId").get()
rootProject.name = modId
