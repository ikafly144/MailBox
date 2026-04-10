import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "net.sabafly"
version = "1.1.0-alpha.4"

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
    compileOnly("com.h2database:h2:2.4.240")
    compileOnly("com.mysql:mysql-connector-j:9.6.0")
    compileOnly("org.spongepowered:configurate-yaml:4.3.0-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        isTransitive = false
    }
    implementation("com.zaxxer:HikariCP:7.0.2") {
        isTransitive = false
    }
    implementation("commons-dbutils:commons-dbutils:1.8.1")
    compileOnly("org.projectlombok:lombok:1.18.44")
    annotationProcessor("org.projectlombok:lombok:1.18.44")
    compileOnly("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.vdurmont:semver4j:3.1.0")
    compileOnly("me.clip:placeholderapi:2.12.2")
    paperweight.paperDevBundle(property("paperVersion") as String)
}

val targetJavaVersion = 25
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

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("MailBox")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    exclude("plugin.yml")

    minimize()
    relocate("com.zaxxer.hikari", "net.sabafly.libs.com.zaxxer.hikari")
    relocate("org.apache.commons.dbutils", "net.sabafly.libs.org.apache.commons.dbutils")
    relocate("com.vdurmont.semver4j", "net.sabafly.libs.com.vdurmont.semver4j")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<RunServer>("runServer") {
    downloadPlugins {
        github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
        modrinth("luckperms", "v5.5.17-bukkit")
        modrinth("placeholderapi", "2.12.2")
        modrinth("emeraldbank", "1.1.2")
    }
    minecraftVersion("26.1.1")
}
