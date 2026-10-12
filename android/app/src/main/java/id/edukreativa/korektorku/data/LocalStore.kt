package id.edukreativa.korektorku.data

import android.content.Context
import id.edukreativa.korektorku.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** Offline prototype persistence. JSON is private to the app; migrate to Room before production. */
class LocalStore(context: Context) {
    private val prefs=context.getSharedPreferences("korektorku_v3",Context.MODE_PRIVATE)
    var settings=Settings()
    var questions=mutableListOf<Question>()
    val submissions=mutableMapOf<String,Submission>()
    var restoredFromBackup=false
        private set

    init { load() }
    private fun JSONObject.optDecimal(key: String): BigDecimal?=optString(key,"").takeIf { it.isNotEmpty() }?.let(::BigDecimal)
    private fun JSONObject.decimalList(key: String): List<BigDecimal> = getJSONArray(key).let {a -> (0 until a.length()).map { BigDecimal(a.getString(it)) } }
    private fun Array<BigDecimal>.json(): JSONArray=JSONArray().also {arr -> forEach { arr.put(it.toPlainString()) } }
    fun save() {
        val root=JSONObject()
        val s=settings
        root.put("settings", JSONObject().apply {
            put("school",s.school);put("gradeClass",s.gradeClass);put("subject",s.subject);put("title",s.title)
            put("kkm",s.kkm?.toPlainString() ?: ""); put("mode",s.mode.name)
            put("proportion",s.proportion.toTypedArray().json()); put("coefficients",s.coefficients.toTypedArray().json())
            put("bonus",s.bonus.toPlainString());put("deduction",s.deduction.toPlainString())
            put("custom",s.custom);put("keyVersion",s.keyVersion);put("formulaVersion",s.formulaVersion)
        })
        root.put("questions",JSONArray().also { a -> questions.forEach {q -> a.put(JSONObject().apply {
            put("id",q.id);put("kind",q.kind.name);put("number",q.number);put("key",q.key)
            put("maxPoints",q.maxPoints.toPlainString());put("active",q.active)
        })} })
        root.put("submissions",JSONArray().also { a -> submissions.values.forEach {sub -> a.put(JSONObject().apply {
            put("code",sub.student.code);put("name",sub.student.name);put("verified",sub.verified)
            put("photoPath",sub.photoPath);put("photoReviewed",sub.photoReviewed);put("keyVersion",sub.keyVersion);put("formulaVersion",sub.formulaVersion)
            put("responses",JSONArray().also { b -> sub.responses.values.forEach {r -> b.put(JSONObject().apply {
                put("questionId",r.questionId);put("answer",r.answer);put("state",r.state.name)
                put("teacherPoints",r.teacherPoints?.toPlainString() ?: "");put("reviewed",r.reviewed)
            }) } })
        }) } })
        val previous=prefs.getString(if(restoredFromBackup) "snapshot_backup" else "snapshot",null)
        val edit=prefs.edit().putString("snapshot",root.toString())
        if(previous!=null) edit.putString("snapshot_backup",previous)
        require(edit.commit()) { "Penyimpanan gagal. Periksa ruang perangkat sebelum lanjut." }
        restoredFromBackup=false
    }
    private fun load() {
        val raw=prefs.getString("snapshot",null) ?: return
        try {
            loadSnapshot(raw)
        } catch(primary:Exception) {
            settings=Settings();questions.clear();submissions.clear()
            val backup=prefs.getString("snapshot_backup",null)
            if(backup!=null) {
                try { loadSnapshot(backup);restoredFromBackup=true;return }
                catch(secondary:Exception) { settings=Settings();questions.clear();submissions.clear() }
            }
            throw IllegalStateException("Data lokal gagal dibaca. Cadangan terakhir juga tidak dapat dipulihkan. Jangan membuat data baru sebelum backup aplikasi.",primary)
        }
    }
    private fun loadSnapshot(raw:String) {
        try {
            val root=JSONObject(raw); val s=root.getJSONObject("settings")
            settings=Settings(s.optString("school"),s.optString("gradeClass"),s.optString("subject"),s.optString("title"),
                s.optDecimal("kkm"),ScoreMode.valueOf(s.getString("mode")), s.decimalList("proportion"),
                s.decimalList("coefficients"),BigDecimal(s.getString("bonus")),BigDecimal(s.getString("deduction")),
                s.getString("custom"),s.getInt("keyVersion"),s.getInt("formulaVersion"))
            val qa=root.getJSONArray("questions"); for(i in 0 until qa.length()) {
                val q=qa.getJSONObject(i); questions.add(Question(q.getString("id"),Kind.valueOf(q.getString("kind")),q.getInt("number"),
                    q.getString("key"),BigDecimal(q.getString("maxPoints")),q.optBoolean("active",true)))
            }
            val sa=root.getJSONArray("submissions"); for(i in 0 until sa.length()) {
                val j=sa.getJSONObject(i); val rs=mutableMapOf<String,Response>(); val ra=j.getJSONArray("responses")
                for(k in 0 until ra.length()) { val r=ra.getJSONObject(k); val id=r.getString("questionId")
                    rs[id]=Response(id,r.getString("answer"),ReadState.valueOf(r.getString("state")),
                        r.optDecimal("teacherPoints"),r.getBoolean("reviewed"))
                }
                val student=Student(j.getString("code"),j.getString("name"))
                submissions[student.code]=Submission(student,rs,j.getBoolean("verified"),j.optString("photoPath"),
                    j.getInt("keyVersion"),j.getInt("formulaVersion"),
                    j.optBoolean("photoReviewed",false))
            }
        } catch(e: Exception) { // Keep corrupt raw snapshot for recovery, never silently overwrite on load.
            settings=Settings();questions.clear();submissions.clear()
            throw IllegalStateException("Snapshot tidak valid",e)
        }
    }
}
