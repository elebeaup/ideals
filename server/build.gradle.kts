import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
  id("java")
  // id("org.jetbrains.kotlin.jvm") version "1.7.0"
//  id("org.jetbrains.intellij.platform") version "2.2.1"
  id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "org.rri.ideals.server"
version = System.getenv("IDEALS_VERSION") ?: "1.0-SNAPSHOT"

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
    marketplace()
  }
}

dependencies {
  implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.17.0")
  implementation("io.github.furstenheim:copy_down:1.1")
  implementation("com.google.code.gson:gson:2.10.1")
  
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.mockito:mockito-core:5.3.1")
  testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")

  intellijPlatform {
    intellijIdeaCommunity("2024.3")
    testFramework(TestFrameworkType.Platform)

    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.kotlin")
    plugin("PythonCore:243.21565.193")
  }
}

tasks.register<RunIdeTask>("plainIdea") {
  maxHeapSize = "4G"
  jvmArgs = listOf(
    "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
    "--add-exports=java.desktop/sun.awt.windows=ALL-UNNAMED",
    "--add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED",
  )
}

tasks {
  test {
    jvmArgs = listOf(
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens=java.base/java.io=ALL-UNNAMED",
      "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
      "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
      "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
      "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
      "--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED",
      "--add-opens=java.desktop/sun.font=ALL-UNNAMED",
      "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
      "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
      "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
      "--add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED",
      "--add-exports=java.desktop/sun.awt.windows=ALL-UNNAMED",
      "--add-exports=java.desktop/sun.java2d=ALL-UNNAMED",
      "--add-exports=java.desktop/sun.font=ALL-UNNAMED",
      "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED",
      "--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED",
      "-Djdk.module.illegalAccess.silent=true"
    )

    useJUnitPlatform {
      includeEngines("junit-jupiter", "junit-vintage")
    }
  }

  getByName<RunIdeTask>("runIde") {
    maxHeapSize = "4G"
    args = listOf("lsp-server", "tcp", "8989")
    systemProperty("java.awt.headless", true)
  }

  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
  }

  patchPluginXml {
    version = System.getenv("IDEALS_VERSION")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}