plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.246-stable")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") {
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
        exclude(group = "it.unimi.dsi")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10") {
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "com.google.guava")
        exclude(group = "com.google.code.gson")
        exclude(group = "it.unimi.dsi")
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }

    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("com.mysql:mysql-connector-j:9.2.0")

    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.papermc.paper:paper-api:26.3.build.246-stable")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

configurations.named("testImplementation") {
    extendsFrom(configurations.compileOnly.get())
}

val integration = sourceSets.create("integration") {
    compileClasspath += sourceSets.main.get().compileClasspath
}
tasks.register<Jar>("integrationProbeJar") {
    dependsOn(tasks.named(integration.classesTaskName))
    from(integration.output)
    archiveFileName.set("riftplots-probe.jar")
    destinationDirectory.set(layout.buildDirectory.dir("integration"))
}
tasks.register<JavaExec>("benchmarkPlots") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("pl.riftplots.SpatialBenchmark")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "pl.riftplots.libs.hikari")
        // SQLite JNI resolves org.sqlite.core.NativeDB by its original name.
        relocate("com.mysql", "pl.riftplots.libs.mysql")
        mergeServiceFiles()
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    test {
        useJUnitPlatform()
        dependsOn(shadowJar)
        systemProperty("riftplots.shadedJar", shadowJar.get().archiveFile.get().asFile.absolutePath)
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
