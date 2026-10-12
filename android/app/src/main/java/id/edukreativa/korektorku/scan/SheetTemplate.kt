package id.edukreativa.korektorku.scan

import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** Single-page A4 PG answer sheet, 1..50 questions; four solid optical registration markers. */
object SheetTemplate {
    const val W=595f; const val H=842f
    val anchors=listOf(PointF(50f,65f),PointF(545f,65f),PointF(50f,777f),PointF(545f,777f))
    fun center(questionNumber:Int, choice:Int):PointF {
        require(questionNumber in 1..50 && choice in 0..4)
        val idx=questionNumber-1; val left=if(idx<25) 45f else 305f
        return PointF(left+83f+choice*26f,170f+(idx%25)*21f)
    }
    fun create(output:OutputStream,school:String,subject:String,title:String,pgCount:Int) {
        require(pgCount in 1..50)
        val doc=PdfDocument(); val page=doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(),H.toInt(),1).create())
        val c=page.canvas; c.drawColor(Color.WHITE)
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.BLACK;typeface=Typeface.create("sans-serif",Typeface.NORMAL) }
        anchors.forEach { c.drawRect(it.x-9,it.y-9,it.x+9,it.y+9,p) }
        p.textSize=19f;p.isFakeBoldText=true;c.drawText("KOREKTORKU - LEMBAR JAWAB PG",82f,98f,p)
        p.textSize=11f;p.isFakeBoldText=false
        c.drawText("Sekolah: ${school.take(55)}",45f,113f,p)
        c.drawText("Mapel / Ujian: ${(subject+" / "+title).take(73)}",45f,127f,p)
        c.drawText("Kode Siswa: _____________________  Nama: _________________________________",45f,140f,p)
        c.drawText("Isi SATU lingkaran hitam tiap soal (A-E). Jangan mengotori tanda hitam di sudut.",45f,151f,p)
        val line=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.DKGRAY;style=Paint.Style.STROKE;strokeWidth=0.9f}
        for(n in 1..pgCount) {
            val first=center(n,0);p.textSize=10.5f;p.color=Color.BLACK
            c.drawText("${n.toString().padStart(2,'0')}.",first.x-62f,first.y+4f,p)
            for(j in 0..4) {
                val pt=center(n,j);c.drawCircle(pt.x,pt.y,7.2f,line)
                p.textSize=8f;c.drawText("${'A'+j}",pt.x-2.4f,pt.y-10f,p)
            }
        }
        p.textSize=10f;c.drawText("Jumlah soal PG: $pgCount    |    Isian dan uraian dinilai di layar aplikasi.",45f,724f,p)
        c.drawText("Hasil scan harus diperiksa guru sebelum finalisasi nilai.",45f,739f,p)
        doc.finishPage(page);doc.writeTo(output);doc.close()
    }
}
