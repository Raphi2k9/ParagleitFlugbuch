package com.example.flugbuch.auth

import com.example.flugbuch.data.model.UserProfile

sealed class AuthState {
    data object Loading   : AuthState()
    data object LoggedOut : AuthState()
    data class  LoggedIn(val profile: UserProfile) : AuthState()
    data class  Error(val message: String) : AuthState()
}
