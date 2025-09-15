# Core API Guide (v2.0)

Welcome to the central hub of `ComboLite` v2.0!

This guide is the authoritative reference for all public APIs of the framework. We will delve into the core managers of the framework and introduce convenient extension functions to give you complete control over every detail of pluginization.

**Document Structure**

1.  [**Setup & Initialization**](#1-setup--initialization)
2.  [**`PluginManager` (Main Controller)**](#2-pluginmanager-main-controller)
3.  [**`InstallerManager` (Installer)**](#3-installermanager-installer)
4.  [**`ProxyManager` (Dispatcher)**](#4-proxymanager-dispatcher)
5.  [**`PluginResourcesManager` (Resource Manager)**](#5-pluginresourcesmanager-resource-manager)
6.  [**`AuthorizationManager` (Authorization Manager)**](#6-authorizationmanager-authorization-manager)
7.  [**`PluginCrashHandler` (Crash Handler)**](#7-plugincrashhandler-crash-handler)
8.  [**`Context` Extension Functions (Convenience Wrappers)**](#8-context-extension-functions-convenience-wrappers)

-----

### 1. Setup & Initialization

This is the first step to start and configure the `ComboLite` framework, typically done in your `Application` class.

| Manager              | Function Signature                                                            | Description                                                                                                                                                                                                                                                                            |
|:---------------------|:------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PluginManager`      | `fun initialize(context: Application, onSetup: (suspend () -> Unit)? = null)` | **[Required]** Initializes the core of the plugin framework. This is the entry point for the entire framework. The `onSetup` block provides a convenient way to perform framework configurations (like setting up proxy pools, validation strategies, etc.) in a background coroutine. |
| `PluginManager`      | `suspend fun awaitInitialization()`                                           | A suspending function that waits until `PluginManager` has completed its initialization. Very useful in scenarios where you need to ensure the framework is ready before performing an operation.                                                                                      |
| `PluginCrashHandler` | `fun initialize(context: Application)`                                        | **[Required]** Initializes and registers the global plugin crash handler. **It is recommended to call this before `PluginManager.initialize`** to ensure it can catch exceptions during the entire initialization process.                                                             |

-----

### 2. `PluginManager` (Main Controller)

The single public API entry point of the framework, providing comprehensive control over the plugin runtime lifecycle, dependencies, and cross-plugin communication.

#### 2.1 Status Query & Listening

| Function Signature                                                   | Description                                                                                                                                  |
|:---------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------|
| `val initStateFlow: StateFlow<InitState>`                            | Gets a `Flow` of the framework's initialization state.                                                                                       |
| `val loadedPluginsFlow: StateFlow<Map<String, LoadedPluginInfo>>`    | Gets a `Flow` of information for all **currently loaded** plugins. Can be used to build a reactive UI that reflects real-time plugin status. |
| `val pluginInstancesFlow: StateFlow<Map<String, IPluginEntryClass>>` | Gets a `Flow` of all **currently loaded** plugin instances.                                                                                  |
| `val isInitialized: Boolean`                                         | Synchronously checks if the framework has completed initialization.                                                                          |
| `getPluginInstance(pluginId: String): IPluginEntryClass?`            | Gets the instance of a specified plugin's entry class, `IPluginEntryClass`, for direct interaction.                                          |
| `getPluginInfo(pluginId: String): LoadedPluginInfo?`                 | Gets the runtime information of a loaded plugin, including `PluginInfo` and `PluginClassLoader`.                                             |
| `getAllInstallPlugins(): List<PluginInfo>`                           | Gets a list of metadata for all **installed** plugins.                                                                                       |
| `getAllPluginInstances(): Map<String, IPluginEntryClass>`            | Gets a `Map` of all **currently loaded** plugin instances.                                                                                   |

#### 2.2 Runtime Lifecycle Control

| Function Signature                                                          | Permission Required | Description                                                                                                                                                                                                                                |
|:----------------------------------------------------------------------------|:--------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `suspend fun launchPlugin(pluginId: String): Boolean`                       | `SELF`              | Launches or restarts a plugin. This is the **most core and frequently used** runtime API. Calling this method on a plugin that is a dependency for others will automatically trigger a **Chain Restart**, ensuring dependency consistency. |
| `suspend fun unloadPlugin(pluginId: String)`                                | `SELF`              | Removes a loaded plugin from memory and **completely cleans up** all its runtime resources (including ClassLoader, resources, Koin modules, etc.).                                                                                         |
| `suspend fun loadEnabledPlugins(): Int`                                     | `HOST`              | Loads all installed plugins that are in an "enabled" state. Typically called on application startup to restore the working environment.                                                                                                    |
| `suspend fun setPluginEnabled(pluginId: String, enabled: Boolean): Boolean` | `HOST`              | **[High Risk]** Modifies the enabled state of a plugin. A disabled plugin will not be automatically loaded on the next app start. This operation is **persisted** to `plugins.xml`.                                                        |

#### 2.3 Framework Configuration

| Function Signature                                                | Permission Required | Description                                                                                                                                                                                   |
|:------------------------------------------------------------------|:--------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `suspend fun setValidationStrategy(strategy: ValidationStrategy)` | `HOST`              | **[High Risk]** Sets the signature validation strategy for plugin installation. Options are `Strict` (identical signature), `UserGrant` (user authorization), and `Insecure` (no validation). |

#### 2.4 Service Discovery & Dependency Query

| Function Signature                                                            | Description                                                                                                                                                                                                                                                                                                               |
|:------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fun <T : Any> getInterface(interfaceClass: Class<T>, className: String): T?` | **[Special Feature]** Gets an instance of an interface without knowing its specific implementation location. This is the best way to achieve **cross-plugin service calls** and **ultimate decoupling**. It utilizes the global class index to automatically locate and return the implementation from the target plugin. |
| `fun getPluginDependentsChain(pluginId: String): List<String>`                | Queries "**Who depends on me?**" and returns a list of plugin IDs that directly or indirectly depend on the specified plugin. Useful for safety checks before uninstalling/updating.                                                                                                                                      |
| `fun getPluginDependenciesChain(pluginId: String): List<String>`              | Queries "**What do I depend on?**" and returns a list of all plugin IDs that the specified plugin depends on. Useful for debugging and diagnostics.                                                                                                                                                                       |

-----

### 3. `InstallerManager` (Installer)

Manages the physical files of plugins, handled by `PluginManager.installerManager`. **It is strongly recommended to execute this on a background thread**.

| Function Signature                                                                               | Permission Required | Description                                                                                                                                                                                                                               |
|:-------------------------------------------------------------------------------------------------|:--------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `suspend fun installPlugin(pluginApkFile: File, forceOverwrite: Boolean = false): InstallResult` | `HOST`              | **[High Risk]** The core method for converting an external APK file into a usable plugin. It internally executes a strict security process, including signature validation, metadata parsing, version comparison, and component analysis. |
| `suspend fun uninstallPlugin(pluginId: String): Boolean`                                         | `SELF`              | Completely removes a plugin and all its related files (including APK, cache, .so libraries, etc.) from the system.                                                                                                                        |

-----

### 4. `ProxyManager` (Dispatcher)

The "behind-the-scenes workhorse" for the four major components, handled by `PluginManager.proxyManager`. **Configuration is mandatory before using any of the four major component features.**

| Function Signature                                                       | Description                                                                                                                                                                   |
|:-------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fun setHostActivity(hostActivity: Class<out BaseHostActivity>)`         | Configures the host `Activity` class used to proxy all plugin `Activity` components.                                                                                          |
| `fun setServicePool(serviceProxyPool: List<Class<out BaseHostService>>)` | Configures the "proxy service pool" used to proxy plugin `Service` components.                                                                                                |
| `fun setHostProviderAuthority(authority: String)`                        | Configures the `Authority` of the host `Provider` used to proxy all plugin `ContentProvider` components.                                                                      |
| `fun getRunningInstancesFor(serviceClassName: String): List<String>`     | Gets a list of all running instance IDs for a specific `Service` class. **This API is very useful for UI scenarios that need to sync with the state of background services.** |

-----

### 5. `PluginResourcesManager` (Resource Manager)

`PluginResourcesManager` implements **unified management** of all plugin resources.

> **Core Philosophy: Transparency**
>
> For developers, resource management is **completely transparent**. You **do not need to**, and **should not**, call any APIs of `PluginResourcesManager` directly.
>
> As long as your Activity inherits from `BaseHostActivity` or you override the `getResources()` method in your custom `Activity` as required, you can access resources from **any loaded plugin** using `R.string.xxx`, `R.drawable.xxx`, etc., just as you would access the host's own resources.

| The only scenario you need to care about                                                                                                                                                                                                                                                                              |
|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `override fun getResources(): Resources { return PluginManager.resourcesManager.getResources() ?: super.getResources() }` <br> If you have a custom `Activity` that **cannot inherit from `BaseHostActivity`**, you need to manually override this method to integrate with `ComboLite`'s resource management system. |

-----

### 6. `AuthorizationManager` (Authorization Manager)

A core security component new in v2.0, handled by `PluginManager.authorizationManager`.

| Function Signature                                                     | Permission Required | Description                                                                                                                                                                                                                                                                                                                                    |
|:-----------------------------------------------------------------------|:--------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `suspend fun setAuthorizationHandler(handler: IAuthorizationHandler?)` | `HOST`              | **[High Risk]** Sets a custom authorization handler. The host can implement the `IAuthorizationHandler` interface to customize the UI and logic for all sensitive operations that require user confirmation (e.g., high-privilege API calls, installation of plugins with unknown signatures). Passing `null` restores the default UI handler. |

-----

### 7. `PluginCrashHandler` (Crash Handler)

The refactored exception handling core in v2.0, which automatically isolates and disables crashing plugins.

| Function Signature                                                                | Permission Required | Description                                                                                                                                                      |
|:----------------------------------------------------------------------------------|:--------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `suspend fun setGlobalClashCallback(callback: IPluginCrashCallback?)`             | `HOST`              | **[High Risk]** Registers a global plugin crash callback, allowing the host to centrally handle and report exceptions from all plugins.                          |
| `suspend fun setClashCallback(pluginId: String, callback: IPluginCrashCallback?)` | `SELF`              | Registers a dedicated crash callback for a specific plugin. This callback has a higher priority than the global callback but only applies to that plugin itself. |

-----

### 8. `Context` Extension Functions (Convenience Wrappers)

To simplify daily development, `ComboLite` provides a rich set of `Context` extension functions. **In routine business development, we strongly recommend you prioritize using these convenient wrappers**.

| Category                | Function Signature                                       | Description                                                                                                                          |
|:------------------------|:---------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------|
| **Activity**            | `startPluginActivity(cls, options, block)`               | Starts a plugin Activity.                                                                                                            |
| **Service**             | `startPluginService(cls, instanceId, block)`             | Starts a plugin Service (supports multiple instances).                                                                               |
|                         | `bindPluginService(cls, instanceId, conn, flags, block)` | Binds to a plugin Service (supports multiple instances).                                                                             |
|                         | `stopPluginService(cls, instanceId, block)`              | Stops a plugin Service (supports multiple instances).                                                                                |
| **Broadcast**           | `sendInternalBroadcast(action, block)`                   | Sends a secure internal broadcast.                                                                                                   |
| **Content<br>Provider** | `queryPlugin(uri, ...)`                                  | Queries a plugin `ContentProvider`.                                                                                                  |
|                         | `insertPlugin(uri, ...)`                                 | Inserts data into a plugin `ContentProvider`.                                                                                        |
|                         | `deletePlugin(uri, ...)`                                 | Deletes data from a plugin `ContentProvider`.                                                                                        |
|                         | `updatePlugin(uri, ...)`                                 | Updates data in a plugin `ContentProvider`.                                                                                          |
|                         | `callPlugin(uri, method, ...)`                           | Calls a custom method of a plugin `ContentProvider`.                                                                                 |
|                         | `registerPluginObserver(uri, ..., observer)`             | Registers a content observer for a plugin `ContentProvider`.                                                                         |
|                         | `unregisterPluginObserver(observer)`                     | Unregisters a content observer.                                                                                                      |
| **Debugging**           | `suspend fun installPluginsFromAssetsForDebug(...)`      | **[Debug Only]** Installs or updates all plugins in parallel from the host's `assets` directory for seamless source-level debugging. |