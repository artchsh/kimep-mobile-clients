package kz.kimep.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("StudentId") val studentId: String,
    @SerialName("Password") val password: String,
    @SerialName("Token") val token: String = "null",
)

@Serializable
data class LoginResponse(
    @SerialName("id") val id: String,
    @SerialName("ExpiredOn") val expiresOn: String? = null,
)

@Serializable
data class IdRequest(
    @SerialName("id") val id: String,
)

@Serializable
data class PersonalInfo(
    @SerialName("StudentID") val studentId: Int = 0,
    @SerialName("FirstName") val firstName: String? = null,
    @SerialName("LastName") val lastName: String? = null,
    @SerialName("ProgramID") val programId: String? = null,
    @SerialName("XRay") val xRay: String? = null,
) {
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ").trim()
}

@Serializable
data class ClassMeeting(
    @SerialName("ID") val id: Int = 0,
    @SerialName("WeekDay") val weekDay: String = "",
    @SerialName("Semester") val semester: String = "",
    @SerialName("Title") val title: String = "",
    @SerialName("CourseID") val courseId: String? = null,
    @SerialName("Hall") val hall: String? = null,
    @SerialName("Instructor") val instructor: String? = null,
    @SerialName("LDrive") val lDrive: String? = null,
    @SerialName("Section") val section: String? = null,
    @SerialName("Time_From") val timeFrom: String? = null,
    @SerialName("Time_To") val timeTo: String? = null,
    @SerialName("Date_From") val dateFrom: String? = null,
    @SerialName("Date_To") val dateTo: String? = null,
)

@Serializable
data class GpaCredits(
    @SerialName("GPA") val gpa: Double = 0.0,
    @SerialName("CreditsEarned") val creditsEarned: Int = 0,
    @SerialName("CreditsTaken") val creditsTaken: Int = 0,
)

@Serializable
data class FinalGrade(
    @SerialName("ID") val id: Int = 0,
    @SerialName("Semester") val semester: String = "",
    @SerialName("Title") val title: String = "",
    @SerialName("Grade") val grade: String = "",
    @SerialName("Point") val point: Double = 0.0,
)

@Serializable
data class AssessmentScore(
    @SerialName("Semester") val semester: String? = null,
    @SerialName("Code") val code: String? = null,
    @SerialName("TitleCourses") val title: String? = null,
    @SerialName("Registration") val registration: String? = null,
    @SerialName("Score1") val score1: Double? = null,
    @SerialName("Score2") val score2: Double? = null,
    @SerialName("Score3") val score3: Double? = null,
    @SerialName("FinalAssessment") val finalAssessment: String? = null,
)

/**
 * A final-exam entry. The exact shape of /schedule/_finalexams could not be
 * confirmed (it returned an empty array during capture), so every field is
 * optional and unknown keys are ignored.
 */
@Serializable
data class FinalExam(
    @SerialName("ID") val id: Int = 0,
    @SerialName("Semester") val semester: String? = null,
    @SerialName("Code") val code: String? = null,
    @SerialName("CourseID") val courseId: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("Hall") val hall: String? = null,
    @SerialName("Section") val section: String? = null,
    @SerialName("WeekDay") val weekDay: String? = null,
    @SerialName("Date") val date: String? = null,
    @SerialName("Time_From") val timeFrom: String? = null,
    @SerialName("Time_To") val timeTo: String? = null,
)
