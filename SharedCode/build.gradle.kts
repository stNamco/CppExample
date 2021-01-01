import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id( "com.android.library")
    kotlin("multiplatform")
    kotlin("native.cocoapods")
}

android {
    compileSdkVersion(29)
    defaultConfig {
        minSdkVersion(19)
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")

    commonTestImplementation("junit:junit:4.13")
    commonTestImplementation("org.jetbrains.kotlin:kotlin-test")
    commonTestImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

// CocoaPods requires the podspec to have a version.
version = "1.0"

kotlin {

    val ktorVersion = "1.4.0"
    val coroutinesVersion = "1.3.9-native-mt"
    val serializationVersion = "1.0.0-RC"

    android()

    iosArm64()
    iosX64()

    cocoapods {
        ios.deploymentTarget = "13.5"
        summary = "CocoaPods test library"
        homepage = "https://github.com/JetBrains/kotlin"

        pod("AFNetworking") {
            version = "~> 4.0.1"
        }
    }

    val commonMain by sourceSets.getting {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            implementation("io.ktor:ktor-client-core:$ktorVersion")
            implementation("io.ktor:ktor-client-serialization:$ktorVersion")
        }
    }

    val androidMain by sourceSets.getting {
        dependencies {
            implementation("io.ktor:ktor-client-android:$ktorVersion")
        }
    }

    val iosX64Main by sourceSets.getting {
        dependencies {
            implementation("io.ktor:ktor-client-ios:$ktorVersion")
        }
    }
    val iosArm64Main by sourceSets.getting {
        dependencies {
            implementation("io.ktor:ktor-client-ios:$ktorVersion")
        }
    }
}

val packForXcode by tasks.creating(Sync::class) {
    val targetDir = File(buildDir, "xcode-frameworks")

    /// selecting the right configuration for the iOS
    /// framework depending on the environment
    /// variables set by Xcode build
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    val framework = kotlin.targets
        .getByName<KotlinNativeTarget>(targetName)
        .binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)

    from({ framework.outputDirectory })
    into(targetDir)

    /// generate a helpful ./gradlew wrapper with embedded Java path
    doLast {
        val gradlew = File(targetDir, "gradlew")
        gradlew.writeText("#!/bin/bash\n"
                + "export 'JAVA_HOME=${System.getProperty("java.home")}'\n"
                + "cd '${rootProject.rootDir}'\n"
                + "./gradlew \$@\n")
        gradlew.setExecutable(true)
    }
}

tasks.getByName("build").dependsOn(packForXcode)

tasks.register("dumpGeneratedSharedCodePath")  {
    val targetDir = File(buildDir, "bin")
    doLast {
        println("$targetDir")
    }
}.dependsOn("build")

tasks.register("iosSimulatorTest")  {
    val device = project.findProperty("iosDevice") as? String ?: "iPhone 8"
    dependsOn("iosX64Test")
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Runs tests for target 'ios' on an iOS simulator"

    doLast {
        val  binary = (kotlin.targets["iosX64"] as KotlinNativeTarget).binaries.getTest("DEBUG").outputFile
        exec {
            commandLine("xcrun", "simctl", "spawn", "--standalone", device, binary.absolutePath)
        }
    }
}