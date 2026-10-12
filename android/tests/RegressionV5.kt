import id.edukreativa.korektorku.domain.*
import java.math.BigDecimal

fun main() {
    val points=BigDecimal("2")
    val qs=listOf(Question("PG-01",Kind.PG,1,"B",points), Question("IS-01",Kind.ISIAN,1,"4|empat",points), Question("UR-01",Kind.URAIAN,1,"",BigDecimal("6")))
    val r=mapOf(
        "PG-01" to Response("PG-01","B",ReadState.RECOGNIZED,reviewed=false),
        "IS-01" to Response("IS-01","Empat",ReadState.VERIFIED,reviewed=true),
        "UR-01" to Response("UR-01","",ReadState.VERIFIED,teacherPoints=BigDecimal.ZERO,reviewed=true)
    )
    val config=Settings(mode=ScoreMode.POIN)
    fun output(resp:Map<String,Response>,photo:String="",checked:Boolean=false)=Grading.grade(config,qs,Submission(Student("101","Uji"),resp,true,photo,photoReviewed=checked))
    val unreviewed=output(r)
    check(unreviewed.status=="PERLU REVIEW" && unreviewed.score==null && "PG-01" in unreviewed.unresolved)
    val reviewed=r.toMutableMap().apply { put("PG-01",getValue("PG-01").copy(reviewed=true)) }
    check(output(reviewed).score==BigDecimal("40.00"))
    val photoUnreviewed=output(reviewed,"/app/photo.jpg",false)
    check(photoUnreviewed.status=="PERLU REVIEW" && photoUnreviewed.score==null && "FOTO_SCAN" in photoUnreviewed.unresolved)
    check(output(reviewed,"/app/photo.jpg",true).score==BigDecimal("40.00"))
    val unfinished=reviewed.toMutableMap().apply { put("IS-01",getValue("IS-01").copy(state=ReadState.NOT_CAPTURED)) }
    check(output(unfinished).score==null)
    val black=reviewed.toMutableMap().apply{ put("UR-01",getValue("UR-01").copy(state=ReadState.BLANK_CONFIRMED,reviewed=true)) }
    check(output(black).score==BigDecimal("40.00"))
    val bad=reviewed.toMutableMap().apply{put("UR-01",getValue("UR-01").copy(teacherPoints=BigDecimal("8")))}
    check(runCatching { output(bad) }.isFailure)
    check(Grading.grade(config.copy(keyVersion=2),qs,Submission(Student("101","Uji"),reviewed,true)).score==null)
    val pgOnly=listOf(Question("PG-01",Kind.PG,1,"A",BigDecimal.ONE))
    val one=Submission(Student("001","Test"),mapOf("PG-01" to Response("PG-01","A",ReadState.VERIFIED,reviewed=true)),true)
    check(Grading.grade(Settings(),pgOnly,one).score==BigDecimal("100.00"))
    check(runCatching { Grading.grade(Settings(mode=ScoreMode.PROPORSI),pgOnly,one) }.isFailure)
    check(Grading.grade(Settings(mode=ScoreMode.PROPORSI,proportion=listOf(BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO)),pgOnly,one).score==BigDecimal("100.00"))
    println("OK: v5 regression final gating, all categories, zero, stale key, scan review, score bounds")
}
