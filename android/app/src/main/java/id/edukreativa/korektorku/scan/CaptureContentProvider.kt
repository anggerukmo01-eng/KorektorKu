package id.edukreativa.korektorku.scan

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Grants the external camera temporary write access to ONE app-private capture path. */
class CaptureContentProvider: ContentProvider() {
    override fun onCreate()=true
    override fun getType(uri:Uri):String?=if(uri.lastPathSegment=="capture.jpg") "image/jpeg" else null
    override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor?=null
    override fun insert(uri:Uri,values:ContentValues?):Uri?=null
    override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?)=0
    override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?)=0
    override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor {
        require(uri.lastPathSegment=="capture.jpg" && mode.contains('w')) { "Path / mode ditolak" }
        val dir=File(context!!.filesDir,"scans");dir.mkdirs()
        val file=File(dir,"capture.jpg")
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_TRUNCATE)
    }
}
