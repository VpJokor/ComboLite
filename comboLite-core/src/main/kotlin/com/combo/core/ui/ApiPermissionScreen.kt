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

package com.combo.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.combo.core.runtime.PluginManager
import com.combo.core.model.AuthorizationRequest
import com.combo.core.model.AuthorizationRequest.Companion.KEY_API_METHOD_NAME
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PERMISSION_LEVEL
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PLUGIN_NAME
import com.combo.core.model.AuthorizationRequest.Companion.KEY_TARGET_PLUGIN_ID
import com.combo.core.runtime.installer.InstallerManager
import com.combo.core.security.auth.AuthorizationManager
import com.combo.core.security.crash.PluginCrashHandler
import com.combo.core.security.permission.PermissionLevel
import com.combo.core.ui.component.InfoRow
import com.combo.core.ui.component.PrimaryButton
import com.combo.core.ui.component.SecondaryButton
import com.combo.core.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPermissionScreen(
    request: AuthorizationRequest,
    onResult: (Boolean) -> Unit,
    onExit: () -> Unit
) {
    val details = request.details
    val pluginName = details[KEY_PLUGIN_NAME] ?: request.callingPluginId
    val apiMethodName = details[KEY_API_METHOD_NAME] ?: "未知操作"
    val permissionLevel = details[KEY_PERMISSION_LEVEL]
    val targetPluginId = details[KEY_TARGET_PLUGIN_ID]

    val purpose = getPurposeFromApiName(apiMethodName)

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onExit) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                // 1. 插件信息区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = android.R.mipmap.sym_def_app_icon),
                        contentDescription = "Plugin Icon",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = pluginName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            text = request.callingPluginId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "插件请求执行以下操作:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = apiMethodName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (permissionLevel == PermissionLevel.HOST.name) {
                                Row(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "宿主权限",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "宿主权限",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = purpose,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!targetPluginId.isNullOrBlank() && targetPluginId != "宿主") {
                        Spacer(modifier = Modifier.height(24.dp))
                        InfoRow("操作目标", targetPluginId)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "拒绝",
                        onClick = { onResult(false) },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = "允许",
                        onClick = { onResult(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 根据API方法名获取其人类可读的用途描述
 */
private fun getPurposeFromApiName(apiName: String): String {
    return when (apiName) {
        PluginManager::launchPlugin.name -> "加载或重新启动一个插件。此操作可能会激活插件功能，或因重启中断该插件正在进行的服务。"
        PluginManager::unloadPlugin.name -> "立即停止一个已加载的插件。如果其他插件正依赖于它，可能会导致它们功能异常或崩溃。"
        PluginManager::loadEnabledPlugins.name -> "加载所有被标记为'已启用'的插件。这是一个全局性的批量操作，通常在应用启动时执行。"
        PluginManager::setPluginEnabled.name -> "设置目标插件是否在应用下次启动时自动加载。禁用插件可以防止应用在下次启动时自动运行该插件。"
        InstallerManager::installPlugin.name -> "从APK文件安装一个新插件或更新一个现有插件。此操作将在应用中引入新的代码和功能，具有高风险。"
        InstallerManager::uninstallPlugin.name -> "从本应用中永久性地卸载移除一个插件及其所有相关数据。这是一个不可恢复的操作。"
        PluginCrashHandler::setGlobalClashCallback.name -> "注册一个全局的插件崩溃回调。这会改变应用处理插件错误的方式，可能被用于日志记录、自定义恢复逻辑，或拦截错误信息。"
        AuthorizationManager::setAuthorizationHandler.name -> "替换框架默认的授权处理器。这将改变未来所有权限请求的界面和处理逻辑，是一项高度敏感的核心安全操作。"
        PluginManager::setValidationStrategy.name -> "修改插件安装时的签名验证策略。改变此策略会直接影响整个应用的安全模型。"
        else -> "执行一项未在描述列表中指定的敏感操作，请谨慎处理。"
    }
}


@Preview(showBackground = true, name = "HOST Permission Light")
@Composable
fun ApiPermissionScreenHostLightPreview() {
    val request = AuthorizationRequest(
        type = AuthorizationRequest.RequestType.API_PERMISSION,
        callingPluginId = "com.plugin.app_store",
        details = mapOf(
            KEY_PLUGIN_NAME to "应用商店插件",
            KEY_API_METHOD_NAME to "installPlugin",
            KEY_PERMISSION_LEVEL to "HOST",
            KEY_TARGET_PLUGIN_ID to "com.plugin.new_app"
        )
    )
    ApiPermissionScreen(request = request, onResult = {}, onExit = {})
}

@Preview(showBackground = true, name = "SELF Permission Dark")
@Composable
fun ApiPermissionScreenSelfDarkPreview() {
    AppTheme(darkTheme = true) {
        val request = AuthorizationRequest(
            type = AuthorizationRequest.RequestType.API_PERMISSION,
            callingPluginId = "com.plugin.downloader",
            details = mapOf(
                KEY_PLUGIN_NAME to "下载器",
                KEY_API_METHOD_NAME to "uninstallPlugin",
                KEY_PERMISSION_LEVEL to "SELF",
                KEY_TARGET_PLUGIN_ID to "com.plugin.downloader"
            )
        )
        ApiPermissionScreen(request = request, onResult = {}, onExit = {})
    }
}