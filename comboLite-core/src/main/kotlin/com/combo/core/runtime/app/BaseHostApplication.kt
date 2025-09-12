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

package com.combo.core.runtime.app

import android.app.Application
import android.content.res.AssetManager
import android.content.res.Resources
import com.combo.core.api.IPluginEntryClass
import com.combo.core.runtime.PluginManager
import com.combo.core.security.crash.PluginCrashHandler

/**
 * 宿主端的插件框架Application基类
 * 用于快速一键初始化插件框架，加载插件
 */
open class BaseHostApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PluginCrashHandler.initialize(this)

        val isDebug = isDebugBuild()
        if (isDebug) {
            getDebugPlugins().forEach { (id, clazz) ->
                PluginManager.registerDebugPlugin(id, clazz)
            }
        }

        PluginManager.initialize(
            context = this,
            pluginLoader = getPluginLoader()
        )
    }

    /**
     * 重写getResources方法，返回插件资源
     */
    override fun getResources(): Resources =
        if (PluginManager.isInitialized) {
            PluginManager.resourcesManager.getResources()
        } else {
            super.getResources()
        }

    /**
     * 重写getAssets方法，返回插件资源
     */
    override fun getAssets(): AssetManager =
        if (PluginManager.isInitialized) {
            PluginManager.resourcesManager.getResources().assets
        } else {
            super.getAssets()
        }

    /**
     * 子类应重写此方法以提供 Debug 模式下需要源码调试的插件列表。
     *
     * @return 返回一个 Map，其中 Key 是插件 ID (包名)，Value 是插件的入口类 (IPluginEntryClass)。
     */
    protected open fun getDebugPlugins(): Map<String, Class<out IPluginEntryClass>> {
        return emptyMap()
    }

    /**
     * 提供在 Release 模式下用于加载插件的逻辑块。
     * 默认实现是加载所有已启用的插件。如果宿主有更复杂的加载需求（如分步加载），可以重写此方法。
     *
     * @return 一个将在初始化完成后被调用的 suspend lambda。
     */
    protected open fun getPluginLoader(): suspend () -> Unit {
        return { PluginManager.loadEnabledPlugins() }
    }

    /**
     * 判断当前是否为 Debug 构建模式。
     * @return 如果是 Debug 构建，则为 true；否则为 false。
     */
    private fun isDebugBuild(): Boolean {
        return try {
            val buildConfigClass = Class.forName("$packageName.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (_: Exception) {
            false
        }
    }
}