/*
 * Copyright (c) 2025, 贵州君城网络科技有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.combo.core.security.permission

/**
 * 定义了插件调用敏感 API 所需的权限等级。
 */
enum class PermissionLevel {
    /**
     * **宿主级权限 (HOST)**
     * - **授予条件**: 插件签名必须与宿主应用完全一致。这是最高级别的信任。
     * - **适用API**:
     * - `PluginManager.installerManager.installPlugin(...)` (安装任意新插件)
     * - `PluginManager.installerManager.uninstallPlugin(...)` (卸载其他插件)
     * - `PluginManager.setPluginEnabled(...)` (启用/禁用其他插件)
     * - 访问宿主内部的私有数据或API。
     */
    HOST,

    /**
     * **签发者级权限 (SIGNATURE)**
     * - **授予条件**: 插件签名链与宿主签名链中的任意一个匹配即可。这适用于拥有相同开发者签名的插件生态。
     * - **适用API**:
     * - 插件间的私有组件调用（如未导出的Service、ContentProvider）。
     * - 共享数据目录。
     */
    SIGNATURE,

    /**
     * **自我管理权限 (SELF)**
     * - **授予条件**: 调用方插件ID必须与操作目标插件ID一致。
     * - **适用API**:
     * - `PluginManager.installerManager.installPlugin(...)` (当用于更新自身时)
     * - `PluginManager.installerManager.uninstallPlugin(this.pluginId)` (卸载自己)
     * - `PluginManager.launchPlugin(this.pluginId)` (重启自己)
     */
    SELF,

    /**
     * **用户授权权限 (USER_GRANTABLE)**
     * - **授予条件**: 在调用时，框架会触发一个可由宿主自定义的回调，通常用于弹出对话框请求用户同意。
     * - **适用API**:
     * - `PluginManager.installerManager.installPlugin(...)` (当安装一个未知来源或签名不一致的插件时)
     * - 访问敏感设备信息（需要宿主进行二次封装和授权，如联系人、位置等）。
     */
    USER_GRANTABLE,

    /**
     * **无限制 (NONE)**
     * - **授予条件**: 对所有已加载的插件开放。
     * - **适用API**:
     * - `PluginManager.getPluginInfo(...)`
     * - `PluginManager.isPluginLoaded(...)`
     * - `PluginManager.getInterface(...)` (调用其他插件公开的接口)
     * - 框架提供的各类查询和只读API。
     */
    NONE
}