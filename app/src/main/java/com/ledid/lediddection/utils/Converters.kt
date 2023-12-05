package com.ledid.lediddection.utils

import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Converters {
    @JvmStatic
    fun convertBitmapToFile(bitmap: Bitmap, onBitmapConverted: (File) -> Unit): Disposable {
        return Single.fromCallable {
            compressBitmap(bitmap)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null) {
                    Log.i("convertedPicturePath", it.path)
                    onBitmapConverted(it)
                }
            }, { it.printStackTrace() })
    }

    private fun compressBitmap(bitmap: Bitmap): File? {
        try {
            val myStuff =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "LedSample"
                )
            if (!myStuff.exists())
                myStuff.mkdirs()
            val picture = File(myStuff, "LedID-" + System.currentTimeMillis() + ".jpeg")

            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            val bitmapData = bos.toByteArray()

            val fos = FileOutputStream(picture)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            return picture
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
