plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

val gdxVersion = "1.12.1"

dependencies {
    implementation(project(":common"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.strategy.desktop.DesktopLauncherKt")
}

tasks.withType<JavaExec> {
    jvmArgs("-XstartOnFirstThread")
}
