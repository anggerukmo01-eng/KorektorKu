package id.edukreativa.korektorku.domain

import java.math.BigDecimal

enum class Kind { PG, ISIAN, URAIAN }
enum class ReadState { NOT_CAPTURED, RECOGNIZED, BLANK_CONFIRMED, MULTIPLE, UNCERTAIN, VERIFIED, RUBRIC_PENDING }
enum class ScoreMode { POIN, PROPORSI, FORMULA_MANUAL, CUSTOM_EXPR }

data class Question(
    val id: String, val kind: Kind, val number: Int,
    val key: String, val maxPoints: BigDecimal,
    val active: Boolean = true
)
data class Response(
    val questionId: String, val answer: String = "",
    val state: ReadState = ReadState.NOT_CAPTURED,
    val teacherPoints: BigDecimal? = null,
    val reviewed: Boolean = false
)
data class Settings(
    val school: String = "", val gradeClass: String = "", val subject: String = "", val title: String = "",
    val kkm: BigDecimal? = BigDecimal("75"),
    val mode: ScoreMode = ScoreMode.POIN,
    val proportion: List<BigDecimal> = listOf(BigDecimal("0.4"),BigDecimal("0.3"),BigDecimal("0.3")),
    val coefficients: List<BigDecimal> = listOf(BigDecimal("0.5"),BigDecimal("0.2"),BigDecimal("0.3")),
    val bonus: BigDecimal = BigDecimal.ZERO, val deduction: BigDecimal = BigDecimal.ZERO,
    val custom: String = "CLAMP(0.5*PG_PERSEN+0.2*IS_PERSEN+0.3*UR_PERSEN+BONUS-PENGURANG,0,100)",
    val keyVersion: Int = 1, val formulaVersion: Int = 1
)
data class Student(val code: String, val name: String)
data class Submission(
    val student: Student, val responses: Map<String, Response>,
    val verified: Boolean = false, val photoPath: String = "",
    val keyVersion: Int = 1, val formulaVersion: Int = 1,
    val photoReviewed: Boolean = false
)
data class Outcome(
    val status: String, val score: BigDecimal?, val passed: Boolean?,
    val earned: BigDecimal, val maximum: BigDecimal,
    val correctPg: Int, val wrongPg: Int, val blank: Int,
    val unresolved: List<String>, val pgEarned: BigDecimal, val isEarned: BigDecimal,
    val urEarned: BigDecimal, val pgMax: BigDecimal, val isMax: BigDecimal, val urMax: BigDecimal
)
