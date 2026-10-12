package id.edukreativa.korektorku.scan
import android.graphics.PointF
object SheetTemplate {
    val anchors=listOf(PointF(50f,65f),PointF(545f,65f),PointF(50f,777f),PointF(545f,777f))
    fun center(n:Int,c:Int):PointF {
        val idx=n-1;val left=if(idx<25)45f else 305f
        return PointF(left+83f+c*26f,170f+(idx%25)*21f)
    }
}
