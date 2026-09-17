package kz.kimep.mobile.data

import kz.kimep.mobile.data.model.AssessmentScore
import kz.kimep.mobile.data.model.ClassMeeting
import kz.kimep.mobile.data.model.FinalExam
import kz.kimep.mobile.data.model.FinalGrade
import kz.kimep.mobile.data.model.GpaCredits
import kz.kimep.mobile.data.model.PersonalInfo

class KimepRepository(
    private val api: KimepApi,
    private val store: SessionStore,
) {

    suspend fun login(studentId: String, password: String): Result<Unit> = runCatching {
        val trimmed = studentId.trim()
        val response = api.login(trimmed, password)
        val expiresOnMs = ApiDate.instant(response.expiresOn)?.toEpochMilli()
        val info = runCatching { api.personalInfo(response.id) }.getOrNull()
        store.save(trimmed, response.id, expiresOnMs, info)
    }

    suspend fun refreshProfile(id: String): Result<PersonalInfo> = runCatching {
        val info = api.personalInfo(id)
        store.updateProfile(info)
        info
    }

    suspend fun schedule(id: String): Result<List<ClassMeeting>> =
        runCatching { api.schedule(id) }

    suspend fun gpa(id: String): Result<GpaCredits> =
        runCatching { api.gpa(id) }

    suspend fun finalGrades(id: String): Result<List<FinalGrade>> =
        runCatching { api.finalGrades(id) }

    suspend fun assessmentScores(id: String): Result<List<AssessmentScore>> =
        runCatching { api.assessmentScores(id) }

    suspend fun finalExams(id: String): Result<List<FinalExam>> =
        runCatching { api.finalExams(id) }

    suspend fun logout() = store.clear()
}
