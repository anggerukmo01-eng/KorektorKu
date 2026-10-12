package id.edukreativa.korektorku.scan

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.*

/** Experimental standalone offline OMR: photo -> four registration markers -> ink density in bubble interiors. */
object OmrReader {
    data class Reading(val answers:List<String>, val warning:String, val anchorsFound:Int)
    private data class Blob(val x:Float,val y:Float,val area:Int,val fill:Float)
    fun detect(original:Bitmap,count:Int):Reading {
        require(count in 1..50)
        require(original.width>0 && original.height>0) { "Ukuran foto tidak valid" }
        require(original.height.toDouble()/original.width in 0.65..2.5) { "Foto harus memperlihatkan lembar secara utuh dengan orientasi tegak" }
        val w=720;val h=(original.height.toFloat()*w/original.width).toInt().coerceAtLeast(1)
        val small=Bitmap.createScaledBitmap(original,w,h,true)
        val pix=IntArray(w*h);small.getPixels(pix,0,w,0,0,w,h); if(small!==original) small.recycle()
        fun dark(c:Int):Boolean {val r=(c shr 16) and 255;val g=(c shr 8) and 255;val b=c and 255;return (r*30+g*59+b*11)/100<115}
        // Find relatively solid black squares. Evaluate each region via 8-connected components on downscaled preview.
        val mask=BooleanArray(w*h){dark(pix[it])};val seen=BooleanArray(w*h);val blobs=ArrayList<Blob>()
        val queue=IntArray(w*h)
        for(y in 0 until h step 2) for(x in 0 until w step 2) {
            val root=y*w+x;if(!mask[root]||seen[root])continue
            var front=0;var back=0;queue[back++]=root;seen[root]=true
            var xmin=x;var xmax=x;var ymin=y;var ymax=y
            while(front<back) {
                val pos=queue[front++];val px=pos%w;val py=pos/w
                xmin=min(xmin,px);xmax=max(xmax,px);ymin=min(ymin,py);ymax=max(ymax,py)
                for(dy in -1..1)for(dx in -1..1) {
                    val nx=px+dx;val ny=py+dy
                    if(nx !in 0 until w || ny !in 0 until h)continue
                    val ix=ny*w+nx;if(mask[ix] && !seen[ix]) {seen[ix]=true;queue[back++]=ix}
                }
            }
            val bw=xmax-xmin+1;val bh=ymax-ymin+1;val area=front
            val ratio=bw.toFloat()/bh
            val fill=area.toFloat()/(bw*bh)
            if(area in 35..4500 && ratio in 0.65f..1.35f && fill>0.63f && bw in 6..85 && bh in 6..85)
                blobs.add(Blob((xmin+xmax)/2f,(ymin+ymax)/2f,area,fill))
        }
        fun find(q:Int):Blob? {
            val tx=if(q%2==0) w*0.11f else w*0.89f
            val ty=if(q<2) h*0.10f else h*0.90f
            return blobs.filter { b->(if(q%2==0) b.x<w*.35 else b.x>w*.65) && (if(q<2) b.y<h*.30 else b.y>h*.70) }
                .minByOrNull {b-> hypot((b.x-tx)/w,(b.y-ty)/h)-ln(1+b.area.toDouble()).toFloat()*.005f }
        }
        val b0=find(0);val b1=find(1);val b2=find(2);val b3=find(3)
        val anchors=listOfNotNull(b0,b1,b2,b3)
        if(anchors.size!=4 || anchors.distinctBy { it.x to it.y }.size!=4) return Reading(List(count){"?"},"Penanda sudut tidak lengkap (${anchors.size}/4). Ulangi foto tegak, seluruh kertas terlihat.",anchors.size)
        val bs=listOf(b0!!,b1!!,b2!!,b3!!)
        val leftWidth=bs[1].x-bs[0].x
        val bottomWidth=bs[3].x-bs[2].x
        val leftHeight=bs[2].y-bs[0].y
        val rightHeight=bs[3].y-bs[1].y
        val markerRatio=(bs.maxOf{it.area}.toFloat()/bs.minOf{it.area})
        val geometryValid=leftWidth>w*.55 && bottomWidth>w*.55 && leftHeight>h*.55 && rightHeight>h*.55 &&
            abs(bs[0].y-bs[1].y)<h*.12 && abs(bs[2].y-bs[3].y)<h*.12 &&
            abs(bs[0].x-bs[2].x)<w*.12 && abs(bs[1].x-bs[3].x)<w*.12 && markerRatio<3.5f
        if(!geometryValid) return Reading(List(count){"?"},"Geometri penanda LJK tidak sesuai; jangan tetapkan nilai dari foto ini. Foto ulang.",4)
        // Bilinear alignment handles moderate skew. Severe perspective must be rejected by teacher.
        fun position(x:Float,y:Float):PointF {
            val u=((x-SheetTemplate.anchors[0].x)/(SheetTemplate.anchors[1].x-SheetTemplate.anchors[0].x)).coerceIn(-.1f,1.1f)
            val v=((y-SheetTemplate.anchors[0].y)/(SheetTemplate.anchors[2].y-SheetTemplate.anchors[0].y)).coerceIn(-.1f,1.1f)
            val x0=bs[0].x*(1-u)+bs[1].x*u;val x1=bs[2].x*(1-u)+bs[3].x*u
            val y0=bs[0].y*(1-u)+bs[1].y*u;val y1=bs[2].y*(1-u)+bs[3].y*u
            return PointF(x0*(1-v)+x1*v,y0*(1-v)+y1*v)
        }
        val bubbleAnswers=ArrayList<String>()
        for(n in 1..count) {
            val density=(0..4).map {choice->
                val pt=SheetTemplate.center(n,choice);var ink=0;var total=0
                for (dy in -3..3 step 2) for(dx in -3..3 step 2) {
                    if(dx*dx+dy*dy>12)continue
                    val target=position(pt.x+dx,pt.y+dy);val xx=target.x.roundToInt();val yy=target.y.roundToInt()
                    if(xx in 0 until w && yy in 0 until h){total++; if(mask[yy*w+xx])ink++}
                }
                if(total==0) 0.0 else ink.toDouble()/total
            }
            val sorted=density.indices.sortedByDescending{density[it]};val best=sorted[0];val second=sorted[1]
            bubbleAnswers.add(when {
                density[best]<0.28 -> "-"
                density[best]>0.43 && density[second]>0.43 -> "?" // Multiple marked
                density[best]>0.45 && density[best]-density[second]>0.18 -> ("A"[0]+best).toString()
                else -> "?"
            })
        }
        return Reading(bubbleAnswers,"OMR percobaan: ${bubbleAnswers.count{it=="?"}} soal perlu tinjau. Periksa setiap jawaban sebelum final.",4)
    }
}
