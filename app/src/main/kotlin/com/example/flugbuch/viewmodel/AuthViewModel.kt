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
        // Erst warten bis die gespeicherte Session geladen ist,
        // sonst wirkt der Nutzer beim App-Start immer ausgeloggt
        runCatching { authRepo.awaitSessionLoaded() }
        if (!authRepo.isLoggedIn) {
            _authState.value = AuthState.LoggedOut
            return
        }
        val profile = runCatching { authRepo.fetchCurrentProfile() }.getOrNull()
            ?: UserProfile(id = authRepo.currentUserId ?: "")
        _authState.value = AuthState.LoggedIn(profile)
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
                // signUpWith mit deaktivierter E-Mail-Bestätigung loggt den Nutzer direkt ein.
                // Falls die Bestätigung aktiv ist, ist der Nutzer nach signUpWith noch nicht eingeloggt.
                if (!authRepo.isLoggedIn) {
                    _authState.value = AuthState.Error(
                        "Bitte bestätige deine E-Mail-Adresse und melde dich dann an."
                    )
                    return@runCatching
                }
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

    fun refreshProfile() {
        viewModelScope.launch {
            val profile = runCatching { authRepo.fetchCurrentProfile() }.getOrNull() ?: return@launch
            _authState.value = AuthState.LoggedIn(profile)
        }
    }

    // Flüge von Supabase laden und lokal cachen
    private suspend fun syncFromSupabase(userId: String) {
        runCatching {
            val flightDao = db.flightDao()
            // Noch nicht hochgeladene lokale Flüge zuerst nach Supabase pushen,
            // damit sie beim Neuaufbau des lokalen Caches nicht verloren gehen
            val failedUploads = flightDao.getUnsyncedFlights()
                .filter { it.userId == userId }
                .filter { supabaseFlightRepo.insertFlight(it.toFlightModel()) == null }
            // Wirft bei Netzwerkfehlern – dann bleibt der lokale Bestand unangetastet
            val remoteFlights = supabaseFlightRepo.getFlights()
            // Lokale Flüge des Nutzers durch den Cloud-Stand ersetzen
            flightDao.deleteAllFlightsByUser(userId)
            remoteFlights.forEach { model ->
                flightDao.insertFlight(FlightEntity.fromFlightModel(model))
            }
            // Flüge, deren Upload fehlgeschlagen ist, lokal behalten
            failedUploads.forEach { flightDao.insertFlight(it.copy(id = 0)) }
        }
    }
}
