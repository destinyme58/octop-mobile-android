import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
}

val appVersionName = "1.0.1"
val buildDate = providers.gradleProperty("buildDate").orElse(
    providers.provider<String> {
        LocalDate.now(ZoneId.of("Asia/Shanghai"))
            .format(DateTimeFormatter.BASIC_ISO_DATE)
    },
)

android {
    namespace = "com.octop.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.octop.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://192.168.32.65:8088\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

fun artifactName(buildType: String): String =
    "Octop-Mobile_Android_universal_v${appVersionName}_${buildDate.get()}_${buildType}.apk"

val exportDebugApk by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Exports the versioned universal Android debug APK to artifacts/."
    dependsOn("packageDebug")
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("artifacts"))
    rename { artifactName("debug") }
}

val exportReleaseApk by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Exports the versioned universal Android release APK to artifacts/."
    dependsOn("packageRelease")
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
    }
    into(rootProject.layout.projectDirectory.dir("artifacts"))
    rename { artifactName("release") }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(exportDebugApk)
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(exportReleaseApk)
}

dependencies {
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
}

