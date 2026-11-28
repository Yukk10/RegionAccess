plugins {
    java
}

group = "dev.yukio"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    // Ядро: Paper API 26.3 (Leaf — форк Paper, API совместим)
    compileOnly("io.papermc.paper:paper-api:26.3.build.263-stable")

    // WorldGuard 7 (API стабилен между версиями 7.0.x)
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.21") {
        exclude(group = "org.bukkit")
        exclude(group = "org.spigotmc")
        exclude(group = "io.papermc.paper")
        exclude(group = "io.papermc")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("RegionAccess-${project.version}.jar")
}
