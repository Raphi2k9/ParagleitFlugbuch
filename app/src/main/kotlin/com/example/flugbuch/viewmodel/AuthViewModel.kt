package com.example.flugbuch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flugbuch.auth.AuthRepository
import com.example.flugbuch.auth.AuthState
import com.example.flugbuch.data.database.FlightDatabase
import com.example.flugbuch.data.entities.FlightEntity
import com.example.flugbuch.data.model.UserProfile
import com.example.flugbuch.data.repository.SupabaseFlightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()
    private val supabaseFlightRepo = SupabaseFlightRepository()
    private val db = FlightDatabase.getDatabase(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Beim Start prüfen ob bereits eingeloggt
        viewModelScope.launch {
            checkSession()
        }
    }

    private suspend fun checkSession() {
        _authState.value = AuthState.Loading
        val profile = runCatching { authRepo.fetchCurrentProfile() }.getOrNull()
        _authState.value = if (profile != null) {
            AuthState.LoggedIn(profile)
        } else {
            AuthState.LoggedOut
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            runCatching {
                authRepo.login(email, password)
                val profile = authRepo.fetchCurrentProfile()
                    ?: UserProfile(id = authRepo.currentUserId ?: "")
                // Flüge aus Supabase in lokale Datenbank synchronisieren
                syncFromSupabase(profile.id)
                _authState.value = AuthState.LoggedIn(profile)
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Login fehlgeschlagen")
            }
            _isLoading.value = false
        }
    }

    fun register(
        email: String,
        password: String,
        fullName: String,
        licenseNumber: String,
        role: String = "STUDENT"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            runCatching {
                authRepo.register(email, password, fullName, licenseNumber, role)
                // Nach Registrierung direkt einloggen
                authRepo.login(email, password)
                val profile = authRepo.fetchCurrentProfile()
                    ?: UserProfile(id = authRepo.currentUserId ?: "", fullName = fullName,
                                   licenseNumber = licenseNumber, role = role)
                _authState.value = AuthState.LoggedIn(profile)
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Registrierung fehlgeschlagen")
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val userId = authRepo.currentUserId
                authRepo.logout()
                // Lokale Flüge des Nutzers aus Room löschen (Datenschutz)
                if (userId != null) {
                    db.flightDao().deleteAllFlightsByUser(userId)
                }
            }
            _authState.value = AuthState.LoggedOut
            _isLoading.value = false
        }
    }

    fun joinSchool(inviteCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching { authRepo.joinSchool(inviteCode) }.getOrDefault(false)
            if (success) {
                // Profil neu laden damit schoolId aktualisiert ist
                val profile = authRepo.fetchCurrentProfile()
                if (profile != null) _authState.value = AuthState.LoggedIn(profile)
            }
            onResult(success)
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.LoggedOut
        }
    }

    // Flüge von Supabase laden und lokal cachen
    private suspend fun syncFromSupabase(userId: String) {
        runCatching {
            val remoteFlights = supabaseFlightRepo.getFlights()
            val flightDao = db.flightDao()
            // Alle bestehenden lokalen Flüge des Nutzers löschen, dann neu einfügen
            flightDao.deleteAllFlightsByUser(userId)
            remoteFlights.forEach { model ->
                flightDao.insertFlight(FlightEntity.fromFlightModel(model))
            }
        }
    }
}
