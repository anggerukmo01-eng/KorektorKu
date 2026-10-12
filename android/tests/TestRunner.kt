import id.edukreativa.korektorku.domain.*
import id.edukreativa.korektorku.export.XlsxExporter
import java.math.BigDecimal
import java.io.File

fun assertScore(actual:BigDecimal?, expected:String){check(actual?.compareTo(BigDecimal(expected))==0){"Expected $expected, actual $actual"}}
fun main(args:Array<String>) {
    val q=mutableListOf<Question>()
    val keys=listOf("A","B","C","D")
    for(i in 1..20)q.add(Question("PG-%02d".format(i),Kind.PG,i,keys[(i-1)%4],BigDecimal.ONE))
    for(i in 1..10)q.add(Question("IS-%02d".format(i),Kind.ISIAN,i,"${i}|${i}",BigDecimal("2")))
    for(i in 1..5)q.add(Question("UR-%02d".format(i),Kind.URAIAN,i,"",BigDecimal("5")))
    fun pg(i:Int,token:String):Response=Response("PG-%02d".format(i),token,if(token=="-") ReadState.BLANK_CONFIRMED else if(token=="?") ReadState.UNCERTAIN else ReadState.VERIFIED,reviewed=true)
    val a=mutableMapOf<String,Response>()
    for(i in 1..20) {val v=when(i){in 1..16->keys[(i-1)%4];in 17..19->"E";else->"-"};a[pg(i,v).questionId]=pg(i,v)}
    for(i in 1..10){val id="IS-%02d".format(i);val token=when(i){in 1..8->"$i";9->"salah";else->"-"}
        a[id]=Response(id,token,if(token=="-")ReadState.BLANK_CONFIRMED else ReadState.VERIFIED,reviewed=true)}
    listOf("4","5","5","3","5").forEachIndexed{i,v->val id="UR-%02d".format(i+1);a[id]=Response(id,"",ReadState.VERIFIED,BigDecimal(v),true)}
    val s=Settings("SD Negeri 1 Gerduren","1A","Matematika","Penilaian Harian")
    val student=Student("001","Siswa Tes")
    val sub=Submission(student,a,true)
    val out=Grading.grade(s,q,sub)
    assertScore(out.score,"83.08")
    check(out.correctPg==16 && out.wrongPg==3 && out.blank==2 && out.status=="FINAL")
    check(out.passed==true)
    val proportion=Grading.grade(s.copy(mode=ScoreMode.PROPORSI),q,sub)
    assertScore(proportion.score,"82.40")
    val linear=Grading.grade(s.copy(mode=ScoreMode.FORMULA_MANUAL),q,sub)
    assertScore(linear.score,"82.40")
    val formula=Grading.grade(s.copy(mode=ScoreMode.CUSTOM_EXPR,custom="ROUND(CLAMP(PG_PERSEN*0.5+IS_PERSEN*0.2+UR_PERSEN*0.3+BONUS-PENGURANG,0,100),2)"),q,sub)
    assertScore(formula.score,"82.40")
    val altered=a.toMutableMap();altered["PG-01"]=pg(1,"?")
    val hold=Grading.grade(s,q,sub.copy(student=Student("002","=HYPERLINK(\"evil\")"),responses=altered))
    check(hold.status=="PERLU REVIEW" && hold.score==null && hold.unresolved==listOf("PG-01"))
    val notYet=Grading.grade(s,q,sub.copy(verified=false));check(notYet.score==null && notYet.status=="DRAF")
    val zero=q.associate{it.id to Response(it.id,"",ReadState.BLANK_CONFIRMED,reviewed=true)}
    assertScore(Grading.grade(s,q,sub.copy(responses=zero)).score,"0.00")
    val scoreZeroEssay=a.toMutableMap();scoreZeroEssay["UR-01"]=Response("UR-01","",ReadState.VERIFIED,BigDecimal.ZERO,true)
    check(Grading.grade(s,q,sub.copy(responses=scoreZeroEssay)).status=="FINAL")
    val stale=Grading.grade(s.copy(keyVersion=2),q,sub);check(stale.status=="VERSI BERBEDA" && stale.score==null)
    val c=mapOf("X" to BigDecimal("3"))
    check(Expression("MIN(4,MAX(2,X*3))",c).eval()==BigDecimal("4"))
    listOf("X/0","Runtime.exec(1)","1+", "UNKNOWN", "ROUND(2,8)","2**4").forEach{exp->
        check(runCatching{Expression(exp,c).eval()}.isFailure){"Dangerous expression accepted: $exp"}
    }
    check(runCatching{Grading.grade(s.copy(mode=ScoreMode.PROPORSI,proportion=listOf(BigDecimal("0.3"),BigDecimal("0.3"),BigDecimal("0.3"))),q,sub)}.isFailure)
    check(runCatching{Grading.grade(s,q,sub.copy(responses=a.toMutableMap().also{it["UR-01"]=Response("UR-01","",ReadState.VERIFIED,BigDecimal("100"),true)}))}.isFailure)
    val saved=File(args.firstOrNull()?:"/mnt/data/KorektorKu_Demo_Export_V3.xlsx")
    saved.outputStream().use { XlsxExporter.write(it,s,q,listOf(sub,sub.copy(student=Student("002","=HYPERLINK(\"evil\")"),responses=altered))) }
    println("OK: 14 checks; nilai 83.08 / 82.40; review ditahan; custom expression aman; zero sah; file=${saved.absolutePath} (${saved.length()} bytes)")
}
