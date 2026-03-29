package com.example.flugbuch.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole { STUDENT, INSTRUCTOR, SCHOOL_ADMIN }

@Serializable
data class UserProfile(
    val id: String = "",
    @SerialName("full_name")      val fullName: String = "",
    @SerialName("license_number") val licenseNumber: String = "",
    val role: String = UserRole.STUDENT.name,
    @SerialName("school_id")      val schoolId: String? = null,
    @SerialName("created_at")     val createdAt: String? = null
) {
    val userRole: UserRole get() = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.STUDENT)
    val isSchoolStaff: Boolean get() = userRole == UserRole.INSTRUCTOR || userRole == UserRole.SCHOOL_ADMIN
}
