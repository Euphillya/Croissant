import java.util.Locale

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.canvasmc.io/public")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "tenseimc"
for (name in listOf("tensei-api", "tensei-server")) {
    val projName = name.lowercase(Locale.ENGLISH)
    include(projName)
    findProject(":$projName")!!.projectDir = file(name)
}

gradle.lifecycle.beforeProject {
    val mcVersion = providers.gradleProperty("mcVersion").get().trim()
    val tenseiChannel = providers.gradleProperty("channel").get().trim()
    val tenseiBuildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.trim()?.toInt()
    val versionString = if (tenseiBuildNumber == null) {
        "$mcVersion.local-SNAPSHOT"
    } else {
        "$mcVersion.build.$tenseiBuildNumber-${tenseiChannel.lowercase()}"
    }
    version = versionString
}