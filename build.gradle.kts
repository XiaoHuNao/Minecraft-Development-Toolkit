plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.xiaohunao"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("org.intellij.plugins.markdown")
    }

    implementation(project(":mdt-protocol"))
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

val buildWeb by tasks.registering(Exec::class) {
    workingDir = file("web")
    if (System.getProperty("os.name").lowercase().contains("win")) {
        commandLine("cmd", "/c", "npm", "run", "build")
    } else {
        commandLine("npm", "run", "build")
    }
    inputs.dir("web/src")
    inputs.file("web/package.json")
    inputs.file("web/vite.config.ts")
    outputs.dir("web/dist")
}

val copyWebDist by tasks.registering(Copy::class) {
    dependsOn(buildWeb)
    from("web/dist")
    into(layout.buildDirectory.dir("resources/main/web"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    // Disable buildSearchableOptions — fails on non-English locale systems (IntelliJ bug),
    // and is only needed for plugin marketplace search indexing, not for development.
    withType<org.jetbrains.intellij.platform.gradle.tasks.BuildSearchableOptionsTask> {
        enabled = false
    }

    withType<org.jetbrains.intellij.platform.gradle.tasks.PrepareJarSearchableOptionsTask> {
        enabled = false
    }

    named("processResources") {
        dependsOn(copyWebDist)
    }

    named<org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask>("runIde") {
        val devMode = providers.gradleProperty("mdt.dev").isPresent
        jvmArgumentProviders.add(CommandLineArgumentProvider {
            if (devMode) listOf("-Dmdt.dev=true") else emptyList()
        })
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
