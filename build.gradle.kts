import edu.wpi.first.gradlerio.GradleRIOPlugin
import edu.wpi.first.gradlerio.frc.FRCJavaArtifact
import edu.wpi.first.gradlerio.frc.RoboRIO
import edu.wpi.first.toolchain.NativePlatforms
import io.gitlab.arturbosch.detekt.detekt

plugins {
    kotlin("jvm") version "1.3.11"
    id("edu.wpi.first.GradleRIO") version "2019.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC12"
}

val kMainRobotClass = "org.ghrobotics.frc2019.robot.Robot"

val roborioTargetName = "roborio"

deploy {
    targets {
        // Add the RoboRIO as a target
        target(roborioTargetName, RoboRIO::class.java, closureOf<RoboRIO> {
            team = 5190
        })
    }
    artifacts {
        // Send the JAR to the RoboRIO
        artifact("frcJava", FRCJavaArtifact::class.java, closureOf<FRCJavaArtifact> {
            targets.add(roborioTargetName)
        })

        // Deploy Vision files in src/main/jevois-vision to the JeVois Smart Camera
//        fileTreeArtifact("jevois", closureOf<FileTreeArtifact> {
//            targets.add(roborioTargetName)
//            setFiles(fileTree("src/main/jevois-vision"))
//            directory = "/home/lvuser/jevois-vision-temp"
//
//            postdeploy.add(closureOf<DeployContext> {
//                // Mount JeVois to /media/jevois
//                execute("echo Mounting JeVois...")
//                execute("echo usbsd > /dev/ttyACM0")
//                execute("sleep 5")
//                execute("mkdir -p /media/jevois")
//                execute("mount /dev/disk/by-id/usb-JeVois_Smart_Camera-0:0 /media/jevois")
//                execute("echo Mounting Complete")
//
//                // Copy Vision Files
//                execute("mv -rfv /home/lvuser/jevois-vision-temp/* /media/jevois")
//
//                // Unmount and Restart
//                execute("echo Unmounting Jevois...")
//                execute("umount /media/jevois")
//                execute("rmdir /media/jevois")
//                execute("eject /dev/disk/by-id/usb-JeVois_Smart_Camera-0:0")
//                execute("Unmounting Complete")
//            })
//        })
    }
}

detekt {
    config = files("$projectDir/detekt-config.yml")

    reports {
        html {
            enabled = true
            destination = file("$rootDir/detekt.html")
        }
    }
}

repositories {
    mavenLocal()
    jcenter()
    maven { setUrl("http://dl.bintray.com/kyonifer/maven") }
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    // Kotlin Standard Library and Coroutines
    compile(kotlin("stdlib"))
    compile("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.1.0")

    // FalconLibrary
    compile("org.ghrobotics", "FalconLibrary", "31bf852")

    // WPILib and Vendors
    wpi.deps.wpilib().forEach { compile(it) }
    wpi.deps.vendor.java().forEach { compile(it) }
    wpi.deps.vendor.jni(NativePlatforms.roborio).forEach { nativeZip(it) }
    wpi.deps.vendor.jni(NativePlatforms.desktop).forEach { nativeDesktopZip(it) }

    // Gson
    compile("com.github.salomonbrys.kotson", "kotson", "2.5.0")

    // XChart for Simulations and Tests
    compile("org.knowm.xchart", "xchart", "3.2.2")

    // Unit Testing
    testCompile("junit", "junit", "4.12")
}

tasks.jar {
    doFirst {
        from(configurations.compile.get().map {
            if (it.isDirectory) it else zipTree(it)
        })
        manifest(GradleRIOPlugin.javaManifest(kMainRobotClass))
    }
}

tasks.withType<Wrapper>().configureEach {
    gradleVersion = "5.0"
}
