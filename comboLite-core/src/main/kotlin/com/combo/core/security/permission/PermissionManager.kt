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

import android.app.Application
import com.combo.core.model.PluginInfo
import com.combo.core.runtime.PluginManager
import com.combo.core.security.signature.SignatureValidator
import timber.log.Timber

/**
 * 插件权限管理器
 *
 * 负责校验插件是否有权限执行敏感操作。
 * 它是框架安全体系的核心，所有对敏感API的调用都必须先通过此管理器的检查。
 *
 * @property context Application 上下文
 */
class PermissionManager(private val context: Application) {

    // 宿主签名摘要的懒加载缓存
    private val hostSignatureDigests: Set<String> by lazy {
        SignatureValidator.getHostSignatures(context).also {
            if (it.isEmpty()) {
                throw IllegalStateException("无法获取宿主签名，权限系统初始化失败。")
            }
        }
    }

    /**
     * 核心权限检查方法。
     *
     * @param callingPluginId 发起调用的插件ID。
     * @param requiredLevel 此操作所要求的最低权限等级。
     * @param targetPluginId (可选) 操作的目标插件ID，用于 SELF 权限检查。
     * @return `true` 如果权限检查通过，否则 `false`。
     */
    fun checkPermission(
        callingPluginId: String,
        requiredLevel: PermissionLevel,
        targetPluginId: String? = null
    ): Boolean {
        Timber.d("权限检查: 调用方[$callingPluginId], 要求[$requiredLevel], 目标[$targetPluginId]")

        // 获取调用方插件的信息
        val callingPluginInfo = PluginManager.getPluginInfo(callingPluginId)?.pluginInfo
        if (callingPluginInfo == null) {
            Timber.e("权限拒绝: 无法找到调用方插件信息 [$callingPluginId]")
            return false
        }

        // 根据要求的权限等级进行检查
        return when (requiredLevel) {
            PermissionLevel.NONE -> true // 无需权限，直接通过
            PermissionLevel.SELF -> {
                if (targetPluginId == null) {
                    Timber.w("权限检查警告: SELF 权限需要明确的 targetPluginId")
                    false
                } else {
                    callingPluginId == targetPluginId
                }
            }
            PermissionLevel.HOST -> hasHostSignature(callingPluginInfo)
            PermissionLevel.SIGNATURE -> hasSignature(callingPluginInfo) // 调用新增的检查
            PermissionLevel.USER_GRANTABLE -> {
                // USER_GRANTABLE 逻辑比较特殊，不在此处直接检查。
                // 而是由调用方（如PluginManager）捕获此等级，然后触发UI回调。
                // 如果宿主未实现回调或用户拒绝，则操作失败。
                true // 此处返回true，表示检查已委托给上层处理
            }
        }
    }

    /**
     * 检查插件签名是否与宿主完全一致。
     * 规则：插件的签名集合必须是宿主签名集合的子集。
     */
    private fun hasHostSignature(pluginInfo: PluginInfo): Boolean {
        val pluginSignatureDigests = SignatureValidator.getPluginSignatures(context, pluginInfo.path)
        if (pluginSignatureDigests.isEmpty()) {
            Timber.w("无法获取插件 [${pluginInfo.pluginId}] 的签名，权限检查失败。")
            return false
        }
        return hostSignatureDigests.containsAll(pluginSignatureDigests)
    }

    /**
     * 检查插件签名是否与宿主签名链匹配（更宽松）。
     * 在当前实现中，此逻辑等同于 `hasHostSignature`。
     * 未来可扩展为更复杂的逻辑，例如校验签名链中的根证书等。
     */
    private fun hasSignature(pluginInfo: PluginInfo): Boolean {
        // 简化处理，可扩展
        return hasHostSignature(pluginInfo)
    }
}