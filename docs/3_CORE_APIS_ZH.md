# 核心 API 指南 (v2.0)

欢迎来到 `ComboLite` v2.0 的核心中枢！

本指南是框架所有公开 API 的权威参考。我们将深入框架的几大核心管理器，并介绍便捷的扩展函数，助你完全掌控插件化的每一个细节。

**文档结构**

1.  [**初始化与设置**](#1-初始化与设置-setup--initialization)
2.  [**`PluginManager` (总控制器)**](#2-pluginmanager-总控制器)
3.  [**`InstallerManager` (安装器)**](#3-installermanager-安装器)
4.  [**`ProxyManager` (调度器)**](#4-proxymanager-调度器)
5.  [**`PluginResourcesManager` (资源管理器)**](#5-pluginresourcesmanager-资源管理器)
6.  [**`AuthorizationManager` (授权管理器)**](#6-authorizationmanager-授权管理器)
7.  [**`PluginCrashHandler` (崩溃处理器)**](#7-plugincrashhandler-崩溃处理器)
8.  [**`Context` 扩展函数 (便捷封装)**](#8-context-扩展函数便捷封装)

-----

### 1. 初始化与设置 (Setup & Initialization)

这是启动和配置 `ComboLite` 框架的第一步，通常在你的 `Application` 类中完成。

| 管理器                  | 函数签名                                                                          | 描述                                                                                  |
|:---------------------|:------------------------------------------------------------------------------|:------------------------------------------------------------------------------------|
| `PluginManager`      | `fun initialize(context: Application, onSetup: (suspend () -> Unit)? = null)` | **[必需]** 初始化插件框架核心。这是整个框架的入口点。`onSetup` 代码块提供了一个在后台协程中执行框架配置（如设置代理池、校验策略等）的便捷方式。    |
| `PluginManager`      | `suspend fun awaitInitialization()`                                           | 一个挂起函数，它会等待直到 `PluginManager` 完成初始化。在需要确保框架就绪后才能执行操作的场景下非常有用。                       |
| `PluginCrashHandler` | `fun initialize(context: Application)`                                        | **[必需]** 初始化并注册全局插件崩溃处理器。**建议在 `PluginManager.initialize` 之前调用**，以确保能捕获整个初始化过程中的异常。 |

-----

### 2. `PluginManager` (总控制器)

框架的唯一公共 API 入口，提供对插件运行时生命周期、依赖关系和跨插件通信的全面控制。

#### 2.1 状态查询与监听

| 函数签名                                                                 | 描述                                                   |
|:---------------------------------------------------------------------|:-----------------------------------------------------|
| `val initStateFlow: StateFlow<InitState>`                            | 获取框架初始化状态的 `Flow`。                                   |
| `val loadedPluginsFlow: StateFlow<Map<String, LoadedPluginInfo>>`    | 获取所有**当前已加载**插件信息的 `Flow`。可用于构建能实时反应插件状态的响应式 UI。     |
| `val pluginInstancesFlow: StateFlow<Map<String, IPluginEntryClass>>` | 获取所有**当前已加载**插件实例的 `Flow`。                           |
| `val isInitialized: Boolean`                                         | 同步检查框架当前是否已完成初始化。                                    |
| `getPluginInstance(pluginId: String): IPluginEntryClass?`            | 获取指定插件入口类 `IPluginEntryClass` 的实例，用于直接交互。            |
| `getPluginInfo(pluginId: String): LoadedPluginInfo?`                 | 获取已加载插件的运行时信息，包含 `PluginInfo` 和 `PluginClassLoader`。 |
| `getAllInstallPlugins(): List<PluginInfo>`                           | 获取所有**已安装**插件的元数据列表。                                 |
| `getAllPluginInstances(): Map<String, IPluginEntryClass>`            | 获取所有**当前已加载**的插件实例 `Map`。                            |

#### 2.2 运行时生命周期控制

| 函数签名                                                                        | 权限要求   | 描述                                                                                       |
|:----------------------------------------------------------------------------|:-------|:-----------------------------------------------------------------------------------------|
| `suspend fun launchPlugin(pluginId: String): Boolean`                       | `SELF` | 启动或重启一个插件。这是**最核心、最常用**的运行时 API。当对一个已被依赖的插件调用此方法时，会自动触发**链式重启 (Chain Restart)**，确保依赖一致性。 |
| `suspend fun unloadPlugin(pluginId: String)`                                | `SELF` | 将一个已加载的插件从内存中移除，并**彻底清理**其所有运行时资源（包括 ClassLoader、资源、Koin模块等）。                            |
| `suspend fun loadEnabledPlugins(): Int`                                     | `HOST` | 加载所有已安装且状态为“已启用”的插件。通常在应用启动时调用，用于恢复工作环境。                                                 |
| `suspend fun setPluginEnabled(pluginId: String, enabled: Boolean): Boolean` | `HOST` | **[高风险]** 修改插件的启用状态。被禁用的插件在应用下次启动时将不会自动加载。此操作会**持久化**到 `plugins.xml`。                    |

#### 2.3 框架配置

| 函数签名                                                              | 权限要求   | 描述                                                                                   |
|:------------------------------------------------------------------|:-------|:-------------------------------------------------------------------------------------|
| `suspend fun setValidationStrategy(strategy: ValidationStrategy)` | `HOST` | **[高风险]** 设置插件安装时的签名验证策略。可选值为 `Strict` (严格同签)、`UserGrant` (用户授权) 和 `Insecure` (不校验)。 |

#### 2.4 服务发现与依赖查询

| 函数签名                                                                          | 描述                                                                                          |
|:------------------------------------------------------------------------------|:--------------------------------------------------------------------------------------------|
| `fun <T : Any> getInterface(interfaceClass: Class<T>, className: String): T?` | **[特殊功能]** 在不感知具体实现位置的情况下，获取一个接口的实例。这是实现**跨插件服务调用**和**极致解耦**的最佳方式。它利用全局类索引，自动定位并返回目标插件中的实现。 |
| `fun getPluginDependentsChain(pluginId: String): List<String>`                | 查询“**谁依赖我？**”，返回所有直接或间接依赖于指定插件的插件 ID 列表。用于卸载/更新前的安全检查。                                      |
| `fun getPluginDependenciesChain(pluginId: String): List<String>`              | 查询“**我依赖谁？**”，返回指定插件所依赖的所有插件 ID 列表。用于调试和诊断。                                                 |

-----

### 3. `InstallerManager` (安装器)

插件的物理文件管理，由 `PluginManager.installerManager` 负责。**强烈建议在后台线程中执行**。

| 函数签名                                                                                             | 权限要求   | 描述                                                                         |
|:-------------------------------------------------------------------------------------------------|:-------|:---------------------------------------------------------------------------|
| `suspend fun installPlugin(pluginApkFile: File, forceOverwrite: Boolean = false): InstallResult` | `HOST` | **[高风险]** 将一个外部 APK 文件转化为一个可用插件的核心方法。它内部执行了严格的安全流程，包括签名校验、元数据解析、版本对比和组件解析。 |
| `suspend fun uninstallPlugin(pluginId: String): Boolean`                                         | `SELF` | 从系统中彻底移除一个插件及其所有相关文件（包括 APK、缓存、so库等）。                                      |

-----

### 4. `ProxyManager` (调度器)

四大组件的“幕后功臣”，由 `PluginManager.proxyManager` 负责。**在使用任何四大组件功能前，必须先进行配置。**

| 函数签名                                                                     | 描述                                                                   |
|:-------------------------------------------------------------------------|:---------------------------------------------------------------------|
| `fun setHostActivity(hostActivity: Class<out BaseHostActivity>)`         | 配置用于代理所有插件 `Activity` 的宿主 `Activity` 类。                              |
| `fun setServicePool(serviceProxyPool: List<Class<out BaseHostService>>)` | 配置用于代理插件 `Service` 的“代理服务池”。                                         |
| `fun setHostProviderAuthority(authority: String)`                        | 配置用于代理所有插件 `ContentProvider` 的宿主 `Provider` 的 `Authority`。           |
| `fun getRunningInstancesFor(serviceClassName: String): List<String>`     | 获取某个 `Service` 类的所有正在运行的实例 ID 列表。**此 API 对于需要与后台服务状态同步的 UI 场景非常有用**。 |

-----

### 5. `PluginResourcesManager` (资源管理器)

`PluginResourcesManager` 实现了对所有插件资源的**合并式管理**。

> **核心理念：透明化**
>
> 对开发者而言，资源管理是**完全透明**的。你**不需要**也**不应该**直接调用 `PluginResourcesManager` 的任何 API。
>
> 只要你的 Activity 继承了 `BaseHostActivity` 或按要求在自定义 `Activity` 中重写了 `getResources()` 方法，你就可以像访问宿主自身资源一样，使用 `R.string.xxx`、`R.drawable.xxx` 等方式，无差别地访问来自**任何已加载插件**的资源。

| 唯一需要关心的场景                                                                                                                                                                                                          |
|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `override fun getResources(): Resources { return PluginManager.resourcesManager.getResources() ?: super.getResources() }` <br> 如果你有一个自定义 `Activity` **无法继承 `BaseHostActivity`**，则需要手动重写此方法以接入 `ComboLite` 的资源管理体系。 |

-----

### 6. `AuthorizationManager` (授权管理器)

v2.0 新增的核心安全组件，由 `PluginManager.authorizationManager` 负责。

| 函数签名                                                                   | 权限要求   | 描述                                                                                                                             |
|:-----------------------------------------------------------------------|:-------|:-------------------------------------------------------------------------------------------------------------------------------|
| `suspend fun setAuthorizationHandler(handler: IAuthorizationHandler?)` | `HOST` | **[高风险]** 设置自定义的授权处理器。宿主可实现 `IAuthorizationHandler` 接口，以自定义所有需要用户确认的敏感操作（如高权限API调用、未知签名插件安装）的 UI 和逻辑。传入 `null` 可恢复为默认的 UI 处理器。 |

-----

### 7. `PluginCrashHandler` (崩溃处理器)

v2.0 重构的异常处理核心，能自动隔离并禁用崩溃的插件。

| 函数签名                                                                              | 权限要求   | 描述                                            |
|:----------------------------------------------------------------------------------|:-------|:----------------------------------------------|
| `suspend fun setGlobalClashCallback(callback: IPluginCrashCallback?)`             | `HOST` | **[高风险]** 注册一个全局的插件崩溃回调，用于宿主统一处理和上报所有插件的异常。   |
| `suspend fun setClashCallback(pluginId: String, callback: IPluginCrashCallback?)` | `SELF` | 为指定插件注册一个专属的崩溃回调。该回调拥有比全局回调更高的优先级，但仅对该插件自身生效。 |

-----

### 8. `Context` 扩展函数(便捷封装)

为了简化日常开发，`ComboLite` 提供了丰富的 `Context` 扩展函数。**在日常业务开发中，我们强烈建议您优先使用这些便捷的封装**。

| 分类                      | 函数签名                                                     | 描述                                                  |
|:------------------------|:---------------------------------------------------------|:----------------------------------------------------|
| **Activity**            | `startPluginActivity(cls, options, block)`               | 启动一个插件 Activity。                                    |
| **Service**             | `startPluginService(cls, instanceId, block)`             | 启动一个插件 Service（支持多实例）。                              |
|                         | `bindPluginService(cls, instanceId, conn, flags, block)` | 绑定到一个插件 Service（支持多实例）。                             |
|                         | `stopPluginService(cls, instanceId, block)`              | 停止一个插件 Service（支持多实例）。                              |
| **Broadcast**           | `sendInternalBroadcast(action, block)`                   | 发送一个安全的内部广播。                                        |
| **Content<br>Provider** | `queryPlugin(uri, ...)`                                  | 查询插件 `ContentProvider`。                             |
|                         | `insertPlugin(uri, ...)`                                 | 插入数据到插件 `ContentProvider`。                          |
|                         | `deletePlugin(uri, ...)`                                 | 从插件 `ContentProvider` 删除数据。                         |
|                         | `updatePlugin(uri, ...)`                                 | 更新插件 `ContentProvider` 中的数据。                        |
|                         | `callPlugin(uri, method, ...)`                           | 调用插件 `ContentProvider` 的自定义方法。                      |
|                         | `registerPluginObserver(uri, ..., observer)`             | 为插件 `ContentProvider` 注册内容观察者。                      |
|                         | `unregisterPluginObserver(observer)`                     | 注销内容观察者。                                            |
| **调试**                  | `suspend fun installPluginsFromAssetsForDebug(...)`      | **[仅限Debug]** 从宿主 `assets` 目录并行安装或更新所有插件，实现源码级无缝调试。 |
