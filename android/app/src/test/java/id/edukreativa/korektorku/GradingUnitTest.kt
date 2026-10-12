package id.edukreativa.korektorku

import id.edukreativa.korektorku.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class GradingUnitTest {
    private val questions = listOf(
        Question("PG-01",Kind.PG,1,"B",BigDecimal("2")),
        Question("IS-01",Kind.ISIAN,1,"4|empat",BigDecimal("2")),
        Question("UR-01",Kind.URAIAN,1,"",BigDecimal("6"))
    )
    private val answers = mapOf(
        "PG-01" to Response("PG-01","B",ReadState.VERIFIED,reviewed=true),
        "IS-01" to Response("IS-01","empat",ReadState.VERIFIED,reviewed=true),
        "UR-01" to Response("UR-01","",ReadState.VERIFIED,BigDecimal.ZERO,reviewed=true)
    )
    private fun result(r:Map<String,Response> = answers, photo:String="", reviewed:Boolean=false):Outcome =
        Grading.grade(Settings(),questions,Submission(Student("01","Siswa Tes"),r,true,photo,photoReviewed=reviewed))

    @Test fun fullReviewGivesFinalAndZeroEssayIsValid() {
        val out=result()
        assertEquals("FINAL",out.status)
        assertEquals(0,out.score!!.compareTo(BigDecimal("40.00")))
    }
    @Test fun scanPhotoBlocksFinalBeforeTeacherConfirms() {
        assertNull(result(photo="/data/photo.jpg").score)
        assertTrue("FOTO_SCAN" in result(photo="/data/photo.jpg").unresolved)
        assertEquals("FINAL",result(photo="/data/photo.jpg",reviewed=true).status)
    }
    @Test fun recognizedPgMustBeReviewedBeforeFinal() {
        val pending=answers.toMutableMap()
        pending["PG-01"]=pending.getValue("PG-01").copy(state=ReadState.RECOGNIZED,reviewed=false)
        assertNull(result(pending).score)
    }
    @Test fun unansweredEssayBlocksFinal() {
        val pending=answers.toMutableMap()
        pending["UR-01"]=pending.getValue("UR-01").copy(state=ReadState.RUBRIC_PENDING,teacherPoints=null)
        assertNull(result(pending).score)
    }
    @Test(expected=IllegalArgumentException::class) fun essayCannotExceedRubricMaximum() {
        val invalid=answers.toMutableMap()
        invalid["UR-01"]=invalid.getValue("UR-01").copy(teacherPoints=BigDecimal("7"))
        result(invalid)
    }
    @Test fun staleKeyCannotBeFinalized() {
        val out=Grading.grade(Settings(keyVersion=10),questions,Submission(Student("01","Siswa Tes"),answers,true))
        assertNull(out.score)
        assertEquals("VERSI BERBEDA",out.status)
    }
    @Test fun maliciousFormulaIsRejected() {
        try { Expression("Runtime.exec(1)",emptyMap()).eval();fail("Should reject unknown identifiers") }
        catch(e: IllegalArgumentException) { }
    }
    @Test fun partialEssayAndCustomFormulaWork() {
        val r=answers.toMutableMap()
        r["UR-01"]=r.getValue("UR-01").copy(teacherPoints=BigDecimal("3"))
        val out=Grading.grade(Settings(mode=ScoreMode.CUSTOM_EXPR,custom="ROUND(PG_PERSEN*0.5+IS_PERSEN*0.2+UR_PERSEN*0.3,2)"),questions,Submission(Student("01","Siswa Tes"),r,true))
        assertEquals(0,out.score!!.compareTo(BigDecimal("85.00")))
    }
}
