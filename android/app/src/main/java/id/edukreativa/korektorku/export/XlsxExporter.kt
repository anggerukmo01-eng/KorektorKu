package id.edukreativa.korektorku.export

import id.edukreativa.korektorku.domain.*
import java.io.OutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** XLSX OpenXML writer with literal text encoding to prevent spreadsheet formula injection. */
object XlsxExporter {
    private fun xml(s:String) = s.replace("&","&amp;").replace("<","&lt;")
        .replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
    private fun col(n: Int): String { var a=n+1;var result="";while(a>0){ a--;result=('A'+a%26)+result;a/=26 };return result }
    private fun cell(v:Any?, row:Int, column:Int): String {
        val ref="${col(column)}$row"
        val sty=if(row==1) " s=\"1\"" else ""
        return when(v) {
            null -> ""
            is Number, is BigDecimal -> "<c r=\"$ref\"$sty><v>${v}</v></c>"
            else -> "<c r=\"$ref\"$sty t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(v.toString())}</t></is></c>"
        }
    }
    private fun sheet(rows:List<List<Any?>>):String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols>")
        val count=rows.maxOfOrNull { it.size } ?: 1
        for(c in 1..count) append("<col min=\"$c\" max=\"$c\" width=\"${if(c==2) 31 else 20}\" customWidth=\"1\"/>")
        append("</cols><sheetData>")
        rows.forEachIndexed{r,values -> append("<row r=\"${r+1}\">");values.forEachIndexed{c,v -> append(cell(v,r+1,c))};append("</row>") }
        append("</sheetData></worksheet>")
    }
    private fun points(q:Question, r:Response?): BigDecimal? {
        if(r==null || r.state in listOf(ReadState.NOT_CAPTURED,ReadState.UNCERTAIN,ReadState.MULTIPLE,ReadState.RUBRIC_PENDING)) return null
        if(r.state==ReadState.BLANK_CONFIRMED) return BigDecimal.ZERO
        return when(q.kind) {
            Kind.PG -> if(Grading.matches(q.key,r.answer)) q.maxPoints else BigDecimal.ZERO
            Kind.ISIAN -> r.teacherPoints ?: if(Grading.matches(q.key,r.answer)) q.maxPoints else BigDecimal.ZERO
            Kind.URAIAN -> r.teacherPoints
        }
    }
    fun write(out:OutputStream, settings: Settings, questions: List<Question>, submissions:List<Submission>) {
        val scores=submissions.associate { it.student.code to runCatching{Grading.grade(settings,questions,it)}.getOrNull() }
        val sheets=linkedMapOf<String,List<List<Any?>>>()
        sheets["PANDUAN"]=listOf(
            listOf("KOREKTORKU — REKAP NILAI OFFLINE", "Versi 5.0 MVP"),
            listOf("Status FINAL", "Nilai sudah diverifikasi sesuai versi kunci & rumus"),
            listOf("DRAF / PERLU REVIEW", "Nilai tidak diterbitkan; kolom NILAI harus kosong"),
            listOf("Keamanan", "Workbook tidak dienkripsi. Simpan sesuai kebijakan sekolah."),
            listOf("Rumus", "Nilai diekspor sebagai snapshot hasil engine Android; mengubah sel tidak memperbarui database."),
            listOf("Pemindaian", "Foto mentah tidak disertakan ke berkas Excel")
        )
        sheets["PENGATURAN"]=listOf(listOf("Parameter","Nilai")) + listOf(
            listOf("Sekolah",settings.school),listOf("Kelas",settings.gradeClass),listOf("Mapel",settings.subject),
            listOf("Asesmen",settings.title),listOf("KKM",settings.kkm?.toPlainString() ?: ""),
            listOf("Mode",settings.mode.name),listOf("Proporsi PG/IS/UR",settings.proportion.joinToString("/") ),
            listOf("Koefisien PG/IS/UR",settings.coefficients.joinToString("/")),
            listOf("Bonus",settings.bonus),listOf("Pengurang",settings.deduction),
            listOf("Custom expression",settings.custom),listOf("Versi kunci",settings.keyVersion),
            listOf("Versi formula",settings.formulaVersion),
            listOf("PG aktif",questions.count {it.active && it.kind==Kind.PG}),
            listOf("Isian aktif",questions.count{it.active && it.kind==Kind.ISIAN}),
            listOf("Uraian aktif",questions.count{it.active && it.kind==Kind.URAIAN}) )
        sheets["KUNCI"]=listOf(listOf("ID","Jenis","Nomor","Kunci / Alternatif","Bobot","Aktif")) + questions.map {q->
            listOf(q.id,q.kind.name,q.number,q.key,q.maxPoints,if(q.active) "YA" else "TIDAK")
        }
        sheets["RESPONS"]=listOf(listOf("Kode Siswa","Nama Siswa","ID Soal","Jenis","Jawaban","Status Baca","Poin","Maksimum","Ditinjau Guru","Versi Kunci")) + submissions.flatMap {sub->
            questions.filter{it.active}.map {q-> val r=sub.responses[q.id];listOf(sub.student.code,sub.student.name,q.id,q.kind.name,r?.answer ?: "",r?.state?.name ?: "NOT_CAPTURED",points(q,r),q.maxPoints,r?.reviewed?:false,sub.keyVersion)}
        }
        sheets["REKAP_NILAI"]=listOf(listOf("Kode Siswa","Nama Siswa","Kelas","PG Benar","PG Salah","Kosong","Perlu Review","Poin","Maks Poin","Nilai 0-100","KKM","Ketuntasan","Status","Versi Kunci","Versi Formula")) + submissions.map{ sub-> val sc=scores[sub.student.code]
            listOf(sub.student.code,sub.student.name,settings.gradeClass,sc?.correctPg,sc?.wrongPg,sc?.blank,sc?.unresolved?.size,
                sc?.earned,sc?.maximum,sc?.score,settings.kkm,when(sc?.passed){true->"TUNTAS";false->"BELUM TUNTAS";null->""},
                sc?.status?:"ERROR VALIDASI",sub.keyVersion,sub.formulaVersion)
        }
        sheets["ANALISIS_SOAL"]=listOf(listOf("ID Soal","Jenis","Maksimum","Jumlah Hasil Final","Jawaban Benar / Penuh","Persen Benar")) + questions.filter{it.active}.map {q->
            val final=submissions.filter { scores[it.student.code]?.status=="FINAL" }; val full=final.count{points(q,it.responses[q.id])==q.maxPoints}
            listOf(q.id,q.kind.name,q.maxPoints,final.size,full,if(final.isEmpty()) null else BigDecimal(full).multiply(BigDecimal("100")).divide(BigDecimal(final.size),2,java.math.RoundingMode.HALF_UP))
        }
        val names=sheets.keys.toList()
        ZipOutputStream(out).use {zip->
            fun file(name:String, content:String){zip.putNextEntry(ZipEntry(name));zip.write(content.toByteArray(Charsets.UTF_8));zip.closeEntry()}
            file("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" + names.indices.joinToString(""){"<Override PartName=\"/xl/worksheets/sheet${it+1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"} + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>")
            file("_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            file("xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"+names.mapIndexed{i,n->"<sheet name=\"${xml(n)}\" sheetId=\"${i+1}\" r:id=\"rId${i+1}\"/>"}.joinToString("")+"</sheets></workbook>")
            file("xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"+names.indices.joinToString(""){ "<Relationship Id=\"rId${it+1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${it+1}.xml\"/>" }+"<Relationship Id=\"rId${names.size+1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>")
            file("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF2563A6"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="1" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>""")
            sheets.values.forEachIndexed{i,rows->file("xl/worksheets/sheet${i+1}.xml",sheet(rows))}
        }
    }
}
