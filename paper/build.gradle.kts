import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.GeneratePluginDescription
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.3"
    id("com.modrinth.minotaur") version "2.+"
    id("io.papermc.hangar-publish-plugin") version "0.+"
}

val javaVersion: String by project
val minecraftVersion: String by project
val pluginArtifactName: String by project
val supportedMinecraftVersions: String by project
val modrinthProjectId: String by project
val modrinthLoaders: String by project
val modrinthVersionType: String by project
val hangarProjectId: String by project
val hangarChannel: String by project

val targetJavaVersion = javaVersion.toInt()
val supportedMcVersions = supportedMinecraftVersions.split(',').map(String::trim).filter(String::isNotEmpty)
val modrinthLoaderList = modrinthLoaders.split(',').map(String::trim).filter(String::isNotEmpty)
val releaseChangelog = providers.environmentVariable("RELEASE_CHANGELOG")
    .orElse("Automated release for ${project.version}")

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://repo.extendedclip.com/releases/")
    }
}

dependencies {
    implementation(project(":mailbox-api"))
    paperLibrary("com.h2database:h2:2.4.240")
    paperLibrary("com.mysql:mysql-connector-j:9.7.0")
    compileOnly("org.spongepowered:configurate-yaml:4.3.0-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        isTransitive = false
    }
    implementation("com.zaxxer:HikariCP:7.1.0") {
        isTransitive = false
    }
    implementation("commons-dbutils:commons-dbutils:1.8.1")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    compileOnly("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.vdurmont:semver4j:3.1.0")
    compileOnly("me.clip:placeholderapi:2.12.2")
    paperweight.paperDevBundle(property("paperVersion") as String)
}

paper {
    name = pluginArtifactName
    main = "net.sabafly.mailBox.MailBox"
    apiVersion = "1.21.6"
    authors = listOf("ikafly144")
    website = "https://github.com/ikafly144/MailBox"
    bootstrapper = "net.sabafly.mailBox.Bootstrapper"
    loader = "net.sabafly.mailBox.Loader"
    generateLibrariesJson = true
    foliaSupported = true

    serverDependencies {
        register("Vault") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            joinClasspath = true
        }
    }

    permissions {
        register("mailBox.admin") {
            description = "Allows the user to use all MailBox commands"
            default = BukkitPluginDescription.Permission.Default.OP
            childrenMap = mapOf(
                "mailbox.template" to true,
                "mailbox.template.send" to true,
                "mailbox.template.create" to true,
                "mailbox.template.delete" to true,
                "mailbox.template.edit" to true,
                "mailbox.attachment.command" to true,
                "mailbox.attachment.message" to true,
                "mailbox.inbox.other" to true,
                "mailbox.mailto.namespace.*" to true
            )
        }
        register("mailBox.default") {
            description = "Allows the user to use MailBox"
            default = BukkitPluginDescription.Permission.Default.TRUE
            childrenMap = mapOf(
                "mailbox.inbox" to true,
                "mailbox.send" to true,
                "mailbox.attachment.item" to true,
                "mailbox.attachment.vault" to true,
                "mailbox.mailto.namespace.minecraft" to true
            )
        }
    }
}

tasks.named<GeneratePluginDescription>("generatePaperPluginDescription") {
    useDefaultCentralProxy()
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set(pluginArtifactName)
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    exclude("plugin.yml")

    minimize()
    relocate("com.zaxxer.hikari", "net.sabafly.libs.com.zaxxer.hikari")
    relocate("org.apache.commons.dbutils", "net.sabafly.libs.org.apache.commons.dbutils")
    relocate("com.vdurmont.semver4j", "net.sabafly.libs.com.vdurmont.semver4j")
}

tasks.named("build") {
    dependsOn(shadowJarTask)
}

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set(modrinthProjectId)
    versionNumber.set(project.version.toString())
    versionName.set("$pluginArtifactName ${project.version}")
    versionType.set(modrinthVersionType)
    uploadFile.set(shadowJarTask)
    gameVersions.addAll(supportedMcVersions)
    loaders.addAll(modrinthLoaderList)
    changelog.set(releaseChangelog)
}

tasks.named("modrinth") {
    dependsOn(shadowJarTask)
}

hangarPublish {
    publications.register("plugin") {
        version = project.version.toString()
        id = hangarProjectId
        channel = hangarChannel
        changelog = releaseChangelog.get()
        apiKey = providers.environmentVariable("HANGAR_API_TOKEN").orElse("").get()

        platforms {
            paper {
                jar = shadowJarTask.flatMap { it.archiveFile }
                platformVersions = supportedMcVersions
            }
        }
    }
}

tasks.named("publishPluginPublicationToHangar") {
    dependsOn(shadowJarTask)
}

runPaper.folia.registerTask {
    downloadPlugins {
        github("SirBlobman", "Vault-Folia", "v1.7.3-folia", "Vault-1.7.3.jar")
        modrinth("luckperms", "v5.5.17-bukkit")
        modrinth("placeholderapi", "2.12.3")
        modrinth("emeraldbank", "1.2.0")
    }
    minecraftVersion(minecraftVersion)
}

tasks.named<RunServer>("runServer") {
    downloadPlugins {
        github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
        modrinth("luckperms", "v5.5.17-bukkit")
        modrinth("placeholderapi", "2.12.3")
        modrinth("emeraldbank", "1.2.0")
    }
    minecraftVersion(minecraftVersion)
}
