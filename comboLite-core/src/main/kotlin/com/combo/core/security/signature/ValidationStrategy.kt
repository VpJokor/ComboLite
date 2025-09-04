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

package com.combo.core.security.signature


/**
 * 定义插件安装时的签名校验策略。
 */
sealed class ValidationStrategy {
    /**
     * **严格模式**: 只允许与宿主签名完全一致的插件。
     * 这是最安全、也是默认的策略。
     */
    object Strict : ValidationStrategy()

    /**
     * **白名单模式**: 允许宿主签名或白名单中指定的签名摘要。
     * @param trustedSignatures 一组受信任的签名SHA-256哈希值。
     */
    data class Whitelist(val trustedSignatures: Set<String>) : ValidationStrategy()

    /**
     * **用户授权模式**: 当遇到未知签名时，通过回调请求用户授权。
     * @param onAuthorizationRequest 一个回调函数，当需要用户授权时被调用。
     * 参数: pluginId, pluginSignatureSha256
     * 返回值: `true` 表示用户同意, `false` 表示拒绝。
     */
    class UserGrant(
        val onAuthorizationRequest: (pluginId: String, pluginSignature: String) -> Boolean
    ) : ValidationStrategy()

    /**
     * **不安全模式**: 完全禁用签名校验。
     * **警告**: 此模式仅应用于内部调试，绝不能在生产环境中使用！
     */
    object Insecure : ValidationStrategy()
}