plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "top.niunaijun.blackbox"
    compileSdk = 35

    aidlPackagedList.add("android/app/IServiceConnection.aidl")
    aidlPackagedList.add("android/accounts/IAccountManagerResponse.aidl")

    buildFeatures {
        aidl = true
        prefab = true
    }

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
        externalNativeBuild {
            cmake {
                arguments("-DBCORE_DIAGNOSTICS=0")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            externalNativeBuild {
                cmake {
                    arguments("-DBCORE_DIAGNOSTICS=1")
                }
            }
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            externalNativeBuild {
                cmake {
                    arguments("-DBCORE_DIAGNOSTICS=0")
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
        ndkVersion = "27.2.12479018"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts.add("**/libbytehook.so")
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
        warningsAsErrors = true
        disable.addAll(setOf("UnusedResources", "RestrictedApi"))
        textOutput = file("stdout")
        textReport = true
        checkOnly.addAll(setOf("NewApi", "InlinedApi"))
    }
}

tasks.matching { it.name.startsWith("compileReleaseJavaWithJavac") || it.name.startsWith("compileDebugJavaWithJavac") }.configureEach {
    val buildDirPath = layout.buildDirectory.asFile.get()
    doFirst {
        val aidlOut = buildDirPath.resolve("generated/aidl_source_output_dir")
        if (aidlOut.exists()) {
            aidlOut.walkTopDown().filter { it.name.endsWith(".java") }.forEach { f ->
                var content = f.readText()
                if (content.contains(" * Using:")) {
                    content = content.replace(Regex("(?m)^ \\* Using:.*$"), "")
                    f.writeText(content)
                }
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("com.google.android.material:material:1.12.0")
    implementation(project(":black-reflection"))
    annotationProcessor(project(":compiler"))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("com.github.tiann:FreeReflection:3.2.2")
    implementation("com.bytedance:bytehook:1.0.8")
}
