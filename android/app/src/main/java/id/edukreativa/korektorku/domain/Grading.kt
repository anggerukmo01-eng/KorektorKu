package id.edukreativa.korektorku.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer

object Grading {
    private val hundred=BigDecimal("100")
    fun bd(s: String): BigDecimal = BigDecimal(s.trim().replace(',','.'))
    fun normalize(s: String): String = Normalizer.normalize(s,Normalizer.Form.NFC)
        .trim().replace(Regex("\\s+")," ").uppercase()
    fun matches(key: String, answer: String): Boolean = key.split('|').any { normalize(it) == normalize(answer) }

    fun grade(settings: Settings, questions: List<Question>, submission: Submission): Outcome {
        val active=questions.filter { it.active }
        require(active.isNotEmpty()) { "Minimal satu soal aktif" }
        require(active.map { it.id }.distinct().size==active.size) { "ID soal ganda" }
        require(active.all { it.maxPoints > BigDecimal.ZERO }) { "Bobot harus positif" }
        require(active.filter { it.kind!=Kind.URAIAN }.all { it.key.trim().isNotEmpty() }) { "Kunci belum lengkap" }
        val earned=mutableMapOf(Kind.PG to BigDecimal.ZERO, Kind.ISIAN to BigDecimal.ZERO, Kind.URAIAN to BigDecimal.ZERO)
        val maximum=mutableMapOf(Kind.PG to BigDecimal.ZERO, Kind.ISIAN to BigDecimal.ZERO, Kind.URAIAN to BigDecimal.ZERO)
        var correct=0; var wrong=0; var blanks=0
        val review=mutableListOf<String>()
        for(q in active) {
            maximum[q.kind]=maximum.getValue(q.kind).add(q.maxPoints)
            val r=submission.responses[q.id]
            if(r==null || r.state in listOf(ReadState.NOT_CAPTURED,ReadState.UNCERTAIN,ReadState.MULTIPLE,ReadState.RUBRIC_PENDING)) {
                review.add(q.id); continue
            }
            if(!r.reviewed) { review.add(q.id); continue }
            if(r.state==ReadState.BLANK_CONFIRMED) { blanks++; continue }
            if(r.state !in listOf(ReadState.RECOGNIZED,ReadState.VERIFIED)) { review.add(q.id); continue }
            if(q.kind==Kind.URAIAN && r.teacherPoints==null) { review.add(q.id); continue }
            val points=when(q.kind) {
                Kind.PG -> { if(matches(q.key,r.answer)) {correct++; q.maxPoints} else {wrong++; BigDecimal.ZERO} }
                Kind.ISIAN -> r.teacherPoints ?: (if(matches(q.key,r.answer)) q.maxPoints else BigDecimal.ZERO)
                Kind.URAIAN -> r.teacherPoints!!
            }
            require(points>=BigDecimal.ZERO && points<=q.maxPoints) { "Poin ${q.id} di luar bobot" }
            earned[q.kind]=earned.getValue(q.kind).add(points)
        }
        val p=Kind.PG; val s=Kind.ISIAN; val u=Kind.URAIAN
        val pg=earned.getValue(p); val ins=earned.getValue(s); val ur=earned.getValue(u)
        val mp=maximum.getValue(p); val ms=maximum.getValue(s); val mu=maximum.getValue(u)
        val total=pg+ins+ur; val max=mp+ms+mu
        val pct={a:BigDecimal,b:BigDecimal -> if(b.compareTo(BigDecimal.ZERO)==0) BigDecimal.ZERO else a.multiply(hundred).divide(b,12,RoundingMode.HALF_UP)}
        val rp=pct(pg,mp); val rs=pct(ins,ms); val ru=pct(ur,mu)
        val shares=settings.proportion
        if(settings.mode==ScoreMode.PROPORSI) {
            require(shares.size==3 && shares.all { it>=BigDecimal.ZERO } && shares.fold(BigDecimal.ZERO) { a,b -> a+b }.compareTo(BigDecimal.ONE)==0) { "Proporsi harus berjumlah 1" }
            require((mp>BigDecimal.ZERO || shares[0]==BigDecimal.ZERO) && (ms>BigDecimal.ZERO || shares[1]==BigDecimal.ZERO) && (mu>BigDecimal.ZERO || shares[2]==BigDecimal.ZERO)) { "Proporsi jenis soal kosong harus nol" }
        }
        val c=settings.coefficients
        if(settings.mode==ScoreMode.FORMULA_MANUAL) require(c.size==3 && c.all { it>=BigDecimal.ZERO }) { "Koefisien tidak valid" }
        val variableMap=mapOf("PG_POIN" to pg,"IS_POIN" to ins,"UR_POIN" to ur,
            "PG_MAKS" to mp,"IS_MAKS" to ms,"UR_MAKS" to mu,
            "PG_PERSEN" to rp,"IS_PERSEN" to rs,"UR_PERSEN" to ru,
            "BENAR_PG" to BigDecimal(correct),"SALAH_PG" to BigDecimal(wrong),"KOSONG_PG" to BigDecimal(blanks),
            "BONUS" to settings.bonus,"PENGURANG" to settings.deduction,"KKM" to (settings.kkm?:BigDecimal.ZERO))
        val value=when(settings.mode) {
            ScoreMode.POIN -> total.multiply(hundred).divide(max,12,RoundingMode.HALF_UP)
            ScoreMode.PROPORSI -> shares[0]*rp+shares[1]*rs+shares[2]*ru
            ScoreMode.FORMULA_MANUAL -> c[0]*rp+c[1]*rs+c[2]*ru+settings.bonus-settings.deduction
            ScoreMode.CUSTOM_EXPR -> Expression(settings.custom,variableMap).eval()
        }.max(BigDecimal.ZERO).min(hundred).setScale(2,RoundingMode.HALF_UP)
        if(submission.photoPath.isNotBlank() && !submission.photoReviewed) review.add("FOTO_SCAN")
        val ready=review.isEmpty() && submission.verified && submission.keyVersion==settings.keyVersion && submission.formulaVersion==settings.formulaVersion
        val status=when {
            review.isNotEmpty() -> "PERLU REVIEW"
            submission.keyVersion!=settings.keyVersion || submission.formulaVersion!=settings.formulaVersion -> "VERSI BERBEDA"
            !submission.verified -> "DRAF"
            else -> "FINAL"
        }
        return Outcome(status,if(ready) value else null,if(ready) settings.kkm?.let { value>=it } else null,
            total,max,correct,wrong,blanks,review,pg,ins,ur,mp,ms,mu)
    }
}
