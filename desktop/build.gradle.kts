plugins {
    kotlin("jvm")
    application
}

val gdxVersion = "1.12.1"

dependencies {
    implementation(project(":common"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
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
