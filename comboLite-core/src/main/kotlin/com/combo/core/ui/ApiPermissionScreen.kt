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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.combo.core.model.AuthorizationRequest
import com.combo.core.model.AuthorizationRequest.Companion.KEY_API_METHOD_NAME
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PERMISSION_LEVEL
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PLUGIN_NAME
import com.combo.core.model.AuthorizationRequest.Companion.KEY_TARGET_PLUGIN_ID
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
                    title = { Text("操作确认") },
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
                        Text(text = pluginName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = request.callingPluginId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 2. 权限申请区域
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "插件请求执行以下操作:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // 权限卡片
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                    ) {
                        // API名称和用途
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
                            // 权限等级标签 (直接内置逻辑)
                            if (permissionLevel == PermissionLevel.HOST.name) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "宿主权限",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "宿主权限",
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = purpose,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 操作目标区域
                    if (!targetPluginId.isNullOrBlank() && targetPluginId != "宿主") {
                        Spacer(modifier = Modifier.height(24.dp))
                        InfoRow("操作目标", targetPluginId)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 3. 按钮区域
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
        "installPlugin" -> "安装新的插件模块"
        "uninstallPlugin" -> "卸载已安装的插件模块"
        "setPluginEnabled" -> "启用或禁用插件模块"
        "launchPlugin" -> "启动或重新加载插件"
        "unloadPlugin" -> "从内存中卸载插件"
        "setValidationStrategy" -> "更改框架的安全校验策略"
        "loadEnabledPlugins" -> "一次性加载所有已启用的插件"
        else -> "执行一项未指定的敏感操作"
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