plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val generatedWebAssetsDir = layout.buildDirectory.dir("generated/tfWebAssets").get().asFile
val generatedResDir = layout.buildDirectory.dir("generated/tfRes").get().asFile

val prepareWebAssets = tasks.register("prepareWebAssets") {
    val rootIndex = rootProject.file("index.html")
    val bridgeScript = project.file("src/main/web/native-bridge.js")
    val fontsDir = rootProject.file("fonts")
    val musicDir = rootProject.file("music")
    val outAssets = generatedWebAssetsDir
    val outRes = generatedResDir

    inputs.file(rootIndex)
    inputs.file(bridgeScript)
    if (fontsDir.exists()) inputs.dir(fontsDir)
    if (musicDir.exists()) inputs.dir(musicDir)
    outputs.dirs(outAssets, outRes)

    doLast {
        val www = File(outAssets, "www")
        www.deleteRecursively()
        www.mkdirs()

        // Inject native-bridge.js right after <head> without touching repo root index.html
        val html = rootIndex.readText(Charsets.UTF_8)
        val headIdx = html.indexOf("<head>")
        val patched = if (headIdx >= 0) {
            html.substring(0, headIdx + 6) +
                "\n<script src=\"native-bridge.js\"></script>" +
                html.substring(headIdx + 6)
        } else {
            "<script src=\"native-bridge.js\"></script>\n$html"
        }
        File(www, "index.html").writeText(patched, Charsets.UTF_8)

        bridgeScript.copyTo(File(www, "native-bridge.js"), overwrite = true)

        listOf(
            "manifest.webmanifest",
            "apple-touch-icon.png",
            "icon-192.png",
            "icon-512.png",
            "icon-maskable-512.png"
        ).forEach { name ->
            val f = rootProject.file(name)
            if (f.exists()) f.copyTo(File(www, name), overwrite = true)
        }

        if (fontsDir.exists()) {
            fontsDir.copyRecursively(File(www, "fonts"), overwrite = true)
        }
        if (musicDir.exists()) {
            musicDir.copyRecursively(File(www, "music"), overwrite = true)
        }

        // Copy brand launcher icons into generated resources
        val drawableNodpi = File(outRes, "drawable-nodpi")
        drawableNodpi.mkdirs()
        val fgIcon = rootProject.file("android-app/assets/icon-foreground.png")
        val legacyIcon = rootProject.file("android-app/assets/icon-only.png")
        val fallbackIcon = rootProject.file("icon-512.png")
        val chosenFg = if (fgIcon.exists()) fgIcon else fallbackIcon
        val chosenLegacy = if (legacyIcon.exists()) legacyIcon else fallbackIcon
        if (chosenFg.exists()) {
            chosenFg.copyTo(File(drawableNodpi, "ic_launcher_fg.png"), overwrite = true)
        }
        if (chosenLegacy.exists()) {
            chosenLegacy.copyTo(File(drawableNodpi, "ic_launcher_legacy.png"), overwrite = true)
            val mipmapXxxhdpi = File(outRes, "mipmap-xxxhdpi")
            mipmapXxxhdpi.mkdirs()
            chosenLegacy.copyTo(File(mipmapXxxhdpi, "ic_launcher.png"), overwrite = true)
            chosenLegacy.copyTo(File(mipmapXxxhdpi, "ic_launcher_round.png"), overwrite = true)
        }
    }
}

android {
    namespace = "com.taiful.tfnewscraft"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taiful.tfnewscraft"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debugConfig")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            assets.directories.add(generatedWebAssetsDir.absolutePath)
            res.directories.add(generatedResDir.absolutePath)
        }
    }
}

tasks.named("preBuild") {
    dependsOn(prepareWebAssets)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.webkit:webkit:1.12.1")

    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
