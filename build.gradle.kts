plugins {
    java
}

val projectGroup: String by project
val pluginVersion: String by project

subprojects {
    group = projectGroup
    version = pluginVersion
}

val publishPluginRelease by tasks.registering {
    group = "publishing"
    description = "Builds and publishes plugin artifacts to Modrinth and Hangar."
}

subprojects {
    plugins.withId("com.modrinth.minotaur") {
        rootProject.tasks.named("publishPluginRelease") {
            dependsOn(tasks.named("build"))
            dependsOn(tasks.named("modrinth"))
        }
    }

    plugins.withId("io.papermc.hangar-publish-plugin") {
        rootProject.tasks.named("publishPluginRelease") {
            dependsOn(tasks.named("publishPluginPublicationToHangar"))
        }
    }
}
