plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.deniskokarev"
            artifactId = "reversy"
            version = "1.0.0"
            from(components["java"])
        }
    }
}
// --- JNI native compilation (appended by solution) ---

tasks.register<Exec>("compileNative") {
    val nativeDir = layout.buildDirectory.dir("native").get().asFile
    val osName = System.getProperty("os.name").lowercase()
    val osPlatform = when {
        osName.contains("mac") || osName.contains("darwin") -> "darwin"
        else -> "linux"
    }
    val libExt = if (osPlatform == "darwin") "dylib" else "so"
    doFirst { nativeDir.mkdirs() }
    commandLine("gcc", "-shared", "-fPIC", "-O2",
        "-I", "${System.getenv("JAVA_HOME")}/include",
        "-I", "${System.getenv("JAVA_HOME")}/include/${osPlatform}",
        "-I", "${projectDir}/c",
        "-o", "${nativeDir}/libreversyjni.${libExt}",
        "${projectDir}/src/main/cpp/reversy_jni.c",
        "${projectDir}/c/game.c",
        "${projectDir}/c/minimax.c")
}

tasks.compileKotlin { dependsOn("compileNative") }

tasks.test {
    dependsOn("compileNative")
    jvmArgs("-Djava.library.path=${layout.buildDirectory.dir("native").get().asFile.absolutePath}")
}

tasks.processResources {
    dependsOn("compileNative")
    from(layout.buildDirectory.dir("native")) {
        include("*.so", "*.dylib")
    }
}
