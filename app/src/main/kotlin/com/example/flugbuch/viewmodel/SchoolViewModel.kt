package com.example.flugbuch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    fun loadSchoolData(schoolId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                val school = schoolRepo.getSchool(schoolId)
                _schoolName.value = school?.name ?: ""
                _studentsWithFlights.value = schoolRepo.getStudentsWithFlights(schoolId)
            }.onFailure { e ->
                _error.value = "Fehler beim Laden: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun createSchool(name: String, location: String, adminUserId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val school = schoolRepo.createSchool(name, location)
                if (school != null) {
                    schoolRepo.assignSchoolAdmin(adminUserId, school.id)
                    _schoolName.value = school.name
                    onResult(true)
                } else {
                    onResult(false)
                }
            }.onFailure {
                onResult(false)
            }
            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
