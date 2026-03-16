package com.example.opensonggoogletvviewer.model

sealed class ConnectionState {
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data object Idle : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
