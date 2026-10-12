import android.graphics.Bitmap
import id.edukreativa.korektorku.scan.*
import kotlin.math.*

fun main() {
    val w=595;val h=842
    val im=IntArray(w*h){0xFFFFFF}
    fun set(x:Int,y:Int,c:Int=0){if(x in 0 until w && y in 0 until h)im[y*w+x]=c}
    for(p in SheetTemplate.anchors) for(y in -9..9)for(x in -9..9) set((p.x+x).roundToInt(),(p.y+y).roundToInt())
    val correct=List(40){i->if(i==7 || i==37)"-" else "${'A'+(i%5)}"}
    for(n in 1..40) for(ch in 0..4) {
        val p=SheetTemplate.center(n,ch)
        for(y in -8..8)for(x in -8..8) {
            val r=hypot(x.toDouble(),y.toDouble())
            if(r in 6.5..7.5) set(p.x.roundToInt()+x,p.y.roundToInt()+y)
            if(correct[n-1]!=("${'A'+ch}"))continue
            if(r<=5) set(p.x.roundToInt()+x,p.y.roundToInt()+y)
        }
    }
    val read=OmrReader.detect(Bitmap(w,h,im),40)
    val matches=read.answers.zip(correct).count{it.first==it.second}
    println("OMR synthetic anchors=${read.anchorsFound}/4, correct=$matches/40, predicted=${read.answers.take(12)}")
    check(read.anchorsFound==4 && matches>=38){"Optical test failed"}
    val white=Bitmap(w,h,IntArray(w*h){0xFFFFFF})
    val missing=OmrReader.detect(white,10)
    check(missing.anchorsFound==0 && missing.answers.all{it=="?"}) { "Missing anchors must not produce grades" }
    check(runCatching{OmrReader.detect(Bitmap(100,800,IntArray(80000){0xFFFFFF}),5)}.isFailure) { "Extreme aspect ratio must be rejected" }
    // Four markers are present, but deliberately in wrong positions and too close together.
    val fake=IntArray(w*h){0xFFFFFF}
    for((mx,my) in listOf(150 to 100, 300 to 100, 150 to 620, 300 to 620)) {
        for(dy in -8..8)for(dx in -8..8) fake[(my+dy)*w+mx+dx]=0
    }
    val malformed=OmrReader.detect(Bitmap(w,h,fake),5)
    check(malformed.answers.all{it=="?"}) { "Bad marker geometry must not yield confirmed answer" }
    println("OMR negative checks OK: missing markers, unsuitable aspect ratio & invalid marker geometry")
}
