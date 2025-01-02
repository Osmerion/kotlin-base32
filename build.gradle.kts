/*
 * Copyright 2024-2025 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import groovy.util.Node
import groovy.util.NodeList
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.yarn.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(buildDeps.plugins.binary.compatibility.validator)
    alias(buildDeps.plugins.dokka)
//    alias(buildDeps.plugins.dokka.javadoc)
    alias(buildDeps.plugins.kotlin.multiplatform)
    id("com.osmerion.maven-publish-conventions")
}

yarn.lockFileName = "kotlin-yarn.lock"
yarn.lockFileDirectory = rootDir

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

kotlin {
    explicitApi()
    applyDefaultHierarchyTemplate()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1
    }

    js {
        browser()
        nodejs()
    }

    jvm {
        withJava()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            freeCompilerArgs.add("-Xjdk-release=11")
        }
    }

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()

    linuxArm64()
    linuxX64()

    iosArm64()
    iosX64()

    iosSimulatorArm64()

    macosArm64()
    macosX64()

    mingwX64()

    tvosArm64()
    tvosX64()

    tvosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    watchosArm32()
    watchosArm64()
    watchosX64()

    watchosDeviceArm64()
    watchosSimulatorArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val nativeAndWasmMain by registering {
            dependsOn(commonMain.get())
        }

        nativeMain {
            dependsOn(nativeAndWasmMain.get())
        }

        wasmJsMain {
            dependsOn(nativeAndWasmMain.get())
        }

        wasmWasiMain {
            dependsOn(nativeAndWasmMain.get())
        }
    }

    targets.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*, *> }.forEach { target ->
        tasks.named<Jar>(target.artifactsTaskName) {
            manifest {
                attributes(mapOf(
                    "Name" to project.name,
                    "Specification-Version" to project.version,
                    "Specification-Vendor" to "Leon Linhart <themrmilchmann@gmail.com>",
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "Leon Linhart <themrmilchmann@gmail.com>"
                ))
            }
        }
    }
}

dokka {
    dokkaGeneratorIsolation = ProcessIsolation {
        maxHeapSize = "4G"
    }

    dokkaSourceSets.configureEach {
        reportUndocumented = true
        skipEmptyPackages = true
        jdkVersion = 11

        val localKotlinSourceDir = layout.projectDirectory.dir("src/$name/kotlin")
        val version = project.version

        sourceLink {
            localDirectory = localKotlinSourceDir

            remoteUrl("https://github.com/Osmerion/kotlin-base32/tree/v${version}/src/main/kotlin")
            remoteLineSuffix = "#L"
        }
    }

    dokkaSourceSets {
        commonMain {
            samples.from(files("src/commonTest/kotlin"))
        }
    }

    dokkaPublications.configureEach {
        moduleName = "kotlin-base32"

        // TODO Remaining warnings are silly atm. Reevaluate this flag in the future.
        failOnWarning = true
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.javaModuleVersion = "$version"
        options.release = 11
    }

    named<JavaCompile>("compileJava") {
        options.compilerArgumentProviders += object : CommandLineArgumentProvider {

            @InputFiles
            @PathSensitive(PathSensitivity.RELATIVE)
            val kotlinClasses = this@tasks.named<KotlinCompile>("compileKotlinJvm").flatMap(KotlinCompile::destinationDirectory)

            override fun asArguments() = listOf(
                "--patch-module",
                "com.osmerion.kotlin.base32=${kotlinClasses.get().asFile.absolutePath}"
            )

        }
    }

    withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true

        includeEmptyDirs = false
    }

    // See https://github.com/GW2ToolBelt/GW2ChatLinks/issues/18
    generatePomFileForKotlinMultiplatformPublication {
        dependsOn(project.tasks.named("generatePomFileForJvmPublication"))
    }
}

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            val emptyJavadocJar = tasks.register<Jar>("${name}JavadocJar") {
                archiveBaseName = "${archiveBaseName.get()}-${name}"
                archiveClassifier = "javadoc"
            }

            artifact(emptyJavadocJar)
        }

        // See https://github.com/GW2ToolBelt/GW2ChatLinks/issues/18
        named<MavenPublication>("kotlinMultiplatform") {
            val jvmPublication = publications.getByName<MavenPublication>("jvm")

            lateinit var jvmXml: XmlProvider
            jvmPublication.pom?.withXml { jvmXml = this }

            pom.withXml {
                val xmlProvider = this
                val root = xmlProvider.asNode()
                // Remove the original content and add the content from the platform POM:
                root.children().toList().forEach { root.remove(it as Node) }
                jvmXml.asNode().children().forEach { root.append(it as Node) }

                // Adjust the self artifact ID, as it should match the root module's coordinates:
                ((root.get("artifactId") as NodeList).first() as Node).setValue(artifactId)

                // Set packaging to POM to indicate that there's no artifact:
                root.appendNode("packaging", "pom")

                // Remove the original platform dependencies and add a single dependency on the platform
                // module:
                val dependencies = (root.get("dependencies") as NodeList).first() as Node
                dependencies.children().toList().forEach { dependencies.remove(it as Node) }
                val singleDependency = dependencies.appendNode("dependency")
                singleDependency.appendNode("groupId", jvmPublication.groupId)
                singleDependency.appendNode("artifactId", jvmPublication.artifactId)
                singleDependency.appendNode("version", jvmPublication.version)
                singleDependency.appendNode("scope", "compile")
            }
        }
    }
}