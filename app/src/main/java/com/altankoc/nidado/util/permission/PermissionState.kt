package com.altankoc.nidado.util.permission

sealed class PermissionState {
    data object Granted : PermissionState()
    data object Denied : PermissionState()
    data object PermanentlyDenied : PermissionState()
    data object NotRequested : PermissionState()
}