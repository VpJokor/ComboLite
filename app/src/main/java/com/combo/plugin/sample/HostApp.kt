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

package com.combo.plugin.sample

import com.combo.core.api.IPluginEntryClass
import com.combo.core.model.PluginCrashInfo
import com.combo.core.runtime.PluginManager
import com.combo.core.runtime.PluginManager.setValidationStrategy
import com.combo.core.runtime.ValidationStrategy
import com.combo.core.runtime.app.BaseHostApplication
import com.combo.core.security.crash.IPluginCrashCallback
import com.combo.core.security.crash.PluginCrashHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber
import kotlin.jvm.java

/**
 * 主应用程序类
 * 该类是主应用程序的入口点，负责初始化插件框架和配置应用程序的全局状态
 */
class HostApp : BaseHostApplication(),IPluginCrashCallback {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        PluginManager.apply {
            proxyManager.setHostActivity(HostActivity::class.java)
            proxyManager.setServicePool(
                listOf(
                    HostService1::class.java,
                    HostService2::class.java,
                    HostService3::class.java,
                    HostService4::class.java,
                    HostService5::class.java,
                    HostService6::class.java,
                    HostService7::class.java,
                    HostService8::class.java,
                    HostService9::class.java,
                    HostService10::class.java,
                ),
            )
            proxyManager.setHostProviderAuthority("com.combo.plugin.sample.provider")
        }

        CoroutineScope(Dispatchers.IO).launch {
            // 设置插件验证策略
            setValidationStrategy(ValidationStrategy.UserGrant)
            // 设置插件崩溃处理回调
            PluginCrashHandler.setGlobalClashCallback(
                object : IPluginCrashCallback {
                    /**
                     * 当插件因 ClassCastException 崩溃时调用。
                     * 通常发生在插件热更新后，新旧类的实例冲突。
                     *
                     * @param info 崩溃详情。
                     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
                     */
                    override fun onClassCastException(info: PluginCrashInfo): Boolean {
                        Timber.e(info.throwable, "插件ClassCastException ${info.culpritPluginId}")
                        return false
                    }

                    /**
                     * 当插件因 PluginDependencyException (ClassNotFound) 崩溃时调用。
                     *
                     * @param info 崩溃详情。
                     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
                     */
                    override fun onDependencyException(info: PluginCrashInfo): Boolean {
                        Timber.e(info.throwable, "插件PluginDependencyException ${info.culpritPluginId}")
                        return false
                    }

                    /**
                     * 当插件因 Resources.NotFoundException 崩溃时调用。
                     * 通常发生在插件更新后，代码尝试访问一个不再存在的资源ID。
                     *
                     * @param info 崩溃详情。
                     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
                     */
                    override fun onResourceNotFoundException(info: PluginCrashInfo): Boolean {
                        Timber.e(info.throwable, "插件Resources.NotFoundException ${info.culpritPluginId}")
                        return false
                    }

                    /**
                     * 当插件因 API 不兼容 (如 NoSuchMethodError, NoSuchFieldError) 崩溃时调用。
                     *
                     * @param info 崩溃详情。
                     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
                     */
                    override fun onApiIncompatibleException(info: PluginCrashInfo): Boolean {
                        Timber.e(info.throwable, "插件onApiIncompatibleException ${info.culpritPluginId}")
                        return false
                    }

                    /**
                     * 当发生其他与插件相关的未知异常时调用。
                     *
                     * @param info 崩溃详情。
                     * @return `true` 表示已处理该异常，框架不再执行默认逻辑；`false` 则继续执行默认逻辑。
                     */
                    override fun onOtherPluginException(info: PluginCrashInfo): Boolean {
                        Timber.e(info.throwable, "插件onOtherPluginException ${info.culpritPluginId}")
                        return false
                    }
                }
            )
        }

        loadKoinModules(
            module {
                viewModel { LoadingViewModel(applicationContext) }
            },
        )
    }

    /**
     * 只需重写这一个方法，即可配置所有需要源码调试的插件。
     * 在 release 构建模式下，这个方法返回的列表不会被使用。
     */
    override fun getDebugPlugins(): Map<String, Class<out IPluginEntryClass>> {
        return mapOf(
            "com.combo.plugin.sample.common" to com.combo.plugin.sample.common.PluginEntryClass::class.java,
            "com.combo.plugin.sample.example" to com.combo.plugin.sample.example.PluginEntryClass::class.java,
            "com.combo.plugin.sample.guide" to com.combo.plugin.sample.guide.PluginEntryClass::class.java,
            "com.combo.plugin.sample.home" to com.combo.plugin.sample.home.PluginEntryClass::class.java,
            "com.combo.plugin.sample.setting" to com.combo.plugin.sample.setting.PluginEntryClass::class.java
        )
    }
}