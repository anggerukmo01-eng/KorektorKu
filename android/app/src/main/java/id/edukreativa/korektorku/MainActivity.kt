package id.edukreativa.korektorku

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import id.edukreativa.korektorku.data.LocalStore
import id.edukreativa.korektorku.domain.*
import id.edukreativa.korektorku.export.XlsxExporter
import id.edukreativa.korektorku.scan.OmrReader
import id.edukreativa.korektorku.scan.SheetTemplate
import java.io.File
import java.math.BigDecimal

class MainActivity:Activity() {
    private lateinit var store: LocalStore
    private lateinit var page: LinearLayout
    private var pgEditor:EditText?=null
    private var isEditor:EditText?=null
    private var urEditor:EditText?=null
    private var codeEditor:EditText?=null
    private var nameEditor:EditText?=null
    private var activePhoto=""
    private var scanNeedsConfirmation=false
    private var photoReviewConfirmed=false
    private var lastScreen="Beranda"
    companion object { const val EXPORT_XLSX=100;const val EXPORT_PDF=101;const val CAMERA=102;const val IMPORT_PHOTO=103 }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        try { store=LocalStore(this) } catch(e:Exception) {
            AlertDialog.Builder(this).setTitle("Data lokal perlu pemulihan").setMessage(e.message).setPositiveButton("Tutup aplikasi"){_,_->finish()}.show()
            return
        }
        home()
        if(store.restoredFromBackup) AlertDialog.Builder(this).setTitle("Pemulihan data lokal")
            .setMessage("Data utama rusak. Aplikasi membuka salinan cadangan terakhir; perubahan terbaru mungkin tidak ikut tersimpan. Ekspor data sebelum melanjutkan.")
            .setPositiveButton("Mengerti",null).show()
    }
    private fun runSafe(work:()->Unit) {try {work()}catch(e:Exception){AlertDialog.Builder(this).setTitle("Periksa data").setMessage(e.message ?: "Terjadi kesalahan").setPositiveButton("OK",null).show()} }
    private fun screen(title:String) {
        lastScreen=title
        val sc=ScrollView(this);page=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL;setPadding(18,18,18,30);setBackgroundColor(Color.rgb(247,249,252))
        };sc.addView(page);setContentView(sc)
        label("KorektorKu  •  Edukreativa",22,true)
        label(title,19,true)
        val bar=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        listOf("Beranda","Asesmen","Koreksi","Rekap").forEach {name ->
            val b=Button(this).apply {text=name;textSize=11f;setAllCaps(false);setOnClickListener {
                when(name){"Beranda"->home();"Asesmen"->assessment();"Koreksi"->answers();"Rekap"->recap()}
            }}
            bar.addView(b,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        }
        page.addView(bar)
    }
    private fun label(t:String,size:Int=14,bold:Boolean=false) {page.addView(TextView(this).apply {
        text=t;textSize=size.toFloat();setTextColor(Color.rgb(25,39,66));setPadding(4,8,4,8)
        if(bold) setTypeface(typeface,Typeface.BOLD)
    })}
    private fun entry(label:String,value:String="",multi:Boolean=false):EditText {
        label(label,13,true)
        val e=EditText(this).apply{
            setText(value);setSingleLine(!multi);textSize=15f
            inputType=if(multi) android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE else android.text.InputType.TYPE_CLASS_TEXT
            if(multi) minLines=2
            setPadding(8,6,8,6);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(75,119,191)))
        };page.addView(e,LinearLayout.LayoutParams(-1,-2));return e
    }
    private fun button(text:String,onClick:()->Unit) {val b=Button(this).apply{
        setText(text);setAllCaps(false);setTextColor(Color.WHITE);setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(31,103,176)))
        setOnClickListener{runSafe(onClick)}
    };page.addView(b,LinearLayout.LayoutParams(-1,-2))}
    private fun show(msg:String)=Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
    private fun value(e:EditText)=e.text.toString().trim()
    private fun parts(s:String,delimiter:Char=',')=s.split(delimiter).map{it.trim()}
    private fun getCount(type:Kind)=store.questions.count {it.kind==type && it.active}

    private fun home() {
        screen("Beranda")
        label("Pemeriksaan jawaban & rekap nilai offline. PG dipindai dari LJK baku; isian dan uraian dapat dinilai manual. Hasil scan tidak otomatis menjadi nilai final.")
        label("Asesmen: ${store.settings.title.ifBlank { "Belum dibuat" }}  •  ${store.settings.gradeClass}")
        label("Soal aktif: ${store.questions.size}   |   Hasil siswa: ${store.submissions.size}",16,true)
        button("1. Atur Soal, Kunci & Bobot") {assessment()}
        button("2. Koreksi atau Scan Jawaban Siswa") {answers()}
        button("3. Rekap Nilai") {recap()}
        button("4. Cetak LJK PG (PDF)") {exportPdf()}
        button("5. Ekspor Excel (.xlsx)") {exportXlsx()}
        label("PERINGATAN: Hasil scan OMR adalah fitur percobaan. Guru wajib memeriksa hasilnya. Foto disimpan lokal; file Excel tidak terenkripsi.",13,true)
    }
    private fun assessment() {
        screen("Pengaturan Asesmen")
        val old=store.settings;val school=entry("Nama sekolah",old.school);val grade=entry("Kelas",old.gradeClass)
        val subject=entry("Mata pelajaran",old.subject);val title=entry("Nama asesmen",old.title)
        val np=entry("Jumlah Pilihan Ganda (0-50)",getCount(Kind.PG).toString())
        val ni=entry("Jumlah Isian (0-20)",getCount(Kind.ISIAN).toString())
        val nu=entry("Jumlah Uraian (0-10)",getCount(Kind.URAIAN).toString())
        val wp=entry("Bobot per PG",store.questions.firstOrNull{it.kind==Kind.PG}?.maxPoints?.toPlainString()?:"1")
        val wi=entry("Bobot per isian",store.questions.firstOrNull{it.kind==Kind.ISIAN}?.maxPoints?.toPlainString()?:"2")
        val wu=entry("Bobot per uraian",store.questions.firstOrNull{it.kind==Kind.URAIAN}?.maxPoints?.toPlainString()?:"5")
        fun prefillWeights(type:Kind):String {
            val q=store.questions.filter{it.kind==type};return if(q.isNotEmpty() && q.any{it.maxPoints!=q[0].maxPoints}) q.joinToString(","){it.maxPoints.toPlainString()} else ""
        }
        val specificPg=entry("Opsional bobot KHUSUS tiap PG (pisah koma; kosong = bobot standar)",prefillWeights(Kind.PG))
        val specificIs=entry("Opsional bobot KHUSUS tiap isian",prefillWeights(Kind.ISIAN))
        val specificUr=entry("Opsional bobot KHUSUS tiap uraian",prefillWeights(Kind.URAIAN))
        val kkm=entry("KKM (kosong = tanpa status ketuntasan)",old.kkm?.toPlainString()?:"")
        label("Kunci PG: urut sesuai soal, pisahkan dengan koma. Contoh: A,B,C,D",13)
        val keyPg=entry("Kunci Pilihan Ganda",store.questions.filter{it.kind==Kind.PG}.joinToString(","){it.key},true)
        label("Kunci isian: satu soal per baris. Alternatif gunakan | (contoh 4|empat).",13)
        val keyIs=entry("Kunci Isian",store.questions.filter{it.kind==Kind.ISIAN}.joinToString("\n"){it.key},true)
        label("Uraian menggunakan bobot dan skor guru; belum ada pengenalan otomatis esai.")
        val options=ScoreMode.values().map{it.name};val spinner=Spinner(this)
        spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options)
        spinner.setSelection(options.indexOf(old.mode.name));page.addView(spinner)
        val prop=entry("Proporsi PG, Isian, Uraian (jumlah=1)",old.proportion.joinToString(","))
        val coef=entry("Koefisien PG, Isian, Uraian",old.coefficients.joinToString(","))
        val bonus=entry("Bonus poin",old.bonus.toPlainString());val deduction=entry("Pengurang poin",old.deduction.toPlainString())
        val custom=entry("Rumus kustom (variabel tersedia di README)",old.custom,true)
        button("Simpan asesmen, kunci, dan aturan nilai") {
            val cnt=listOf(np,ni,nu).map{value(it).toInt()}
            require(cnt[0] in 0..50 && cnt[1] in 0..20 && cnt[2] in 0..10 && cnt.sum()>0){"Jumlah PG 0-50, isian 0-20, uraian 0-10. Minimal 1 soal."}
            val weights=listOf(wp,wi,wu).map{Grading.bd(value(it))};require(weights.all{it>BigDecimal.ZERO}){"Bobot harus positif"}
            val pgKeys=parts(value(keyPg),',').let{if(value(keyPg).isEmpty()) emptyList() else it}
            val isKeys=value(keyIs).split('\n').map{it.trim()}.let{if(value(keyIs).isEmpty()) emptyList() else it}
            require(pgKeys.size==cnt[0] && pgKeys.all{it.matches(Regex("[A-Ea-e]"))}){"Harus ada ${cnt[0]} kunci PG tunggal A-E"}
            require(isKeys.size==cnt[1] && isKeys.all{it.isNotEmpty()}){"Harus ada ${cnt[1]} kunci isian"}
            fun weightsFor(input:EditText, count:Int, default:BigDecimal):List<BigDecimal> {
                val result=if(value(input).isBlank()) List(count){default} else parts(value(input)).map(Grading::bd)
                require(result.size==count && result.all{it>BigDecimal.ZERO}){"Jumlah/isi bobot khusus tidak sesuai jumlah soal atau tidak positif"}
                return result
            }
            val perPg=weightsFor(specificPg,cnt[0],weights[0]);val perIs=weightsFor(specificIs,cnt[1],weights[1]);val perUr=weightsFor(specificUr,cnt[2],weights[2])
            val newQuestions=mutableListOf<Question>()
            for(i in 1..cnt[0])newQuestions.add(Question("PG-${i.toString().padStart(2,'0')}",Kind.PG,i,pgKeys[i-1],perPg[i-1]))
            for(i in 1..cnt[1])newQuestions.add(Question("IS-${i.toString().padStart(2,'0')}",Kind.ISIAN,i,isKeys[i-1],perIs[i-1]))
            for(i in 1..cnt[2])newQuestions.add(Question("UR-${i.toString().padStart(2,'0')}",Kind.URAIAN,i,"",perUr[i-1]))
            val pr=parts(value(prop)).map(Grading::bd);val co=parts(value(coef)).map(Grading::bd)
            require(pr.size==3 && co.size==3){"Isi 3 nilai PG, Isian, Uraian"}
            val kk=value(kkm).takeIf{it.isNotEmpty()}?.let(Grading::bd);require(kk==null || (kk>=BigDecimal.ZERO && kk<=BigDecimal("100"))){"KKM harus 0-100"}
            val modified=store.questions!=newQuestions || old.title!=value(title) || old.gradeClass!=value(grade) || old.subject!=value(subject)
            val newSettings=Settings(value(school),value(grade),value(subject),value(title),kk,ScoreMode.valueOf(spinner.selectedItem.toString()),
                pr,co,Grading.bd(value(bonus)),Grading.bd(value(deduction)),value(custom),
                if(modified) old.keyVersion+1 else old.keyVersion,
                if(modified || old.mode.name!=spinner.selectedItem.toString() || old.proportion!=pr || old.coefficients!=co || old.custom!=value(custom) || old.bonus!=Grading.bd(value(bonus)) || old.deduction!=Grading.bd(value(deduction)) || old.kkm!=kk) old.formulaVersion+1 else old.formulaVersion)
            // Validate scoring config using a temporary verified all-blank submission.
            val dummy=Submission(Student("CHECK","CHECK"),newQuestions.associate{it.id to Response(it.id,"",ReadState.BLANK_CONFIRMED,reviewed=true)},true,newQuestions.firstOrNull()?.id?:"",newSettings.keyVersion,newSettings.formulaVersion)
            Grading.grade(newSettings,newQuestions,dummy)
            val commit={store.settings=newSettings;store.questions=newQuestions;store.save();show("Asesmen disimpan. Nilai lama yang memakai versi sebelumnya tidak final.");home()}
            if(store.submissions.isNotEmpty() && (newSettings.keyVersion!=old.keyVersion || newSettings.formulaVersion!=old.formulaVersion))
                AlertDialog.Builder(this).setTitle("Versi asesmen berubah").setMessage("Hasil lama disimpan tetapi status FINAL lama akan tertahan hingga guru memverifikasi ulang. Lanjutkan?")
                    .setNegativeButton("Batal",null).setPositiveButton("Simpan"){_,_->commit()}.show()
            else commit()
        }
    }
    private fun parseResponse(q:Question, token:String):Response {
        val s=token.trim()
        val state=when {
            s.isEmpty()->ReadState.NOT_CAPTURED
            s=="?"->ReadState.UNCERTAIN
            s.contains('+') && q.kind==Kind.PG->ReadState.MULTIPLE
            s=="-"->ReadState.BLANK_CONFIRMED
            else->ReadState.VERIFIED
        }
        val points=if(q.kind==Kind.URAIAN && state==ReadState.VERIFIED) Grading.bd(s) else null
        return Response(q.id,if(q.kind==Kind.URAIAN) "" else s,state,points,reviewed=state !in listOf(ReadState.NOT_CAPTURED,ReadState.UNCERTAIN,ReadState.MULTIPLE))
    }
    private fun answers(preloadCode:String="") {
        activePhoto=""
        scanNeedsConfirmation=false
        photoReviewConfirmed=false
        screen("Koreksi Siswa & Verifikasi")
        if(store.questions.isEmpty()) {label("Atur asesmen dan kunci lebih dulu.");return}
        label("Petunjuk: PG pisah koma; ? = ragu; - = kosong; A+B = ganda. Isian 1 jawaban per baris; uraian skor angka per baris. Angka 0 tetap dinilai sah.")
        codeEditor=entry("Kode siswa unik (contoh 01)",preloadCode);nameEditor=entry("Nama siswa")
        button("Muat hasil tersimpan untuk kode di atas") {
            val sub=store.submissions[value(codeEditor!!)];require(sub!=null){"Kode siswa belum tersimpan"};answers(sub.student.code)
        }
        val submission=store.submissions[preloadCode]
        photoReviewConfirmed=submission?.photoReviewed==true
        scanNeedsConfirmation=submission?.photoPath?.isNotBlank()==true && !photoReviewConfirmed
        if(submission!=null) nameEditor?.setText(submission.student.name)
        val getText={type:Kind,delimiter:String->store.questions.filter{it.kind==type}.joinToString(delimiter){q->
            val r=submission?.responses?.get(q.id)
            when {r==null -> "";r.state==ReadState.BLANK_CONFIRMED -> "-";r.state==ReadState.UNCERTAIN -> "?"
                q.kind==Kind.URAIAN -> r.teacherPoints?.toPlainString()?:"";else->r.answer}
        }}
        pgEditor=entry("Jawaban PG (${getCount(Kind.PG)} soal)",getText(Kind.PG,","),true)
        isEditor=entry("Jawaban isian (${getCount(Kind.ISIAN)} soal; satu per baris)",getText(Kind.ISIAN,"\n"),true)
        urEditor=entry("Nilai uraian (${getCount(Kind.URAIAN)} soal; satu skor per baris)",getText(Kind.URAIAN,"\n"),true)
        button("Foto/Scan LJK melalui kamera") {captureCamera()}
        button("Pilih foto LJK dari file") {choosePhoto()}
        label("Pemindaian PG membaca lembar LJK cetak KorektorKu. Jawaban isian/uraian tetap harus diperiksa guru.")
        if(activePhoto.isNotBlank() || submission?.photoPath?.isNotBlank()==true) {
            val path=if(activePhoto.isNotBlank()) activePhoto else submission!!.photoPath
            val bitmap=decodeScaled(File(path))
            if(bitmap!=null) {val image=ImageView(this);image.setImageBitmap(bitmap);image.adjustViewBounds=true
                page.addView(image,LinearLayout.LayoutParams(-1,350))}
        }
        if(scanNeedsConfirmation) button("Saya sudah memeriksa foto & jawaban") { scanNeedsConfirmation=false;photoReviewConfirmed=true;show("Konfirmasi diterima. Pastikan setiap hasil terbaca dengan tepat sebelum FINAL.") }
        button("Hitung & Simpan Draf") {saveSubmission(false)}
        button("Verifikasi Jawaban & Simpan Nilai FINAL") {saveSubmission(true)}
    }
    private fun saveSubmission(verify:Boolean) {
        val code=value(codeEditor!!);val name=value(nameEditor!!)
        require(code.isNotBlank() && code.length<=40 && name.isNotBlank() && name.length<=150){"Kode dan nama siswa wajib diisi"}
        val pg=if(getCount(Kind.PG)==0) emptyList() else parts(value(pgEditor!!))
        val ins=if(getCount(Kind.ISIAN)==0) emptyList() else value(isEditor!!).split('\n').map{it.trim()}
        val ura=if(getCount(Kind.URAIAN)==0) emptyList() else value(urEditor!!).split('\n').map{it.trim()}
        require(pg.size==getCount(Kind.PG) && ins.size==getCount(Kind.ISIAN) && ura.size==getCount(Kind.URAIAN)){
            "Jumlah jawaban tidak sesuai PG=${pg.size}, Isian=${ins.size}, Uraian=${ura.size}. Gunakan ? pada soal ragu dan - pada jawaban kosong."}
        val all=mutableMapOf<String,Response>();val groups=mapOf(Kind.PG to pg,Kind.ISIAN to ins,Kind.URAIAN to ura)
        for(q in store.questions) {val token=groups.getValue(q.kind)[q.number-1];val r=parseResponse(q,token)
            if(q.kind==Kind.PG && r.state==ReadState.VERIFIED)require(r.answer.matches(Regex("[A-Ea-e]"))){"Jawaban PG ${q.number} harus A-E, - atau ?"}
            all[q.id]=r
        }
        val old=store.submissions[code]
        val photo=activePhoto.ifBlank{old?.photoPath?:""}
        if(verify && scanNeedsConfirmation) throw IllegalArgumentException("Periksa hasil scan dan tekan tombol 'Saya sudah memeriksa foto & jawaban' sebelum FINAL")
        val photoReviewed=photo.isBlank() || photoReviewConfirmed
        val sub=Submission(Student(code,name),all,verify,photo,store.settings.keyVersion,store.settings.formulaVersion,photoReviewed)
        val result=Grading.grade(store.settings,store.questions,sub)
        if(verify) require(result.status=="FINAL") {"Belum dapat FINAL: ${result.status}. Periksa ${result.unresolved.joinToString().ifBlank { "versi asesmen" }} terlebih dahulu."}
        store.submissions[code]=sub;store.save();activePhoto=""
        AlertDialog.Builder(this).setTitle("${result.status} — $name")
            .setMessage("Poin ${result.earned}/${result.maximum}\nNilai: ${result.score?.toPlainString() ?: "TIDAK FINAL"}\nPG benar: ${result.correctPg}\nPerlu review: ${result.unresolved.joinToString().ifBlank{"Tidak ada"}}")
            .setPositiveButton("Rekap"){_,_->recap()}.setNegativeButton("Koreksi lagi"){_,_->answers(code)}.show()
    }
    private fun recap() {
        screen("Rekap Nilai")
        if(store.submissions.isEmpty()) label("Belum ada hasil siswa")
        for((code,sub) in store.submissions.toSortedMap()) {
            val out=runCatching{Grading.grade(store.settings,store.questions,sub)}.getOrNull()
            label("${sub.student.name} ($code)  |  ${out?.score?.toPlainString()?:"—"}  |  ${out?.status?:"ERROR KONFIGURASI"}",14,true)
            button("Periksa jawaban: $code") {answers(code)}
        }
        button("Ekspor Excel Rekap Lengkap") {exportXlsx()}
        label("Nilai kosong berarti belum final; bukan berarti nilai 0. Ekspor tidak menyertakan foto mentah.")
    }
    private fun exportXlsx() {
        require(store.questions.isNotEmpty()){"Buat asesmen dahulu"}
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE);type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE,"Rekap_KorektorKu_${store.settings.gradeClass.replace(Regex("[^A-Za-z0-9_-]"),"_")}.xlsx")
        };startActivityForResult(intent,EXPORT_XLSX)
    }
    private fun exportPdf() {
        require(getCount(Kind.PG)>0){"Asesmen ini tidak punya soal PG"}
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf";putExtra(Intent.EXTRA_TITLE,"LJK_${store.settings.gradeClass}.pdf")
        };startActivityForResult(intent,EXPORT_PDF)
    }
    private fun captureCamera() {
        val uri=Uri.parse("content://${packageName}.capture/capture.jpg")
        val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply{
            putExtra(MediaStore.EXTRA_OUTPUT,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData=android.content.ClipData.newUri(contentResolver,"scan",uri)
        }
        startActivityForResult(intent,CAMERA)
    }
    private fun choosePhoto() {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {addCategory(Intent.CATEGORY_OPENABLE);type="image/*"}
        startActivityForResult(intent,IMPORT_PHOTO)
    }
    @Deprecated("Legacy activities for minimal dependency-free Android MVP")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK)return
        runSafe {
            when(requestCode) {
                EXPORT_XLSX -> contentResolver.openOutputStream(requireNotNull(data?.data))!!.use {
                    XlsxExporter.write(it,store.settings,store.questions,store.submissions.values.sortedBy{it.student.code})
                }.also {show("Excel berhasil dibuat")}
                EXPORT_PDF -> contentResolver.openOutputStream(requireNotNull(data?.data))!!.use {
                    SheetTemplate.create(it,store.settings.school,store.settings.subject,store.settings.title,getCount(Kind.PG))
                }.also {show("LJK PDF berhasil dibuat")}
                CAMERA -> {
                    val source=File(File(filesDir,"scans"),"capture.jpg")
                    require(source.exists() && source.length() in 2001..20_000_000){"Foto kamera tidak tersimpan atau lebih dari 20MB"}
                    activePhoto=File(File(filesDir,"scans"),"scan_${System.currentTimeMillis()}.jpg").also{source.copyTo(it,true)}.absolutePath
                    scanPhoto()
                }
                IMPORT_PHOTO -> {
                    val uri=requireNotNull(data?.data)
                    val output=File(File(filesDir,"scans").also{it.mkdirs()},"scan_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)!!.use{input->output.outputStream().use{input.copyTo(it)}}
                    require(output.length() in 2000..20_000_000){"File foto harus 2KB - 20MB"}
                    activePhoto=output.absolutePath;scanPhoto()
                }
            }
        }
    }
    private fun decodeScaled(file:File):Bitmap? {
        if(!file.isFile)return null
        val options=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        BitmapFactory.decodeFile(file.absolutePath,options)
        var sample=1;while(options.outWidth/sample>1300 || options.outHeight/sample>1700)sample*=2
        val bm=BitmapFactory.decodeFile(file.absolutePath,BitmapFactory.Options().apply{inSampleSize=sample})?:return null
        val angle=ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)
        val deg=when(angle){ExifInterface.ORIENTATION_ROTATE_90->90f;ExifInterface.ORIENTATION_ROTATE_180->180f;ExifInterface.ORIENTATION_ROTATE_270->270f;else->0f}
        return if(deg==0f) bm else Bitmap.createBitmap(bm,0,0,bm.width,bm.height,Matrix().apply{postRotate(deg)},true).also{bm.recycle()}
    }
    private fun scanPhoto() {
        val count=getCount(Kind.PG)
        if(count==0){show("Foto tersimpan, tetapi asesmen tidak punya PG");answers(value(codeEditor?:return));return}
        val bitmap=decodeScaled(File(activePhoto))?:throw IllegalArgumentException("Foto tidak terbaca")
        val result=OmrReader.detect(bitmap,count);bitmap.recycle()
        pgEditor?.setText(result.answers.joinToString(","))
        scanNeedsConfirmation=true
        photoReviewConfirmed=false
        val photoPreview=ImageView(this).apply { setImageBitmap(decodeScaled(File(activePhoto)));adjustViewBounds=true }
        page.addView(photoPreview,LinearLayout.LayoutParams(-1,420))
        button("Saya sudah memeriksa foto & jawaban") { scanNeedsConfirmation=false;photoReviewConfirmed=true;show("Hasil scan telah diperiksa. Silakan koreksi jawaban bila diperlukan.") }
        label("FOTO ASLI HASIL SCAN — Cocokkan dengan jawaban di atas",13,true)
        AlertDialog.Builder(this).setTitle("Hasil percobaan OMR")
            .setMessage("${result.warning}\n\nJawaban: ${result.answers.joinToString(",")}\n\nSelalu cocokkan dengan foto sebelum Verifikasi & Simpan.")
            .setPositiveButton("Periksa"){_,_-> /* keep form and scan tokens visible */ }.show()
    }
}
