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

package com.combo.core.runtime


import com.combo.core.security.permission.PermissionLevel
import com.combo.core.security.permission.RequiresPermission
import java.lang.reflect.Method

/**
 * 通过注解获取方法所需的权限等级。
 */
fun Method.getRequiredPermission(): PermissionLevel? {
    val annotation = this.getAnnotation(RequiresPermission::class.java)
    return annotation?.level
}

/**
 * 权限代理执行函数。
 *
 * @param callingPluginId 发起调用的插件ID。
 * @param targetPluginId (可选) 操作的目标插件ID。
 * @param method 被调用的方法（通过反射获取）。
 * @param action 需要执行的业务逻辑。
 */
inline fun <T> runWithPermissionCheck(
    callingPluginId: String,
    targetPluginId: String? = null,
    method: Method,
    action: () -> T
): T {
    val requiredLevel = method.getRequiredPermission()
        ?: return action()

    if (PluginManager.permissionManager.checkPermission(callingPluginId, requiredLevel, targetPluginId)) {
        if (requiredLevel == PermissionLevel.USER_GRANTABLE) {
            val userApproved = true
            if (!userApproved) {
                throw SecurityException("用户拒绝了权限请求: ${method.name}")
            }
        }
        return action()
    } else {
        throw SecurityException("插件 [$callingPluginId] 没有权限 [${requiredLevel.name}] 调用方法 [${method.name}]")
    }
}