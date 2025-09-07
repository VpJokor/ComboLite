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

package com.combo.core.security.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.combo.core.component.activity.BasePluginActivity
import com.combo.core.model.AuthorizationRequest
import com.combo.core.model.AuthorizationRequest.Companion.KEY_API_METHOD_NAME
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PERMISSION_LEVEL
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PLUGIN_DESCRIPTION
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PLUGIN_ICON_URL
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PLUGIN_NAME
import com.combo.core.model.AuthorizationRequest.Companion.KEY_PLUGIN_VERSION
import com.combo.core.model.AuthorizationRequest.Companion.KEY_SIGNATURE_HASH
import com.combo.core.model.AuthorizationRequest.Companion.KEY_TARGET_PLUGIN_ID

/**
 * 通用的授权UI界面，用于处理API权限请求和插件安装请求。
 */
class AuthorizationActivity : BasePluginActivity() {

    companion object {
        const val EXTRA_REQUEST_CODE = "request_code"
        const val EXTRA_AUTH_REQUEST = "auth_request"
        const val ACTION_AUTHORIZATION_RESULT = "com.combo.core.security.AUTHORIZATION_RESULT"
        const val EXTRA_RESULT_GRANTED = "result_granted"
    }

    private var requestCode: Int = -1
    private var resultSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = proxyActivity?.intent ?: return
        this.requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)

        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_AUTH_REQUEST, AuthorizationRequest::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_AUTH_REQUEST) as? AuthorizationRequest
        }

        if (request == null) {
            finishWithResult(this.requestCode, false)
            return
        }

        proxyActivity?.setContent {
            MaterialTheme {
                AuthorizationScreen(
                    request = request,
                    onResult = { granted ->
                        finishWithResult(this.requestCode, granted)
                    }
                )
            }
        }
    }

    private fun finishWithResult(requestCode: Int, granted: Boolean) {
        if (resultSent) return
        resultSent = true

        val resultIntent = Intent(ACTION_AUTHORIZATION_RESULT).apply {
            `package` = proxyActivity?.packageName
            putExtra(EXTRA_REQUEST_CODE, requestCode)
            putExtra(EXTRA_RESULT_GRANTED, granted)
        }
        proxyActivity?.sendBroadcast(resultIntent)
        proxyActivity?.finish()
    }

    override fun onPause() {
        super.onPause()
        if (proxyActivity?.isFinishing == true && !resultSent) {
            finishWithResult(this.requestCode, false)
        }
    }
}


// 以下是 UI 部分，为保持完整性一并提供 (已包含问题一的修复)

@Composable
private fun AuthorizationScreen(
    request: AuthorizationRequest,
    onResult: (Boolean) -> Unit
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (request.type) {
                    AuthorizationRequest.RequestType.API_PERMISSION -> ApiPermissionUi(request)
                    AuthorizationRequest.RequestType.INSTALL_PERMISSION -> InstallPermissionUi(request)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = { onResult(false) }, modifier = Modifier.weight(1f)) {
                        Text("拒绝")
                    }
                    Button(onClick = { onResult(true) }, modifier = Modifier.weight(1f)) {
                        Text("允许")
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiPermissionUi(request: AuthorizationRequest) {
    val details = request.details
    Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = "Permission",
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "插件权限申请",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "插件 ${request.callingPluginId} 正在申请执行一个敏感操作。",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "操作: ${details[KEY_API_METHOD_NAME]}\n" +
                "目标: ${details[KEY_TARGET_PLUGIN_ID]}\n" +
                "所需权限: ${details[KEY_PERMISSION_LEVEL]}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun InstallPermissionUi(request: AuthorizationRequest) {
    val details = request.details
    val iconUrl = details[KEY_PLUGIN_ICON_URL]

//    val painter = if (!iconUrl.isNullOrBlank()) {
//        rememberAsyncImagePainter(
//            ImageRequest.Builder(LocalContext.current).data(data = iconUrl)
//                .apply(block = fun ImageRequest.Builder.() {
//                    crossfade(true)
//                }).build()
//        )
//    } else {
//        null
//    }

//    if (painter != null) {
//        Image(
//            painter = painter,
//            contentDescription = "Plugin Icon",
//            modifier = Modifier
//                .size(80.dp)
//                .clip(CircleShape),
//            contentScale = ContentScale.Crop
//        )
//    } else {
//        Icon(
//            painter = painterResource(android.R.drawable.sym_def_app_icon),
//            contentDescription = "Installation",
//            modifier = Modifier.size(80.dp),
//            tint = MaterialTheme.colorScheme.primary
//        )
//    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = details[KEY_PLUGIN_NAME] ?: request.callingPluginId,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "您是否要安装此插件？该插件的签名与应用不一致，可能存在安全风险。",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("插件ID: ${request.callingPluginId}", style = MaterialTheme.typography.bodyMedium)
        Text("版本: ${details[KEY_PLUGIN_VERSION] ?: "未知"}", style = MaterialTheme.typography.bodyMedium)
        Text("描述: ${(details[KEY_PLUGIN_DESCRIPTION] ?: "无").ifBlank { "无" }}", style = MaterialTheme.typography.bodyMedium)
        Text("签名指纹 (SHA-256): ...${details[KEY_SIGNATURE_HASH]?.takeLast(8)}", style = MaterialTheme.typography.bodySmall)
    }
}