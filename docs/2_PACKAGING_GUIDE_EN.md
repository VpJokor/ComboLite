# Plugin Packaging & Debugging Guide

Welcome to the `ComboLite` guide for plugin packaging and debugging!

"Packaging" is the crucial step of transforming your plugin module into a standalone APK file that can be dynamically loaded, installed, and run by the host. During the development phase, "debugging" efficiency directly determines your iteration speed.

This guide will delve into the two core workflows provided by `ComboLite`:

1.  **Seamless Development-Time Debugging**: Achieve real-time application of source code changes via a Gradle plugin.
2.  **Packaging for Production**: Manually execute tasks to generate official plugin APKs for online distribution.

We will focus on the workflow for `Library` modules using the `aar2apk` Gradle plugin, which is our officially recommended best practice.

-----

## Core Concepts: The Foundation of Packaging Strategy

Before we dive into specific operations, let's grasp two core concepts that determine your packaging strategy.

### 1. Two Plugin Forms: Library vs. Application

`ComboLite` gives you immense flexibility, allowing you to develop two different forms of Android modules as plugins:

* 📦 **Library Module (Recommended)**

    * **Nature**: A standard `com.android.library` module (produces an AAR).
    * **Characteristics**: Lightweight and non-standalone. It does not include common dependencies like the Kotlin standard library or AndroidX, assuming the host app will provide them at runtime. This is the top choice for building "Super Apps" and pursuing ultimate performance.
    * **Packaging Method**: Requires our `aar2apk` Gradle plugin to be "upgraded" into an installable APK.

* 📱 **Application Module**

    * **Nature**: A standard `com.android.application` module (produces an APK).
    * **Characteristics**: Standalone and self-contained. It acts like a mini-app, bundling most of its own dependencies.
    * **Packaging Method**: Uses the standard Android official packaging process.

### 2. Dependency Scopes: `compileOnly` vs `implementation`

Correctly using Gradle's dependency scopes is the magic behind achieving "lightweight" plugins and "dependency sharing."

* ✅ **`compileOnly` (Plugin's First Choice)**

    * **Meaning**: "Compile-time only dependency." It tells the compiler: "This library is necessary to **compile my plugin code**, but please **do not** package it into the final plugin APK. I promise that the host app will provide this library at runtime."
    * **Usage**: This is the **highest-priority and most commonly used** scope for plugin modules. All common libraries you expect the host to provide (like `comboLite-core`, `Kotlin`, `AndroidX`, `OkHttp`, etc.) should use `compileOnly`.

* ⚠️ **`implementation`**

    * **Meaning**: "Implementation dependency." It tells the compiler: "This library is needed at both compile-time and runtime. Please make sure to **package it** into the final product."
    * **Usage**: Only use this when an `Application` plugin or a `Library` plugin needs to package **private dependencies**.

> **The Golden Rule**:
> **For plugin development, `compileOnly` is the norm, `implementation` is the exception.**

-----

## Workflow One: Seamless Development-Time Debugging (Automatic Integration)

This is a revolutionary experience enhancement brought by `ComboLite` v2.0, designed to completely eliminate the tedious "build-install-run" debugging cycle of traditional plugin development.

### 1. Core Principle

Through configuration, the `aar2apk` plugin can automatically and silently perform the following operations in the background when you click the "Run" or "Debug" button in Android Studio:

1.  Compile the specified plugin `Library` modules.
2.  Package them into `debug` version APKs.
3.  Place these APKs **directly into the `assets` directory of the host app's final build output**.

This entire process has **zero intrusion** on your project's source code. You just need to modify your code as if you were developing a regular module, click run, and all changes will take effect in the host app.

### 2. Configuration Steps

Automatic integration requires two configurations to work together: one for declaration and one for enablement.

#### ① Declare Plugins in the Project Root `build.gradle.kts`

This configuration is used to **tell** the `aar2apk` plugin which `Library` modules in the project are plugins to be managed.

```kotlin
// in your project's root /build.gradle.kts
plugins {
    alias(libs.plugins.combolite.aar2apk)
}

aar2apk {
    // a. Declare all Library modules to be packaged as plugins
    modules {
        module(":plugin-user")
        module(":plugin-settings")
        // ... Add all your plugin modules
    }

    // b. (Optional) Configure global signing information for production packaging later
    signing {
        keystorePath.set(rootProject.file("your_keystore.jks").absolutePath)
        keystorePassword.set("your_password")
        keyAlias.set("your_alias")
        keyPassword.set("your_password")
    }
}
```

#### ② Enable Integration in the Host App `build.gradle.kts`

This configuration is used to **turn on** the automatic integration feature.

```kotlin
// in your :app/build.gradle.kts
plugins {
    id("io.github.lnzz123.combolite-aar2apk")
}

// Configure the plugin auto-integration feature
packagePlugins {
    // a. Master switch for the feature, set to true during development
    enabled.set(true)

    // b. Specify the build variant for packaging plugins (DEBUG or RELEASE)
    buildType.set(PackageBuildType.DEBUG)

    // c. The directory where plugin APKs are stored within the host's assets
    pluginsDir.set("plugins")
}

dependencies {
    implementation(libs.combolite.core)
}
```

**Configuration complete!** Now, you can directly modify the code in the `:plugin-user` or `:plugin-settings` modules, then run the `:app` host. The framework will automatically load the latest plugins from your `assets/plugins/` directory for debugging.

> 🔗 **Code Example**: For guidance on how to write code in your Activity to load plugins from the `assets` directory, please refer to the **[[Must Read] Quick Start](./1_QUICK_START_EN.md)** guide.

-----

## Workflow Two: Packaging for Production (Manual Execution)

When you need to publish your plugins to a server or pre-install them, you'll need to manually execute packaging tasks to generate official `release` version APKs.

### 1. Configure Packaging Strategy (Optional)

Manual packaging tasks will follow the strategies you define in the `aar2apk` configuration block in your project root `build.gradle.kts`. In addition to declaring modules, you can fine-tune the dependency packaging method for each plugin.

```kotlin
// in your project's root /build.gradle.kts
aar2apk {
    // ... signing { ... }

    modules {
        // Strategy 1: Default minimal packaging (most common)
        // Does not package any external dependency code or resources, resulting in the smallest plugin size.
        module(":plugin-user")
        module(":plugin-settings")

        // Strategy 2: Package partial dependencies (for special cases)
        // For example, if :plugin-reader depends on Library A which has drawable resources, and the host doesn't have it.
        module(":plugin-reader") {
            // Note: Library A must be included with `implementation` in :plugin-reader
            includeDependenciesRes.set(true) // Only package resources from external dependencies
        }

        // Strategy 3: Package all dependencies (rarely used, high risk)
        // Suitable when a plugin depends on a completely private library that neither the host nor other plugins have.
        module(":plugin-private") {
            includeAllDependencies() // Convenience method, same as setting the four options below to true
        }
    }
}
```

### 2. Execute Packaging Tasks (Gradle Tasks)

The `aar2apk` plugin automatically generates a series of convenient Gradle tasks for your project. You can find them in the `Plugin APKs` group in the **Gradle task panel of Android Studio**. Double-click to execute, or run the command in the terminal.

```bash
# [Recommended] Build all configured Library plugins in one go (Release version)
./gradlew buildAllReleasePluginApks

# [Recommended] Build all configured Library plugins in one go (Debug version)
./gradlew buildAllDebugPluginApks

# Build a single plugin (Release version)
./gradlew :plugin-user:buildReleasePluginApk

# Build a single plugin (Debug version)
./gradlew :plugin-user:buildDebugPluginApk

# Clean all plugin build artifacts (APKs, logs, temp files)
./gradlew cleanAllPluginApks
```

The build artifacts will be located in the project root's `build/outputs/plugin-apks/` folder, automatically categorized into `debug` and `release`.

-----

## Alternative Approach: Packaging Application Modules

Using a standard `com.android.application` module as a plugin is a traditional method that is still useful in certain scenarios.

### 1. Configuration Steps

**① Add Core Dependency**: In the `build.gradle.kts`, declare the framework core library as `compileOnly`.

**② (❗Extremely Important) Configure Package ID**: To avoid resource ID conflicts (`R.java`) with the host or other plugins, you **must** manually specify a unique `Package ID` for each `Application` plugin module.

```kotlin
// in your-application-plugin/build.gradle.kts
android {
    // ...
    aaptOptions {
        // This hex value must be unique across all Application plugins (0x02 ~ 0x7E)
        additionalParameters("--package-id", "0x70") 
    }
}
```

### 2. Execute Packaging

Use the standard task provided by AGP to complete the packaging: `./gradlew :your-application-plugin:assembleRelease`

### 3. Pros and Cons

* ✅ **Pros**: Highly independent, simple deployment, no compatibility headaches.
* ⚠️ **Trade-offs**: Larger size, and potential risk of dependency version conflicts.

-----

## How to Choose?

| Scenario | Recommended Approach | Reason |
|:---|:---|:---|
| **UI component libraries, utility classes, common business logic** | **✅ Library Module (aar2apk)** | **Top choice**. Single-purpose, small size, suitable for frequent updates and reuse as shared resources. |
| **"Super App" with a large number of plugins** | **✅ Library Module (aar2apk)** | Maximizes reuse of common dependencies, significantly reducing the overall app size and memory footprint. |
| **Extreme requirements for plugin update speed and size** | **✅ Library Module (aar2apk)** | The tiny package size makes the experience of dynamic delivery and hot-updates almost imperceptible. |
| **Large, independent feature modules** (e.g., shopping, game center) | **⚠️ Application Module** | Complex business logic, numerous dependencies, requires high cohesion and independence. |
| **Plugins provided for third-party integration** | **⚠️ Application Module** | Cannot control the host environment; must be self-contained with all dependencies to ensure stable operation. |

-----

## Important Practices & Risk Warnings

### Deep Warning: Be Cautious About Packaging Full Dependencies

Although the `aar2apk` plugin provides capabilities like `includeAllDependencies()` to bundle dependencies into the plugin, this should be considered a **fallback for special cases, not a routine operation**.

> **In the vast majority of cases, we strongly recommend you use the default minimal packaging mode.**

Packaging full dependencies can introduce a series of serious and hard-to-diagnose problems:

* **Class Duplication Conflicts**: If a plugin packages `OkHttp 4.9.0`, while the host or another plugin uses `OkHttp 4.10.0`, you may encounter fatal crashes at runtime like `NoSuchMethodError` or `ClassCastException` due to inconsistent class definitions.
* **Resource Duplication and Overwriting**: If multiple plugins or the host contain resources with the same name, the resource loaded at runtime might not be what you expect, leading to UI glitches.
* **APK Size Bloat**: Repeatedly packaging the same dependency libraries will significantly increase the plugin APK size and the total size of the final application.

**Rule of Thumb**: Only consider packaging a dependency if you are **absolutely certain** that it is **exclusively private** to this plugin and will not conflict with the host or any other plugin.

### Special Note: Impact of UI Technology Choice on Packaging Strategy

In a pluginized scenario, your choice of UI construction technology (Jetpack Compose or XML) directly affects the complexity of packaging.

* **Jetpack Compose (Recommended)**: Perfectly aligns with `ComboLite`'s `compileOnly` dependency sharing strategy with virtually no compatibility issues.
* **XML Layouts**: In `compileOnly` mode, there are difficulties with **cross-module resource references**, which may lead to build failures or require more complex packaging configurations.

> **We strongly recommend prioritizing Jetpack Compose in your plugins.**
> For an in-depth discussion of the specific issues you might encounter with XML and their solutions, please refer to the "UI Implementation Technology Choice" section in the **[[Advanced] The Four Major Components Guide](./4_COMPONENTS_GUIDE_EN.md)**.