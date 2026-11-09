import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    java
    alias(libs.plugins.architectury)
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.spotless)
}

val modId = providers.gradleProperty("modId").get()
val modVersion = providers.gradleProperty("modVersion").get()
val mavenGroup = providers.gradleProperty("mavenGroup").get()
val enabledPlatforms = providers.gradleProperty("enabledPlatforms").get()
val devModulesEnabled = providers.gradleProperty("devModules").map { it.toBoolean() }.getOrElse(false)
val minecraftVersion: String = libs.versions.minecraft.get()

subprojects {
    apply(plugin = "java")
    apply(plugin = "pmd")
    apply(plugin = rootProject.libs.plugins.architectury.get().pluginId)
    apply(plugin = rootProject.libs.plugins.loom.get().pluginId)

    version = "${modVersion}+${gitRef()}"
    group = mavenGroup
    base.archivesName.set("${modId}-MC${minecraftVersion}-${project.name}")

    architectury {
        minecraft = minecraftVersion
    }

    repositories {
        exclusiveContent {
            forRepository { maven("https://maven.blamejared.com") }
            filter {
                includeGroup("mezz.jei")
                includeGroup("net.mezzdev.config")
            }
        }
        exclusiveContent {
            forRepository { maven("https://fnuecke.github.io/maven") }
            filter {
                includeModule("li.cil.oc2", "oc-openos2")
                includeModule("li.cil.sedna", "sedna-buildroot")
                includeModule("li.cil.sedna", "sedna-cpm")
                includeModule("li.cil.vox2mc", "vox2mc")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.squiddev.cc") }
            filter { includeGroup("cc.tweaked") }
        }
        mavenCentral()
    }

    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        "compileOnly"("com.google.code.findbugs:jsr305:3.0.2")
    }

    configureJava()
    configurePmd("**/jcodec/**")
    embedLicenses("LICENSE-JCODEC")
    configureIdeaExcludes()
}

val projectConfigurations = mapOf(
    "fabric" to "Fabric",
    "neoforge" to "NeoForge"
)

for (platform in enabledPlatforms.split(',')) {
    project(":$platform") {
        apply(plugin = rootProject.libs.plugins.shadow.get().pluginId)

        architectury {
            platformSetupLoomIde()
            loader(platform)
        }

        configure<LoomGradleExtensionAPI> {
            runs {
                named("client") { programArguments.addAll(devPlayerArgs()) }
            }
        }

        val common = configurations.create("common")
        val shadowBundle = configurations.create("shadowBundle")

        configurations {
            common.isCanBeResolved = true
            common.isCanBeConsumed = false

            compileClasspath.get().extendsFrom(common)
            runtimeClasspath.get().extendsFrom(common)
            getByName("development${projectConfigurations[platform]}").extendsFrom(common)

            shadowBundle.isCanBeResolved = true
            shadowBundle.isCanBeConsumed = false
            shadowBundle.isTransitive = false
        }

        dependencies {
            common(project(path = ":common")) { isTransitive = false }
            shadowBundle(
                project(
                    path = ":common",
                    configuration = "transformProduction${projectConfigurations[platform]}"
                )
            ) { isTransitive = false }
        }

        tasks {
            val jarTask = named<Jar>("jar")
            val shadowJarTask = named<ShadowJar>("shadowJar") {
                exclude("architectury.common.json")
                configurations = listOf(shadowBundle)
            }

            if (platform == "fabric") {
                // Loom adds jar-in-jar dependencies (`include`) to the `jar` task, including the entries in
                // fabric.mod.json, so here that task produces the release jar, with the shadowed jar (platform
                // code plus transformed common code and bundled libraries) merged into it.
                shadowJarTask.configure { archiveClassifier.set("dev-shadow") }
                jarTask.configure {
                    dependsOn(shadowJarTask)
                    from(zipTree(shadowJarTask.flatMap { it.archiveFile })) {
                        exclude("META-INF/MANIFEST.MF")
                    }
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
                extra["releaseJarTask"] = "jar"
            } else {
                jarTask.configure { archiveClassifier.set("dev") }
                shadowJarTask.configure {
                    dependsOn(jarTask)
                    archiveClassifier.set(null as String?)
                }
                named("assemble") { dependsOn(shadowJarTask) }
                extra["releaseJarTask"] = "shadowJar"
            }
        }

        (components["java"] as AdhocComponentWithVariants)
            .withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
                skip()
            }
    }
}

if (devModulesEnabled) {
    for (extraModule in listOf("instrumentation", "gametest")) {
        for (platform in enabledPlatforms.split(',')) {
            project(":$extraModule-$platform") {
                architectury {
                    platformSetupLoomIde()
                    loader(platform)
                }

                val common = configurations.create("common")
                val bundle = configurations.create("bundle")

                configurations {
                    common.isCanBeResolved = true
                    common.isCanBeConsumed = false

                    compileClasspath.get().extendsFrom(common)
                    runtimeClasspath.get().extendsFrom(common)
                    getByName("development${projectConfigurations[platform]}").extendsFrom(common)

                    bundle.isCanBeResolved = true
                    bundle.isCanBeConsumed = false
                }

                dependencies {
                    common(project(path = ":$extraModule-common")) { isTransitive = false }
                    bundle(
                        project(
                            path = ":$extraModule-common",
                            configuration = "transformProduction${projectConfigurations[platform]}"
                        )
                    ) { isTransitive = false }
                }

                tasks.jar {
                    val bundleFiles = configurations["bundle"]
                    dependsOn(bundleFiles)
                    from(bundleFiles.elements.map { files -> files.map { zipTree(it) } }) {
                        exclude("architectury.common.json", "META-INF/MANIFEST.MF")
                    }
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }
        }
    }
}

tasks.named("build") {
    dependsOn("apiJar", "apiSourcesJar")
}

spotless {
    java {
        target("**/src/*/java/li/cil/**/*.java")
        targetExclude("**/src/*/java/li/cil/oc2/jcodec/**/*.java")

        licenseHeader("/* SPDX-License-Identifier: MIT */\n\n")

        endWithNewline()
        trimTrailingWhitespace()
        removeUnusedImports()
        indentWithSpaces()
        importOrder("", "javax|java", "\\#")
    }

    format("packageInfo") {
        target("**/src/*/java/li/cil/**/package-info.java")
        targetExclude("**/src/*/java/li/cil/oc2/jcodec/**/package-info.java")

        licenseHeader("/* SPDX-License-Identifier: MIT */\n\n", "(/\\*\\*|//|@|package )")
    }
}

serializeArchitecturyTransforms()
if (devModulesEnabled) {
    registerGameTestTask()
}
registerPackageInfoTask("**/src/*/java/li/cil/oc2/jcodec/**")
registerLintTask()
registerApiJarTask(minecraftVersion)
configureMavenPublishing(minecraftVersion, "https://github.com/fnuecke/oc2")
