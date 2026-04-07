package com.example.flugbuch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flugbuch.data.model.FlightModel
import com.example.flugbuch.data.model.StudentWithFlights
import com.example.flugbuch.data.repository.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SchoolViewModel(application: Application) : AndroidViewModel(application) {

    private val schoolRepo = SchoolRepository()

    private val _studentsWithFlights = MutableStateFlow<List<StudentWithFlights>>(emptyList())
    val studentsWithFlights: StateFlow<List<StudentWithFlights>> = _studentsWithFlights.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _schoolName = MutableStateFlow("")
    val schoolName: StateFlow<String> = _schoolName.asStateFlow()

    private val _inviteCode = MutableStateFlow("")
    val inviteCode: StateFlow<String> = _inviteCode.asStateFlow()

    fun loadSchoolData(schoolId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                val school = schoolRepo.getSchool(schoolId)
                _schoolName.value = school?.name ?: ""
                _inviteCode.value = school?.inviteCode ?: ""
                _studentsWithFlights.value = schoolRepo.getStudentsWithFlights(schoolId)
            }.onFailure { e ->
                _error.value = "Fehler beim Laden: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun createSchool(name: String, location: String, adminUserId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val school = schoolRepo.createSchool(name, location)
                if (school != null) {
                    schoolRepo.assignSchoolAdmin(adminUserId, school.id)
                    _schoolName.value = school.name
                    _inviteCode.value = school.inviteCode
                    onResult(school.inviteCode)
                } else {
                    onResult(null)
                }
            }.onFailure {
                onResult(null)
            }
            _isLoading.value = false
        }
    }

    // Flug eines Schülers akzeptieren/ablehnen (Instructor)
    fun approveStudentFlight(flightId: String, approved: Boolean, schoolId: String) {
        viewModelScope.launch {
            runCatching {
                schoolRepo.approveStudentFlight(flightId, approved)
                // Lokale Liste aktualisieren
                _studentsWithFlights.value = _studentsWithFlights.value.map { swf ->
                    swf.copy(
                        flights = swf.flights.map { f ->
                            if (f.id == flightId) f.copy(instructorApproved = approved) else f
                        }
                    )
                }
            }
        }
    }

    // Flug eines Schülers bearbeiten (School-Admin)
    fun updateStudentFlight(flight: FlightModel, schoolId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                schoolRepo.updateStudentFlight(flight)
                // Lokale Liste aktualisieren
                _studentsWithFlights.value = _studentsWithFlights.value.map { swf ->
                    swf.copy(
                        flights = swf.flights.map { f ->
                            if (f.id == flight.id) flight else f
                        }
                    )
                }
            }.onFailure { e ->
                _error.value = "Fehler beim Speichern: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
