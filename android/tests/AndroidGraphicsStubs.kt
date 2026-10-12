package android.graphics
class PointF(val x:Float,val y:Float)
class Bitmap(val width:Int,val height:Int,private val pix:IntArray) {
    fun getPixels(dst:IntArray,offset:Int,stride:Int,x:Int,y:Int,w:Int,h:Int) {
        for(j in 0 until h)for(i in 0 until w) dst[offset+j*stride+i]=pix[(y+j)*width+(x+i)]
    }
    fun recycle() {}
    companion object {
        fun createScaledBitmap(original:Bitmap,width:Int,height:Int,filter:Boolean):Bitmap {
            val x=IntArray(width*height)
            for(j in 0 until height)for(i in 0 until width){
                val sx=(i.toDouble()*original.width/width).toInt().coerceIn(0,original.width-1)
                val sy=(j.toDouble()*original.height/height).toInt().coerceIn(0,original.height-1)
                x[j*width+i]=original.pix[sy*original.width+sx]
            }
            return Bitmap(width,height,x)
        }
    }
}
